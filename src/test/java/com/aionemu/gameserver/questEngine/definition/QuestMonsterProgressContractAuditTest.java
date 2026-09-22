package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证怪物击杀任务与 Aion 5.8 客户端进度契约（SECTION_0 与 SECTION_1）的对齐性。
 * Verifies that monster hunt quests align with the Aion 5.8 client progress contract (SECTION_0 step vs SECTION_1 kill counter).
 */
class QuestMonsterProgressContractAuditTest {
	private static final Path CONTRACTS_CSV = Path.of("docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv");
	private static final List<Integer> SPECIAL_MISSIONS_1_TO_24 = List.of(
		19631, 19632, 19633, 19634, 19635, 19636, 19637, 19638, 19639, 19640, 19641, 19642,
		29631, 29632, 29633, 29634, 29635, 29636, 29637, 29638, 29639, 29640, 29641, 29642);
	private static final List<Integer> STEP_ZERO_MULTI_COUNTER_REPORT_QUESTS = List.of(
		15001, 15020, 15073, 15100, 15104, 15203, 15406, 15407, 15408,
		15580, 15671, 25671, 25060, 18952);

	@Test
	void specialMissionsElyosAndAsmodiansStrictlyAlignWithClientStepAndKillCounterSeparation() throws Exception {
		for (int questId : SPECIAL_MISSIONS_1_TO_24) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();

			// 1. 变量契约：必须具有 var0（步骤，width=6）与 var1（击杀计数，width=6）
			BitField var0 = layout.field("var0");
			BitField var1 = layout.field("var1");
			assertNotNull(var0, () -> "quest " + questId + " must declare var0");
			assertNotNull(var1, () -> "quest " + questId + " must declare var1 for kill counter");
			assertEquals(0, var0.offset());
			assertEquals(6, var1.offset());

			// 2. 节点契约：started 节点只投影 START，不得锁死实时计数字段；reward 节点投影 var0=1, var1=10
			assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0));
			assertNode(definition, "started", QuestStatus.START, Map.of());
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 10));
			assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0));

			// 3. 击杀路线契约：存在 priority 1 的 var1 递增自环（PACKET_ONLY），且绝不递增 var0
			List<QuestTransition> continuingKills = definition.transitions().stream()
				.filter(t -> isKillEvent(t.event()))
				.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("started"))
				.filter(t -> Integer.valueOf(1).equals(t.priority()))
				.toList();
			assertFalse(continuingKills.isEmpty(), () -> "quest " + questId + " must have continuing kill route");
			for (QuestTransition t : continuingKills) {
				assertTrue(t.actions().stream().anyMatch(a -> a instanceof QuestAction.IncrementVariable(String field, int delta) && field.equals("var1")),
					() -> "quest " + questId + " continuing kill route must increment var1");
				assertFalse(t.actions().stream().anyMatch(a -> a instanceof QuestAction.IncrementVariable(String field, int delta) && field.equals("var0")),
					() -> "quest " + questId + " continuing kill route must not increment var0");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)), t.afterCommit());
			}

			// 4. 终结路线契约：存在 priority 0 的终击直达 reward 路线（LEVEL_AND_VISIBILITY_REFRESH），并显式设置 var0=1 与 var1=10
			List<QuestTransition> completingKills = definition.transitions().stream()
				.filter(t -> isKillEvent(t.event()))
				.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("reward"))
				.filter(t -> Integer.valueOf(0).equals(t.priority()))
				.toList();
			assertFalse(completingKills.isEmpty(), () -> "quest " + questId + " must have priority 0 completing kill route");
			for (QuestTransition t : completingKills) {
				assertTrue(t.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable(String field, int value) && field.equals("var0") && value == 1),
					() -> "quest " + questId + " completing route must set var0=1");
				assertTrue(t.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable(String field, int value) && field.equals("var1") && value == 10),
					() -> "quest " + questId + " completing route must set var1=10");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), t.afterCommit());
			}

			// 5. 满计数恢复路由：在 started 状态下且 var1>=10 时，必须能向报告 NPC 提交报告
			boolean hasRecoveredReport = definition.transitions().stream()
				.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("reward"))
				.anyMatch(t -> t.event() instanceof QuestEvent.TalkToNpc talk && (talk.dialogId() == null || talk.dialogId() == 31 || talk.dialogId() == 1009)
					&& t.conditions().stream().anyMatch(c -> c instanceof QuestCondition.VariableAtLeast(String field, int value) && field.equals("var1") && value == 10));
			assertTrue(hasRecoveredReport, () -> "quest " + questId + " must provide recovered report route for full counters");
		}
	}

	/**
	 * 验证复杂多阶段/多目标杀怪任务必须根据客户端契约声明对应的独立字段 (var1, var2...)。
	 * 彻底根除步骤号跳变与怪物计数漏记。
	 */
	@Test
	void complexAndMultiDimensionKillQuestsAlignWithClientSections() throws Exception {
		List<Integer> targetQuests = List.of(
			13945, 18994, 28994, 13705, 25406, 25407, 25408, 25580,
			15546, 25546, 17510, 27510, 10112, 20112, 10011, 20011);

		for (int questId : targetQuests) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();

			BitField var0 = layout.field("var0");
			BitField var1 = layout.field("var1");
			assertNotNull(var0, () -> "quest " + questId + " must declare var0");
			assertNotNull(var1, () -> "quest " + questId + " must declare var1");
			assertEquals(0, var0.offset());
			assertEquals(6, var1.offset());

			if (List.of(25406, 25407, 25408, 25580, 17510, 27510, 10112, 20112).contains(questId)) {
				BitField var2 = layout.field("var2");
				assertNotNull(var2, () -> "quest " + questId + " must declare var2");
				assertEquals(12, var2.offset());
			}

			if (List.of(15546, 25546).contains(questId)) {
				for (int i = 2; i <= 4; i++) {
					final int fieldIndex = i;
					BitField varField = layout.field("var" + fieldIndex);
					assertNotNull(varField, () -> "quest " + questId + " must declare var" + fieldIndex);
					assertEquals(6 * fieldIndex, varField.offset());
				}
			}
		}
	}

	@Test
	void repairedMultiKillQuestsDeclareSeparateKillCounters() throws Exception {
		for (int questId : List.of(15324, 50091, 50092)) {
			CompiledQuestDefinition compiled = load(questId);
			ProgressLayout layout = compiled.definition().progressLayout();
			assertNotNull(layout.field("var0"), "quest " + questId + " must declare var0");
			assertNotNull(layout.field("var1"), "quest " + questId + " must declare var1");
			assertEquals(0, layout.field("var0").offset());
			assertEquals(6, layout.field("var1").offset());
		}
	}

	@Test
	void stepZeroMultiCounterHuntsAdvanceSectionZeroToTheReportStep() throws Exception {
		for (int questId : STEP_ZERO_MULTI_COUNTER_REPORT_QUESTS) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();

			QuestNode reward = definition.nodes().stream()
				.filter(node -> node.label().equals("reward"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest " + questId + " must define a reward node"));
			assertEquals(1, reward.projection().variables().get("var0"),
				() -> "quest " + questId + " reward must project SECTION_0=1");

			boolean hasMigrationRepair = definition.transitions().stream()
				.filter(transition -> transition.sourceNode() == null)
				.filter(transition -> transition.targetNode().equals("reward"))
				.filter(transition -> transition.event() instanceof QuestEvent.EnterWorld)
				.anyMatch(transition -> transition.conditions().stream()
						.anyMatch(condition -> condition instanceof QuestCondition.StatusIs status
							&& status.status() == QuestStatus.REWARD)
					&& transition.conditions().stream()
						.anyMatch(condition -> condition instanceof QuestCondition.QuestVariableIs variable
							&& variable.field().equals("var0") && variable.value() == 0)
					&& transition.actions().stream()
						.anyMatch(action -> action instanceof QuestAction.SetVariable(String field, int value)
							&& field.equals("var0") && value == 1)
					&& transition.afterCommit().contains(new AfterCommitAction.SyncQuestState(
						QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)));
			assertTrue(hasMigrationRepair,
				() -> "quest " + questId + " must repair legacy REWARD SECTION_0=0 saves");

			List<QuestTransition> completing = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(transition -> isKillEvent(transition.event()))
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.toList();
			assertFalse(completing.isEmpty(),
				() -> "quest " + questId + " must have a completing kill route");
			for (QuestTransition transition : completing) {
				assertTrue(transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.SetVariable(String field, int value)
							&& field.equals("var0") && value == 1),
					() -> "quest " + questId + " completing kill route must set SECTION_0=1");
				assertTrue(transition.afterCommit().contains(
						new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
					() -> "quest " + questId + " completing kill route must refresh visibility");
			}

			List<QuestTransition> continuing = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "started".equals(transition.targetNode()))
				.filter(transition -> isKillEvent(transition.event()))
				.toList();
			assertFalse(continuing.isEmpty(),
				() -> "quest " + questId + " must have a continuing kill route");
			for (QuestTransition transition : continuing) {
				assertTrue(transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.SetVariable(String field, int value)
							&& field.equals("var0") && value == 0),
					() -> "quest " + questId + " continuing kill route must pin SECTION_0=0");
			}
		}
	}

	@Test
	void quest15001SaturatesBothSectionsAndEntersRewardWithSectionZeroOne() throws Exception {
		CompiledQuestDefinition definition = load(15001);
		QuestSnapshot state = new QuestSnapshot(7, 15001, QuestStatus.START, 0, Map.of());
		for (int count = 0; count < 5; count++) {
			state = apply(definition, state, new QuestEvent.KillNpc(235790));
		}
		for (int count = 0; count < 5; count++) {
			state = apply(definition, state, new QuestEvent.KillNpc(235799));
		}
		assertEquals(QuestStatus.REWARD, state.status());
		assertEquals(1 + (5 << 6) + (5 << 12), state.packedVariables());
	}

	@Test
	void clientMonsterProgressContractsCsvExistsAndHasExpectedShape() throws Exception {
		assertTrue(Files.exists(CONTRACTS_CSV), "contracts CSV must exist in docs/quest/client-dialog-mapping/");
		try (BufferedReader reader = Files.newBufferedReader(CONTRACTS_CSV, StandardCharsets.UTF_8)) {
			String header = reader.readLine();
			assertNotNull(header);
			assertTrue(header.contains("quest_id"));
			assertTrue(header.contains("step_section"));
			assertTrue(header.contains("counter_section"));
			assertTrue(header.contains("required"));

			int count = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					count++;
				}
			}
			final int totalCount = count;
			assertTrue(totalCount >= 800, () -> "expected at least 800 client monster progress contracts, found " + totalCount);
		}
	}


	/**
	 * 15101：804715 的 SETPRO1 点头把 SECTION_0 推到击杀行（0->1），
	 * 第 10 只击杀写报告行（2）并进入 REWARD。
	 * 15101: the 804715 SETPRO1 nod pushes SECTION_0 onto the kill row and the tenth kill writes the report row.
	 */
	@Test
	void quest15101DialogUnlocksTheKillRowAndTheLastKillReachesTheReportRow() throws Exception {
		CompiledQuestDefinition compiled = load(15101);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "hunt", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 2, "var1", 10));

		QuestSnapshot state = new QuestSnapshot(7, 15101, QuestStatus.START, 0, Map.of());
		state = apply(compiled, state, new QuestEvent.TalkToNpc(804715, QuestDialogAction.SETPRO1.id()));
		assertEquals(1, unpack(state, layout).get("var0"),
			"804715 SETPRO1 must push SECTION_0 onto the kill row");

		for (int kill = 1; kill <= 10; kill++) {
			final int killIndex = kill;
			state = apply(compiled, state, new QuestEvent.KillNpc(235939));
			final Map<String, Integer> variables = unpack(state, layout);
			assertEquals(killIndex, variables.get("var1"), () -> "kill " + killIndex + " must advance SECTION_1");
			assertEquals(killIndex == 10 ? 2 : 1, variables.get("var0"),
				() -> "kill " + killIndex + " must keep the client journal on its row");
		}
		assertEquals(QuestStatus.REWARD, state.status());
	}

	/**
	 * 24153：客户端 SECTION_0..4 是 5 只冰冻独眼巨人的独立计数，全部由 SECTION_5==0 门控，
	 * 与旧 handler 的 setQuestVarById(0..4) / setQuestVarById(5,1->0) 同值。
	 * 24153: client SECTION_0..4 are the five independent cyclops counters, all gated by SECTION_5==0.
	 */
	@Test
	void quest24153DeclaresFiveClientCountersBehindTheSectionFiveGate() throws Exception {
		CompiledQuestDefinition compiled = load(24153);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		for (int index = 0; index <= 5; index++) {
			final int fieldIndex = index;
			BitField field = layout.field("var" + fieldIndex);
			assertNotNull(field, () -> "quest 24153 must declare var" + fieldIndex);
			assertEquals(6 * fieldIndex, field.offset(), () -> "var" + fieldIndex + " must map its client SECTION");
			assertEquals(1, field.maxValue(), () -> "var" + fieldIndex + " is a 0/1 counter");
		}
		assertNode(definition, "started", QuestStatus.START, Map.of("var5", 1));
		assertNode(definition, "hunted", QuestStatus.START, Map.of("var5", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of(
			"var0", 1, "var1", 1, "var2", 1, "var3", 1, "var4", 1, "var5", 0));

		Map<Integer, String> counters = new LinkedHashMap<>();
		counters.put(213730, "var0");
		counters.put(213788, "var1");
		counters.put(213789, "var2");
		counters.put(213790, "var3");
		counters.put(213791, "var4");
		for (Map.Entry<Integer, String> entry : counters.entrySet()) {
			QuestTransition route = definition.transitions().stream()
				.filter(transition -> "hunted".equals(transition.sourceNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.KillNpc kill
					&& kill.npcId() == entry.getKey())
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest 24153 misses the kill route " + entry.getKey()));
			assertTrue(route.actions().contains(new QuestAction.SetVariable(entry.getValue(), 1)),
				() -> "kill route " + entry.getKey() + " must pin " + entry.getValue());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				route.afterCommit(), () -> "kill route " + entry.getKey() + " must stay PACKET_ONLY");
		}

		QuestSnapshot state = new QuestSnapshot(7, 24153, QuestStatus.START,
			layout.pack(Map.of("var5", 1)), Map.of());
		state = apply(compiled, state, new QuestEvent.TalkToNpc(204784, QuestDialogAction.SETPRO2.id()));
		assertEquals(0, unpack(state, layout).get("var5"),
			"the Delris dialog must clear the SECTION_5 gate so the client counts the five cyclopes");

		for (int npcId : List.of(213730, 213788, 213789, 213790, 213791)) {
			state = apply(compiled, state, new QuestEvent.KillNpc(npcId));
		}
		Map<String, Integer> variables = unpack(state, layout);
		assertEquals(List.of(1, 1, 1, 1, 1), List.of(variables.get("var0"), variables.get("var1"),
			variables.get("var2"), variables.get("var3"), variables.get("var4")));
		assertEquals(QuestStatus.START, state.status());

		// 跨部署保持在线的旧存档靠报告 NPC 的无 source 自愈路线补齐五段计数；
		// 该路线与既有 reward->reward QUEST_SELECT 路线在编译期来源节点互斥，因此可以并存。
		assertTrue(definition.transitions().stream()
				.filter(transition -> transition.sourceNode() == null)
				.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(204787,
					QuestDialogAction.QUEST_SELECT.id())))
				.anyMatch(transition -> transition.conditions().stream()
					.anyMatch(condition -> condition instanceof QuestCondition.VariableBelow(String field, int value)
						&& field.equals("var0") && value == 1)),
			"quest 24153 must keep the report-NPC talk self-heal for stale SECTION_0 rows");

		state = apply(compiled, state, new QuestEvent.TalkToNpc(204787, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(QuestStatus.REWARD, state.status());
		assertEquals(Map.of("var0", 1, "var1", 1, "var2", 1, "var3", 1, "var4", 1, "var5", 0),
			unpack(state, layout));
	}

	/**
	 * 25304：0 找帕赫曼 -> 1 交出雕花 -> 2 守护哥尔哈 60 点 -> 3 回报帕赫曼 -> 4 向斯库顿报告。
	 * 25304: every journal row from the Fachmann dialog up to the Skuldun report must be reachable.
	 */
	@Test
	void quest25304RebuildsEveryJournalRowUpToTheSkuldunReport() throws Exception {
		CompiledQuestDefinition compiled = load(25304);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		assertEquals(0, layout.field("var0").offset());
		assertEquals(6, layout.field("var1").offset());
		assertEquals(60, layout.field("var1").maxValue());
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 4, "var1", 60));

		QuestTransition patternCheck = definition.transitions().stream()
			.filter(transition -> "s1".equals(transition.sourceNode()) && "s2".equals(transition.targetNode()))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(805340,
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id())))
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.HasItem(182215850, 1)), patternCheck.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), patternCheck.afterCommit());

		QuestSnapshot state = new QuestSnapshot(7, 25304, QuestStatus.START, layout.pack(Map.of("var0", 0, "var1", 0)), Map.of());
		state = apply(compiled, state, new QuestEvent.TalkToNpc(805340, QuestDialogAction.SETPRO1.id()));
		assertEquals(1, unpack(state, layout).get("var0"), "SETPRO1 must unlock the crafted-pattern row");

		QuestSnapshot withoutPattern = apply(compiled, state, new QuestEvent.TalkToNpc(805340,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(1, unpack(withoutPattern, layout).get("var0"),
			"a missing pattern must keep the journal on row 1");

		QuestSnapshot withPattern = new QuestSnapshot(7, 25304, QuestStatus.START, state.packedVariables(),
			Map.of(182215850, 1));
		state = apply(compiled, withPattern, new QuestEvent.TalkToNpc(805340,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(2, unpack(state, layout).get("var0"), "handing the pattern over must open the Gorha row");

		for (int kill = 1; kill <= 60; kill++) {
			final int killIndex = kill;
			state = apply(compiled, state, new QuestEvent.KillNpc(233902));
			final Map<String, Integer> variables = unpack(state, layout);
			assertEquals(killIndex, variables.get("var1"), () -> "kill " + killIndex + " must advance SECTION_1");
			assertEquals(killIndex == 60 ? 3 : 2, variables.get("var0"),
				() -> "kill " + killIndex + " must stay on the Gorha row until the sixtieth kill opens the report row");
		}
		assertEquals(3, unpack(state, layout).get("var0"), "60 Gorha kills must open the Fachmann-report row");

		QuestSnapshot withPrototype = new QuestSnapshot(7, 25304, QuestStatus.START, state.packedVariables(),
			Map.of(182215850, 1));
		state = apply(compiled, withPrototype, new QuestEvent.TalkToNpc(805340, QuestDialogAction.SET_SUCCEED.id()));
		assertEquals(QuestStatus.REWARD, state.status());
		assertEquals(Map.of("var0", 4, "var1", 60), unpack(state, layout));
	}

	/**
	 * 25604：s2 的 SECTION_1 计数事件（703125 x3）与 reward 的报告行（客户端第 5 行）。
	 * 25604: the s2 SECTION_1 counting event (703125 x3) and the reward report row (client row 5).
	 */
	@Test
	void quest25604KeepsTheKillCounterAndProjectsTheReportRow() throws Exception {
		CompiledQuestDefinition compiled = load(25604);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		assertEquals(6, layout.field("var1").offset());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 5, "var1", 3));

		boolean countingEvent = definition.transitions().stream()
			.filter(transition -> "s2".equals(transition.sourceNode()) && "s3".equals(transition.targetNode()))
			.anyMatch(transition -> transition.event() instanceof QuestEvent.KillNpc kill
				&& kill.npcId() == 703125);
		assertTrue(countingEvent, "quest 25604 must keep the SECTION_1 counting event on the s2 kill row");

		QuestSnapshot state = new QuestSnapshot(7, 25604, QuestStatus.START,
			layout.pack(Map.of("var0", 2, "var1", 0)), Map.of());
		for (int kill = 1; kill <= 3; kill++) {
			final int killIndex = kill;
			state = apply(compiled, state, new QuestEvent.KillNpc(703125));
			final Map<String, Integer> variables = unpack(state, layout);
			assertEquals(killIndex, variables.get("var1"), () -> "kill " + killIndex + " must advance SECTION_1");
			assertEquals(killIndex == 3 ? 3 : 2, variables.get("var0"),
				() -> "kill " + killIndex + " must keep the client journal on the expected row");
		}

		state = apply(compiled, state, new QuestEvent.TalkToNpc(806173, QuestDialogAction.SETPRO4.id()));
		assertEquals(4, unpack(state, layout).get("var0"), "SETPRO4 must open the clue row");

		QuestSnapshot withClue = new QuestSnapshot(7, 25604, QuestStatus.START, state.packedVariables(),
			Map.of(182216003, 5));
		state = apply(compiled, withClue, new QuestEvent.TalkToNpc(806173,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(QuestStatus.REWARD, state.status());
		assertEquals(Map.of("var0", 5, "var1", 3), unpack(state, layout));
	}

	/**
	 * 14252/24252：SECTION_0 是任务说明行索引（0/1/2 三行击杀），SECTION_1 是当前行计数，
	 * 三行打完后停在报告行，向报告 NPC 交付后才进入 REWARD。
	 * 14252/24252: SECTION_0 steps through the three kill rows and the NPC report closes the quest.
	 */
	@Test
	void quest14252And24252StepSectionZeroThroughEveryKillRow() throws Exception {
		record Case(int questId, int startNpc, int reportNpc, int rowOneMob, int rowTwoMob, int rowThreeMob) {
		}
		for (Case testCase : List.of(
			new Case(14252, 805736, 832824, 213775, 213780, 237275),
			new Case(24252, 805737, 832820, 213775, 213780, 237275))) {
			CompiledQuestDefinition compiled = load(testCase.questId());
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			assertEquals(0, layout.field("var0").offset());
			assertEquals(6, layout.field("var1").offset());
			assertNode(definition, "r0", QuestStatus.START, Map.of("var0", 0, "var1", 0));
			assertNode(definition, "r1", QuestStatus.START, Map.of("var0", 1, "var1", 0));
			assertNode(definition, "r2", QuestStatus.START, Map.of("var0", 2, "var1", 0));
			assertNode(definition, "r3", QuestStatus.START, Map.of("var0", 3, "var1", 1));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3, "var1", 1));

			boolean reportRoute = definition.transitions().stream()
				.filter(transition -> "r3".equals(transition.sourceNode()) && "reward".equals(transition.targetNode()))
				.anyMatch(transition -> transition.event().equals(new QuestEvent.TalkToNpc(testCase.reportNpc(),
					QuestDialogAction.SELECT_QUEST_REWARD.id())));
			assertTrue(reportRoute, () -> "quest " + testCase.questId() + " must report from the SECTION_0=3 row");

			// 跨部署保持在线的旧存档靠报告 NPC 的无 source 自愈路线补齐行索引；该路线与既有
			// reward->reward QUEST_SELECT 路线在编译期来源节点互斥，因此可以并存。
			// Stale saves that stayed online self-heal on the report NPC; the route coexists with the
			// reward->reward QUEST_SELECT route because their compatible source nodes are disjoint.
			boolean talkSelfHeal = definition.transitions().stream()
				.filter(transition -> transition.sourceNode() == null)
				.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(testCase.reportNpc(),
					QuestDialogAction.QUEST_SELECT.id())))
				.anyMatch(transition -> transition.conditions().stream()
					.anyMatch(condition -> condition instanceof QuestCondition.VariableBelow(String field, int value)
						&& field.equals("var0") && value == 3)
					&& transition.actions().contains(new QuestAction.SetVariable("var0", 3)));
			assertTrue(talkSelfHeal, () -> "quest " + testCase.questId()
				+ " must keep the report-NPC talk self-heal for stale SECTION_0 rows");

			QuestSnapshot state = new QuestSnapshot(7, testCase.questId(), QuestStatus.START,
				layout.pack(Map.of("var0", 0, "var1", 0)), Map.of());
			// r1/r2 投影仍保留旧 grid 的 var2 迁移标记，因此逐字段断言而不是整表相等。
			// The r1/r2 projections still carry the legacy grid migration marker var2, so assert field by field.
			state = apply(compiled, state, new QuestEvent.KillNpc(testCase.rowOneMob()));
			final Map<String, Integer> rowOne = unpack(state, layout);
			assertEquals(1, rowOne.get("var0"),
				() -> "quest " + testCase.questId() + " must open row 1");
			assertEquals(0, rowOne.get("var1"),
				() -> "quest " + testCase.questId() + " must clear the row counter on row 1");
			state = apply(compiled, state, new QuestEvent.KillNpc(testCase.rowTwoMob()));
			final Map<String, Integer> rowTwo = unpack(state, layout);
			assertEquals(2, rowTwo.get("var0"),
				() -> "quest " + testCase.questId() + " must open row 2");
			assertEquals(0, rowTwo.get("var1"),
				() -> "quest " + testCase.questId() + " must clear the row counter on row 2");
			state = apply(compiled, state, new QuestEvent.KillNpc(testCase.rowThreeMob()));
			final Map<String, Integer> rowThree = unpack(state, layout);
			assertEquals(3, rowThree.get("var0"),
				() -> "quest " + testCase.questId() + " must stop on the report row");
			assertEquals(1, rowThree.get("var1"),
				() -> "quest " + testCase.questId() + " must mark the report row counter as complete");
			assertEquals(QuestStatus.START, state.status());

			state = apply(compiled, state, new QuestEvent.TalkToNpc(testCase.reportNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD.id()));
			assertEquals(QuestStatus.REWARD, state.status());
		}
	}

	/**
	 * 23918：SECTION_0..4 链式门控 5 名精锐兵（旧 XML 误配 EvGuard 怪物与 4 维计数）。
	 * 23918: SECTION_0..4 chain the five elite raiders (the previous XML used the wrong EvGuard mobs).
	 */
	@Test
	void quest23918ChainsFiveKillerCountersOnTheClientSections() throws Exception {
		CompiledQuestDefinition compiled = load(23918);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		Map<Integer, String> counters = new LinkedHashMap<>();
		counters.put(235559, "var0");
		counters.put(235560, "var1");
		counters.put(235561, "var2");
		counters.put(235326, "var3");
		counters.put(235327, "var4");
		int index = 0;
		for (Map.Entry<Integer, String> entry : counters.entrySet()) {
			BitField field = layout.field(entry.getValue());
			assertNotNull(field, () -> "quest 23918 must declare " + entry.getValue());
			assertEquals(6 * index, field.offset(), () -> entry.getValue() + " must map its client SECTION");
			// counter-grid 逐维展开成单 npc 的 KillNpc 路线（不是 KillNpcSet），计数靠目标节点投影推进。
			// The counter-grid expands one single-NPC KillNpc route per dimension and advances via the target projection.
			assertTrue(definition.transitions().stream()
					.filter(transition -> transition.event() instanceof QuestEvent.KillNpc kill
						&& kill.npcId() == entry.getKey())
					.anyMatch(transition -> definition.nodes().stream()
						.filter(node -> node.label().equals(transition.targetNode()))
						.anyMatch(node -> Integer.valueOf(1).equals(
							node.projection().variables().get(entry.getValue())))),
				() -> "quest 23918 must count " + entry.getKey() + " on " + entry.getValue());
			index++;
		}
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of(
			"var0", 1, "var1", 1, "var2", 1, "var3", 1, "var4", 1));

		QuestSnapshot state = new QuestSnapshot(7, 23918, QuestStatus.START, 0, Map.of());
		for (int npcId : counters.keySet()) {
			state = apply(compiled, state, new QuestEvent.KillNpc(npcId));
		}
		assertEquals(Map.of("var0", 1, "var1", 1, "var2", 1, "var3", 1, "var4", 1), unpack(state, layout));
		state = apply(compiled, state, new QuestEvent.TalkToNpc(802347, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(QuestStatus.REWARD, state.status());
	}

	/**
	 * 10101/20101：按客户端 Progress(2~!4) 仅通过单变量 var0 进行阶段行走（2->3->4）。
	 * 严禁在高位声明或写入多余的 var2（SECTION_2），否则步数被打包为 4099/8196 导致客户端 HTML 渲染完全崩溃。
	 * 10101/20101: walk phase steps only via the single variable var0 per client Progress(2~!4) (2->3->4).
	 * Extra high-bit fields such as var2 (SECTION_2) are forbidden because packed steps like 4099/8196 break client HTML.
	 */
	@Test
	void quest10101And20101WalkPhaseStepsWithoutCorruptingPackedStep() throws Exception {
		for (int questId : List.of(10101, 20101)) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			assertNotNull(layout.field("var0"), () -> "quest " + questId + " must declare var0");
			assertNull(layout.field("var2"), () -> "quest " + questId + " must NOT declare var2");

			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START,
				layout.pack(Map.of("var0", 2)), Map.of());
			state = apply(compiled, state, new QuestEvent.KillNpc(234680));
			assertEquals(Map.of("var0", 3), unpack(state, layout));
			assertEquals(3, state.packedVariables(), () -> "quest " + questId + " step must be exactly 3, not corrupted by high bits");

			state = apply(compiled, state, new QuestEvent.KillNpc(234680));
			assertEquals(Map.of("var0", 4), unpack(state, layout));
			assertEquals(4, state.packedVariables(), () -> "quest " + questId + " step must be exactly 4, not corrupted by high bits");
		}
	}

	/**
	 * 10101/20101：向波尔迪安 (802357) / 基西安 (802361) 交付计划书后处于 s8（var0=8），
	 * 点击「移动到沙帕灵开拓地」(SET_SUCCEED 10255) 必须顺利切入 REWARD 态，无需不存在的幻象道具门禁。
	 * 10101/20101: at s8 (var0=8) after submitting plans to Voltin (802357) / Kisian (802361),
	 * clicking SET_SUCCEED (10255) must transition to REWARD status without phantom item gates.
	 */
	@Test
	void quest10101And20101AdvanceToRewardWithoutPhantomItem() throws Exception {
		Map<Integer, Integer> npcs = Map.of(10101, 802357, 20101, 802361);
		for (var entry : npcs.entrySet()) {
			int questId = entry.getKey();
			int npcId = entry.getValue();
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();

			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START,
				layout.pack(Map.of("var0", 8)), Map.of());
			state = apply(compiled, state, new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SET_SUCCEED.id()));
			assertEquals(QuestStatus.REWARD, state.status(),
				() -> "quest " + questId + " should advance to REWARD status upon SET_SUCCEED with npc " + npcId);
			assertEquals(Map.of("var0", 8), unpack(state, layout),
				() -> "quest " + questId + " should retain var0 at 8 upon SET_SUCCEED with npc " + npcId);
		}
	}

	/**
	 * 10101/20101：向波尔迪安 (802357) / 基西安 (802361) 提交进攻计划书只需持有计划书本身 (182215452 / 182215453)，
	 * 不得额外强求副本钥匙 (182215520 / 182215522)，钥匙在使用秘密回廊 (731532) 时即已消耗。
	 * 10101/20101: submitting the invasion plan to Voltin (802357) / Kisian (802361) only requires the plan item,
	 * not the dungeon key (182215520 / 182215522) which was already consumed upon using corridor portal 731532.
	 */
	@Test
	void quest10101And20101SubmitInvasionPlanOnlyRequiresPlanItem() throws Exception {
		Map<Integer, Integer> npcs = Map.of(10101, 802357, 20101, 802361);
		Map<Integer, Integer> items = Map.of(10101, 182215452, 20101, 182215453);
		for (var entry : npcs.entrySet()) {
			int questId = entry.getKey();
			int npcId = entry.getValue();
			int planItemId = items.get(questId);
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();

			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START,
				layout.pack(Map.of("var0", 7)), Map.of(planItemId, 1));
			state = apply(compiled, state, new QuestEvent.TalkToNpc(npcId, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
			assertEquals(QuestStatus.START, state.status());
			assertEquals(Map.of("var0", 8), unpack(state, layout),
				() -> "quest " + questId + " should advance var0 to 8 upon presenting invasion plan");
		}
	}

    /**
	 * 10526/20526：在 s11 向觉醒的德贾博 (806292 / 806297) 结算切入 REWARD 态，
	 * 仅需扣除真正持有的任务道具 (182216074 / 182216086)，严禁因未曾发放的幻象道具 (164002347 / 164002348) 阻断交付。
	 * 10526/20526: settling with Awakened Dezabo (806292 / 806297) at s11 into REWARD status
	 * only removes the actual quest item held, without being blocked by phantom items (164002347 / 164002348).
	 */
	@Test
	void quest10526And20526AdvanceToRewardWithoutPhantomItem() throws Exception {
		Map<Integer, Integer> npcs = Map.of(10526, 806292, 20526, 806297);
		Map<Integer, Integer> items = Map.of(10526, 182216074, 20526, 182216086);
		for (var entry : npcs.entrySet()) {
			int questId = entry.getKey();
			int npcId = entry.getValue();
			int itemId = items.get(questId);
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();

			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START,
				layout.pack(Map.of("var0", 11)), Map.of(itemId, 1));
			state = apply(compiled, state, new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SET_SUCCEED.id()));
			assertEquals(QuestStatus.REWARD, state.status(),
				() -> "quest " + questId + " should advance to REWARD status upon SET_SUCCEED with npc " + npcId);
			assertEquals(Map.of("var0", 12), unpack(state, layout),
				() -> "quest " + questId + " should set var0 to 12 upon SET_SUCCEED with npc " + npcId);
		}
	}

	@Test
	void midRouteJournalRowAdvancesCarryAVisibilityRefresh() throws Exception {
		record RowAdvance(int questId, String source, String target) {
		}
		List<RowAdvance> advances = List.of(
			new RowAdvance(10101, "s3", "s4"), new RowAdvance(10101, "s4", "s5"),
			new RowAdvance(10101, "s5", "s6"), new RowAdvance(10101, "s6", "s7"),
			new RowAdvance(10101, "s7", "s8"),
			new RowAdvance(20101, "s3", "s4"), new RowAdvance(20101, "s4", "s5"),
			new RowAdvance(20101, "s5", "s6"), new RowAdvance(20101, "s6", "s7"),
			new RowAdvance(20101, "s7", "s8"),
			new RowAdvance(14021, "s6", "s7"), new RowAdvance(24014, "s4", "s5"));
		for (RowAdvance advance : advances) {
			CompiledQuestDefinition compiled = load(advance.questId());
			QuestTransition transition = compiled.definition().transitions().stream()
				.filter(candidate -> advance.source().equals(candidate.sourceNode())
					&& advance.target().equals(candidate.targetNode()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest " + advance.questId() + " has no "
					+ advance.source() + "->" + advance.target() + " route"));
			boolean refreshed = transition.afterCommit().stream()
				.filter(AfterCommitAction.SyncQuestState.class::isInstance)
				.map(AfterCommitAction.SyncQuestState.class::cast)
				.anyMatch(sync -> sync.mode().refreshVisibility());
			assertTrue(refreshed, () -> "quest " + advance.questId() + " " + advance.source() + "->"
				+ advance.target() + " advances the journal row and must refresh client visibility");
		}
	}

	private static Map<String, Integer> unpack(QuestSnapshot snapshot, ProgressLayout layout) {
		return layout.unpack(snapshot.packedVariables());
	}

	private static boolean isKillEvent(QuestEvent event) {
		return event instanceof QuestEvent.KillNpc || event instanceof QuestEvent.KillNpcSet;
	}

	private static QuestSnapshot apply(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestTransition> candidates = definition.transitionsFor(event.type()).stream()
			.filter(transition -> QuestEvent.matches(transition.event(), event))
			.sorted(Comparator.comparingInt(transition -> transition.priority() == null
				? Integer.MAX_VALUE : transition.priority()))
			.toList();
		for (QuestTransition transition : candidates) {
			var plan = QuestMutationPlanner.plan(definition, snapshot, event, transition);
			if (plan.isPresent()) {
				QuestMutationPlan mutation = plan.orElseThrow();
				return new QuestSnapshot(7, definition.id(), mutation.nextStatus(),
					mutation.nextPackedVariables(), Map.of());
			}
		}
		throw new AssertionError("no route for quest " + definition.id() + " event " + event);
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
		assertEquals(status, node.projection().status(), () -> "status mismatch on node " + label);
		assertEquals(variables, node.projection().variables(), () -> "variables mismatch on node " + label);
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestMonsterProgressContractAuditTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
