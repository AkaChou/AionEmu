package com.aionemu.gameserver.questEngine.definition;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 客户端任务书进度合同审计（client-monster-progress-contracts.csv 的服务端定义对拍）：
 * 逐族核对 SECTION 归属（打包 quest_vars int 的 6 位段 [6n, 6n+6)）、计数链推进、
 * 报告行投影与自愈边；混合链行按客户端 SECTION 对齐形（行阶梯 + 段计数）审计。
 * Client journal progress-contract audit (against client-monster-progress-contracts.csv):
 * per-family checks of SECTION ownership (6-bit slots [6n, 6n+6) of the packed quest-vars int),
 * counter-chain advances, report-row projections and heal edges; mixed-chain rows are audited in
 * the client SECTION-aligned shape (row ladder plus stage counters).
 */
class QuestMonsterProgressContractAuditTest {
	private static final Path CONTRACTS_CSV = Path.of("docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv");
	private static final List<Integer> SPECIAL_MISSIONS_1_TO_24 = List.of(
		19631, 19632, 19633, 19634, 19635, 19636, 19637, 19638, 19639, 19640, 19641, 19642,
		29631, 29632, 29633, 29634, 29635, 29636, 29637, 29638, 29639, 29640, 29641, 29642);
	private static final List<Integer> STEP_ZERO_SEQUENTIAL_CHAINS = List.of(
		15001, 15020, 15073, 15100, 15104, 15203, 15406, 15407, 15408, 15580, 25060);
	private static final List<Integer> STEP_ZERO_XML_RETAINED = List.of(15671, 25671, 18952);

	/**
	 * 特殊任务 1..24（双阵营）：网格形按网格合同、计数形按 var0 行标记 + var1 计数（SECTION_0/1）审计，
	 * 继续边只加计数、完成边落行并保留满计数、满计数 1009 自愈进领奖。
	 * Special missions 1..24 (both factions): grid-shaped rows follow the grid contract, counted rows
	 * follow var0 row marker plus var1 counter (SECTION_0/1) — continuing routes only increment,
	 * the completing route lands the row keeping the full count, and full counters self-heal into
	 * reward via 1009.
	 */
	@Test
	void specialMissionsElyosAndAsmodiansStrictlyAlignWithClientStepAndKillCounterSeparation() throws Exception {
		for (int questId : SPECIAL_MISSIONS_1_TO_24) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			if (layout.field("var1") == null) {
				assertGridSpecialMission(compiled, definition);
				continue;
			}
			BitField var0 = layout.field("var0");
			BitField var1 = layout.field("var1");
			Assertions.assertNotNull(var0, () -> "quest " + questId + " must declare var0");
			Assertions.assertNotNull(var1, () -> "quest " + questId + " must declare var1 for kill counter");
			Assertions.assertEquals(0, var0.offset());
			Assertions.assertEquals(6, var1.offset());
			assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0));
			assertNode(definition, "started", QuestStatus.START, Map.of());
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 10));
			assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0));
			List<QuestTransition> continuingKills = definition.transitions().stream()
				.filter(transition -> isKillEvent(transition.event()))
				.filter(transition -> transition.sourceNode().equals("started")
					&& transition.targetNode().equals("started"))
				.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
				.toList();
			Assertions.assertFalse(continuingKills.isEmpty(),
				() -> "quest " + questId + " must have continuing kill route");
			for (QuestTransition transition : continuingKills) {
				Assertions.assertTrue(transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.IncrementVariable(var field, var delta)
							&& field.equals("var1") && delta == 1),
					() -> "quest " + questId + " continuing kill route must increment var1");
				Assertions.assertFalse(transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.IncrementVariable(var field, var delta)
							&& field.equals("var0")),
					() -> "quest " + questId + " continuing kill route must not increment var0");
				Assertions.assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					transition.afterCommit());
			}
			List<QuestTransition> completingKills = definition.transitions().stream()
				.filter(transition -> isKillEvent(transition.event()))
				.filter(transition -> transition.sourceNode().equals("started")
					&& transition.targetNode().equals("reward"))
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.toList();
			Assertions.assertFalse(completingKills.isEmpty(),
				() -> "quest " + questId + " must have priority 0 completing kill route");
			for (QuestTransition transition : completingKills) {
				Assertions.assertTrue(transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.SetVariable(var field, var value)
							&& field.equals("var0") && value == 1),
					() -> "quest " + questId + " completing route must set var0=1");
				Assertions.assertTrue(transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.SetVariable(var field, var value)
							&& field.equals("var1") && value == 10),
					() -> "quest " + questId + " completing route must set var1=10");
				Assertions.assertEquals(
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
					transition.afterCommit());
			}
			boolean hasRecoveredReport = definition.transitions().stream()
				.filter(transition -> transition.sourceNode().equals("started")
					&& transition.targetNode().equals("reward"))
				.anyMatch(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& (talk.dialogId() == null || talk.dialogId() == 31 || talk.dialogId() == 1009)
					&& transition.conditions().stream().anyMatch(condition ->
						condition instanceof QuestCondition.VariableAtLeast(var field, var value)
							&& field.equals("var1") && value == 10));
			Assertions.assertTrue(hasRecoveredReport,
				() -> "quest " + questId + " must provide recovered report route for full counters");
		}
	}

	/**
	 * 网格形特殊任务（P0-2 DD 切片后规范形）：零链态击杀边 PACKET_ONLY、满段 QUEST_SELECT 直翻
	 * REWARD + 分档查表窗（QE-028），未满段零对话路由（关窗兜底，"未满上交保持门禁"由零路由更强承接）。
	 * Grid-shaped special missions (canonical since the P0-2 DD slice): zero-node kill edges stay
	 * PACKET_ONLY, the full-node QUEST_SELECT flips REWARD with the tiered window (QE-028), and
	 * incomplete stages have zero dialog routes (close fallback — a stronger gate than the old
	 * conditional turn-in).
	 */
	private static void assertGridSpecialMission(CompiledQuestDefinition compiled, QuestDefinition definition) {
		int questId = definition.id();
		BitField var0 = definition.progressLayout().field("var0");
		Assertions.assertNotNull(var0, () -> "quest " + questId + " must declare var0");
		Assertions.assertEquals(0, var0.offset());
		QuestNode reward = definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.REWARD).findFirst().orElseThrow();
		Map<String, Integer> zeroVars = Map.of("var0", 0);
		QuestNode zero = definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.START
				&& node.projection().variables().equals(zeroVars))
			.findFirst().orElseThrow();
		QuestNode full = definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.START
				&& node.projection().variables().equals(reward.projection().variables()))
			.findFirst().orElseThrow();
		List<QuestTransition> killEdges = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc
				|| transition.event() instanceof QuestEvent.KillNpcSet)
			.filter(transition -> zero.label().equals(transition.sourceNode()))
			.toList();
		Assertions.assertFalse(killEdges.isEmpty(), () -> "quest " + questId + " must have kill edges from a0");
		for (QuestTransition edge : killEdges) {
			Assertions.assertEquals(List.of(), edge.conditions(), () -> "quest " + questId + " kill conditions");
			Assertions.assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				edge.afterCommit(), () -> "quest " + questId + " kill sync");
		}
		int reportNpc = definition.transitions().stream()
			.filter(transition -> full.label().equals(transition.sourceNode())
				&& "reward".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
			.mapToInt(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.findFirst().orElseThrow();
		int rewardWindow = QuestDialogPage.rewardWindowForTier(
			definition.metadata().rewardGroups().size() - 1).orElseThrow().id();
		Assertions.assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(rewardWindow)),
			definition.transitions().stream()
				.filter(transition -> full.label().equals(transition.sourceNode())
					&& "reward".equals(transition.targetNode())
					&& transition.event().equals(new QuestEvent.TalkToNpc(reportNpc,
						QuestDialogAction.QUEST_SELECT.id())))
				.findFirst().orElseThrow().afterCommit(),
			() -> "quest " + questId + " canonical delivery protocol");
		Assertions.assertTrue(definition.transitions().stream().noneMatch(transition ->
				zero.label().equals(transition.sourceNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == reportNpc
					&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
			() -> "quest " + questId + " incomplete stages must have zero dialog routes");
	}

	/**
	 * 复杂/多维击杀任务的 SECTION 对齐：并行计数行 var1/var2 各占 SECTION_1/2，两段链不声明 var2，
	 * 四段链 var0..3 逐位对齐且领奖投影满链态。
	 * SECTION alignment for complex/multi-dimension kill quests: parallel counter rows own
	 * var1/var2 (SECTION_1/2), two-stage chains declare no var2, and four-stage chains align
	 * var0..3 bit-for-bit with the full chain state in the reward projection.
	 */
	@Test
	void complexAndMultiDimensionKillQuestsAlignWithClientSections() throws Exception {
		List<Integer> parallelQuests = List.of(13945, 18994, 28994, 13705, 17510, 27510, 10112, 20112, 10011, 20011);
		List<Integer> twoStageChains = List.of(25406, 25407, 25408, 25580);
		List<Integer> fourStageChains = List.of(15546, 25546);
		for (int questId : parallelQuests) {
			CompiledQuestDefinition compiled = load(questId);
			ProgressLayout layout = compiled.definition().progressLayout();
			BitField var0 = layout.field("var0");
			BitField var1 = layout.field("var1");
			Assertions.assertNotNull(var0, () -> "quest " + questId + " must declare var0");
			Assertions.assertNotNull(var1, () -> "quest " + questId + " must declare var1");
			Assertions.assertEquals(0, var0.offset());
			Assertions.assertEquals(6, var1.offset());
			if (List.of(17510, 27510, 10112, 20112).contains(questId)) {
				BitField var2 = layout.field("var2");
				Assertions.assertNotNull(var2, () -> "quest " + questId + " must declare var2");
				Assertions.assertEquals(12, var2.offset());
			}
		}
		Map<Integer, Map<String, Integer>> twoStageFull = Map.of(
			25406, Map.of("var0", 4, "var1", 4),
			25407, Map.of("var0", 4, "var1", 4),
			25408, Map.of("var0", 4, "var1", 4),
			25580, Map.of("var0", 20, "var1", 20));
		for (int questId : twoStageChains) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			Assertions.assertNull(layout.field("var2"),
				() -> "quest " + questId + " is a two-stage chain and must not declare var2");
			assertRewardProjection(definition, questId, twoStageFull.get(questId));
		}
		for (int questId : fourStageChains) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			for (int index = 0; index <= 3; index++) {
				final int slot = index;
				BitField varField = layout.field("var" + index);
				Assertions.assertNotNull(varField, () -> "quest " + questId + " must declare var" + slot);
				Assertions.assertEquals(6 * index, varField.offset(),
					() -> "quest " + questId + " var" + slot + " must map its chain slot");
			}
			Assertions.assertNull(layout.field("var4"),
				() -> "quest " + questId + " is a four-stage chain and must not declare var4");
			assertRewardProjection(definition, questId, Map.of("var0", 4, "var1", 4, "var2", 4, "var3", 4));
		}
	}

	/** 领奖节点投影合同：REWARD 状态 + 满链态变量。 */
	/** The reward-node projection contract: REWARD status plus the full chain-state variables. */
	private static void assertRewardProjection(QuestDefinition definition, int questId,
			Map<String, Integer> expected) {
		QuestNode reward = definition.nodes().stream()
			.filter(node -> node.label().equals("reward"))
			.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " must define a reward node"));
		Assertions.assertEquals(QuestStatus.REWARD, reward.projection().status());
		Assertions.assertEquals(expected, reward.projection().variables(),
			() -> "quest " + questId + " reward must project the full chain state");
	}

	/** 修复过的多杀任务（15324/50091/50092）：var0 行标记 + var1 计数各占 SECTION_0/1。 */
	/** Repaired multi-kill quests (15324/50091/50092): var0 row marker and var1 counter own
	 * SECTION_0/1. */
	@Test
	void repairedMultiKillQuestsDeclareSeparateKillCounters() throws Exception {
		for (int questId : List.of(15324, 50091, 50092)) {
			CompiledQuestDefinition compiled = load(questId);
			ProgressLayout layout = compiled.definition().progressLayout();
			Assertions.assertNotNull(layout.field("var0"), "quest " + questId + " must declare var0");
			Assertions.assertNotNull(layout.field("var1"), "quest " + questId + " must declare var1");
			Assertions.assertEquals(0, layout.field("var0").offset());
			Assertions.assertEquals(6, layout.field("var1").offset());
		}
	}

	/**
	 * step0 多计数 hunt 行（顺序链）：零链态击杀只推进段 1、满链态 1009 无门禁进领奖、
	 * 未满上交保持门禁。
	 * step0 multi-counter hunt rows (sequential chains): zero-node kills advance stage 1 only, the
	 * full-chain 1009 reports ungated, and early turn-ins stay gated.
	 */
	@Test
	void stepZeroMultiCounterHuntsWalkSequentialStagesToTheReport() throws Exception {
		for (int questId : STEP_ZERO_SEQUENTIAL_CHAINS) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			QuestNode reward = definition.nodes().stream()
				.filter(node -> node.label().equals("reward"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest " + questId + " must define a reward node"));
			Map<String, Integer> full = reward.projection().variables();
			Assertions.assertFalse(full.isEmpty(), () -> "quest " + questId + " reward must project the chain state");
			QuestNode fullChainNode = definition.nodes().stream()
				.filter(node -> node.projection().status() == QuestStatus.START
					&& node.projection().variables().equals(full))
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest " + questId + " must declare the full chain node"));
			Map<String, Integer> zero = new LinkedHashMap<>();
			full.forEach((field, value) -> zero.put(field, 0));
			QuestNode zeroNode = definition.nodes().stream()
				.filter(node -> node.projection().status() == QuestStatus.START
					&& node.projection().variables().equals(zero))
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest " + questId + " must declare the zero chain node"));
			List<QuestTransition> killEdges = definition.transitions().stream()
				.filter(transition -> zeroNode.label().equals(transition.sourceNode()))
				.filter(QuestMonsterProgressContractAuditTest::isKillTransition)
				.toList();
			Assertions.assertFalse(killEdges.isEmpty(),
				() -> "quest " + questId + " must carry stage-1 kill edges from the zero node");
			Map<String, Integer> firstStep = new LinkedHashMap<>(zero);
			firstStep.put("var0", 1);
			for (QuestTransition edge : killEdges) {
				Assertions.assertEquals(List.of(), edge.conditions(),
					() -> "quest " + questId + " kill edges count by projection");
				Assertions.assertEquals(firstStep, node(definition, edge.targetNode()).projection().variables(),
					() -> "quest " + questId + " zero-node kills must advance stage 1 only");
				Assertions.assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					edge.afterCommit(), () -> "quest " + questId + " kill edges must stay PACKET_ONLY");
			}
			// P0-2 顺序链规范形：满链 QUEST_SELECT 无门禁直翻领奖并按档位查表下发奖励窗；
			// 零链节点无 QUEST_SELECT/1009 报告通道（提前上交不可达）。
			// Canonical sequential shape: the full chain's QUEST_SELECT flips reward ungated with
			// the tiered window; the zero chain node keeps no QUEST_SELECT/1009 report channel.
			int reportNpc = definition.transitions().stream()
				.filter(transition -> fullChainNode.label().equals(transition.sourceNode())
					&& "reward".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
				.mapToInt(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
				.findFirst().orElseThrow();
			QuestTransition report = definition.transitions().stream()
				.filter(transition -> fullChainNode.label().equals(transition.sourceNode())
					&& "reward".equals(transition.targetNode())
					&& transition.event().equals(new QuestEvent.TalkToNpc(reportNpc,
						QuestDialogAction.QUEST_SELECT.id())))
				.findFirst().orElseThrow();
			Assertions.assertEquals(List.of(), report.conditions(),
				() -> "quest " + questId + " full-chain delivery must stay ungated");
			Assertions.assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
					definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
				report.afterCommit(), () -> "quest " + questId + " delivery protocol");
			Assertions.assertTrue(definition.transitions().stream().noneMatch(transition ->
				zeroNode.label().equals(transition.sourceNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == reportNpc && talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
				() -> "quest " + questId + " 零链节点不得保留报告通道路由");
		}
	}

	private static boolean isKillTransition(QuestTransition transition) {
		return isKillEvent(transition.event());
	}

	/**
	 * XML 保留的 step0 行：REWARD 段 SECTION_0=1、进世界自愈边修复 SECTION_0=0 的旧存档、
	 * 完成边落 SECTION_0=1 且继续边钉住 SECTION_0=0。
	 * XML-retained step0 rows: the reward step owns SECTION_0=1, an enter-world heal edge repairs
	 * legacy SECTION_0=0 saves, completing kills set SECTION_0=1 and continuing kills pin it to 0.
	 */
	@Test
	void retainedStepZeroRowsKeepTheLegacyReportStepContract() throws Exception {
		for (int questId : STEP_ZERO_XML_RETAINED) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			QuestNode reward = definition.nodes().stream()
				.filter(node -> node.label().equals("reward"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest " + questId + " must define a reward node"));
			Assertions.assertEquals(1, reward.projection().variables().get("var0"),
				() -> "quest " + questId + " reward must project SECTION_0=1");
			boolean hasMigrationRepair = definition.transitions().stream()
				.filter(transition -> transition.sourceNode() == null)
				.filter(transition -> transition.targetNode().equals("reward"))
				.filter(transition -> transition.event() instanceof QuestEvent.EnterWorld)
				.anyMatch(transition -> transition.conditions().stream().anyMatch(condition ->
					condition instanceof QuestCondition.StatusIs status && status.status() == QuestStatus.REWARD)
					&& transition.conditions().stream().anyMatch(condition ->
						condition instanceof QuestCondition.QuestVariableIs variable
							&& variable.field().equals("var0") && variable.value() == 0)
					&& transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.SetVariable(var field, var value)
							&& field.equals("var0") && value == 1)
					&& transition.afterCommit().contains(
						new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)));
			Assertions.assertTrue(hasMigrationRepair,
				() -> "quest " + questId + " must repair legacy REWARD SECTION_0=0 saves");
			List<QuestTransition> completing = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(QuestMonsterProgressContractAuditTest::isKillTransition)
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.toList();
			Assertions.assertFalse(completing.isEmpty(),
				() -> "quest " + questId + " must have a completing kill route");
			for (QuestTransition transition : completing) {
				Assertions.assertTrue(transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.SetVariable(var field, var value)
							&& field.equals("var0") && value == 1),
					() -> "quest " + questId + " completing kill route must set SECTION_0=1");
				Assertions.assertTrue(transition.afterCommit().contains(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
					() -> "quest " + questId + " completing kill route must refresh visibility");
			}
			List<QuestTransition> continuing = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "started".equals(transition.targetNode()))
				.filter(QuestMonsterProgressContractAuditTest::isKillTransition)
				.toList();
			Assertions.assertFalse(continuing.isEmpty(),
				() -> "quest " + questId + " must have a continuing kill route");
			for (QuestTransition transition : continuing) {
				Assertions.assertTrue(transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.SetVariable(var field, var value)
							&& field.equals("var0") && value == 0),
					() -> "quest " + questId + " continuing kill route must pin SECTION_0=0");
			}
		}
	}

	/**
	 * 15001（两段顺序链）：前 5 杀只满段 1，后 5 杀进满链态（packed=5+5*64=325），
	 * 满链 QUEST_SELECT 交付进领奖（P0-2 顺序链规范形，1009 中转删除）。
	 * 15001 (two-stage sequential chain): the first five kills fill stage 1 only, the next five
	 * reach the full chain state (packed 325), and the full-node QUEST_SELECT delivery enters
	 * reward (canonical since P0-2, no 1009 hop).
	 */
	@Test
	void quest15001FillsBothSequentialStagesBeforeTheRetailReport() throws Exception {
		CompiledQuestDefinition definition = load(15001);
		ProgressLayout layout = definition.definition().progressLayout();
		QuestSnapshot state = new QuestSnapshot(7, 15001, QuestStatus.START, 0, Map.of());
		for (int count = 0; count < 5; count++) {
			state = apply(definition, state, new QuestEvent.KillNpc(235790));
		}
		Assertions.assertEquals(Map.of("var0", 5, "var1", 0), unpack(state, layout),
			"stage 1 must fill first and keep stage 2 untouched");
		for (int count = 0; count < 5; count++) {
			state = apply(definition, state, new QuestEvent.KillNpc(235799));
		}
		Assertions.assertEquals(QuestStatus.START, state.status());
		Assertions.assertEquals(325, state.packedVariables());
		state = apply(definition, state, new QuestEvent.TalkToNpc(804698, QuestDialogAction.QUEST_SELECT.id()));
		Assertions.assertEquals(QuestStatus.REWARD, state.status());
		Assertions.assertEquals(Map.of("var0", 5, "var1", 5), unpack(state, layout));
	}

	/** 客户端进度合同 CSV 必须存在且列形完整（quest_id/step_section/counter_section/required）。 */
	/** The client progress-contract CSV must exist with its full header shape. */
	@Test
	void clientMonsterProgressContractsCsvExistsAndHasExpectedShape() throws Exception {
		Assertions.assertTrue(Files.exists(CONTRACTS_CSV, new LinkOption[0]),
			"contracts CSV must exist in docs/quest/client-dialog-mapping/");
		try (BufferedReader reader = Files.newBufferedReader(CONTRACTS_CSV, StandardCharsets.UTF_8)) {
			String header = reader.readLine();
			Assertions.assertNotNull(header);
			Assertions.assertTrue(header.contains("quest_id"));
			Assertions.assertTrue(header.contains("step_section"));
			Assertions.assertTrue(header.contains("counter_section"));
			Assertions.assertTrue(header.contains("required"));
			int count = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					count++;
				}
			}
			final int rowCount = count;
			Assertions.assertTrue(rowCount >= 800,
				() -> "expected at least 800 client monster progress contracts, found " + rowCount);
		}
	}

	/**
	 * 15301（{talk, collectitem} 单阶梯链）：SETPRO1 推进行阶梯、39 检查无货留行/整组过扣进交付行、
	 * SET_SUCCEED 授予凭证且领奖行 = QE-051 客户端末行。
	 * 15301 (the {talk, collectitem} single-ladder chain): SETPRO1 bumps the ladder, the 39 check
	 * stays without goods and hands the whole group over into the handover row, and SET_SUCCEED
	 * grants the voucher with the reward row equal to the QE-051 client last row.
	 */
	@Test
	void quest15301LaddersIntoTheCollectRowAndTheFinalSetSucceedGrantsTheVoucher() throws Exception {
		CompiledQuestDefinition compiled = load(15301);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3));
		QuestSnapshot state = new QuestSnapshot(7, 15301, QuestStatus.START, 0, Map.of());
		state = apply(compiled, state, new QuestEvent.TalkToNpc(805328, QuestDialogAction.SETPRO1.id()));
		Assertions.assertEquals(Map.of("var0", 1), unpack(state, layout),
			"SETPRO1 must bump the ladder onto the collect row");
		state = apply(compiled, state, new QuestEvent.TalkToNpc(805328,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		Assertions.assertEquals(Map.of("var0", 1), unpack(state, layout),
			"the check must stay on the collect row without goods");
		QuestTransition check = definition.transitions().stream()
			.filter(candidate -> "s1".equals(candidate.sourceNode()) && "s2".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id())
			.findFirst().orElseThrow(() -> new AssertionError("missing group check edge s1->s2"));
		Assertions.assertEquals(List.of(new QuestCondition.HasItem(182215829, 1),
			new QuestCondition.HasItem(182215830, 1), new QuestCondition.HasItem(182215831, 1)),
			check.conditions(), "the check must demand the whole group");
		Assertions.assertEquals(List.of(new QuestAction.RemoveItem(182215829, 1),
			new QuestAction.RemoveItem(182215830, 1), new QuestAction.RemoveItem(182215831, 1)),
			check.actions(), "the check must consume the whole group");
		QuestSnapshot fullHand = new QuestSnapshot(7, 15301, QuestStatus.START, layout.pack(Map.of("var0", 1)),
			Map.of(182215829, 1, 182215830, 1, 182215831, 1));
		state = apply(compiled, fullHand, new QuestEvent.TalkToNpc(805328,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		Assertions.assertEquals(Map.of("var0", 2), unpack(state, layout),
			"the group check must advance onto the handover row");
		QuestTransition handover = definition.transitions().stream()
			.filter(candidate -> "s2".equals(candidate.sourceNode()) && "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.SET_SUCCEED.id())
			.findFirst().orElseThrow(() -> new AssertionError("missing SET_SUCCEED handover s2->reward"));
		Assertions.assertEquals(List.of(new QuestAction.GiveItem(182215859, 1)), handover.actions(),
			"the handover must grant the work-item voucher");
		state = apply(compiled, state, new QuestEvent.TalkToNpc(805328, QuestDialogAction.SET_SUCCEED.id()));
		Assertions.assertEquals(QuestStatus.REWARD, state.status());
		Assertions.assertEquals(Map.of("var0", 3), unpack(state, layout), "the reward row is the QE-051 last row");
	}

	/**
	 * 15101（{talk, hunt} 混合链，客户端 SECTION 对齐形）：804715 信件推进只推行阶梯落击杀行
	 * （SECTION_0=1）、击杀边 = 显示名族闭包（含旧目标 235939）、逐杀推进 SECTION_1、
	 * 第 10 杀完成边进领奖且奖励投影保留满计数（QE-051）。
	 * 15101 (the {talk, hunt} mixed chain, client SECTION-aligned shape): the 804715 letter advance
	 * lands the kill row by ladder only (SECTION_0=1), the kill edges form the display-name family
	 * closure (legacy target 235939 included), kills advance SECTION_1, and the tenth kill's
	 * completing edge enters reward keeping the full count in the projection (QE-051).
	 */
	@Test
	void quest15101DialogUnlocksTheKillRowAndTheLastKillReachesTheReportRow() throws Exception {
		CompiledQuestDefinition compiled = load(15101);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 2, "var1", 10));
		// talk 步：804715 的推进边 started->s1 只推行阶梯（动作 id 从信件梯登记读）。
		// Talk step: the 804715 advance edge started->s1 bumps the row ladder only.
		QuestTransition talkAdvance = definition.transitions().stream()
			.filter(candidate -> "started".equals(candidate.sourceNode()) && "s1".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 804715 && !candidate.actions().isEmpty())
			.findFirst().orElseThrow(() -> new AssertionError("missing 804715 advance edge started->s1"));
		int advanceAction = ((QuestEvent.TalkToNpc) talkAdvance.event()).dialogId();
		QuestSnapshot state = new QuestSnapshot(7, 15101, QuestStatus.START, 0, Map.of());
		state = apply(compiled, state, new QuestEvent.TalkToNpc(804715, advanceAction));
		Assertions.assertEquals(Map.of("var0", 1, "var1", 0), unpack(state, layout),
			"the advance must land on the kill row");
		// hunt 步：击杀边 = 显示名族闭包（真端表模板 + 变体），旧击杀目标留在族内。
		// Hunt step: the kill edges form the display-name family closure; the legacy kill target
		// stays inside the family.
		Set<Integer> targets = new LinkedHashSet<>();
		for (QuestTransition candidate : definition.transitions()) {
			if ("s1".equals(candidate.sourceNode())
					&& candidate.event() instanceof QuestEvent.KillNpc kill) {
				targets.add(kill.npcId());
			}
		}
		Assertions.assertFalse(targets.isEmpty(), "the kill row must expose kill edges");
		Assertions.assertTrue(targets.contains(235939), "the legacy kill target stays in the retail family");
		for (int kill = 1; kill <= 9; kill++) {
			final int killIndex = kill;
			state = apply(compiled, state, new QuestEvent.KillNpc(235939));
			Assertions.assertEquals(Map.of("var0", 1, "var1", killIndex), unpack(state, layout),
				() -> "kill " + killIndex + " must advance the kill counter on the hunt row");
		}
		// 第 10 杀完成边进领奖（保留满计数 var1=10）。 / The tenth kill's completing edge enters reward keeping the full count.
		state = apply(compiled, state, new QuestEvent.KillNpc(235939));
		Assertions.assertEquals(QuestStatus.REWARD, state.status());
		Assertions.assertEquals(Map.of("var0", 2, "var1", 10), unpack(state, layout));
	}

	/**
	 * 18990（{talk, hunt} 混合链，块内并行目标，客户端 SECTION 对齐形）：僵尸/公主两目标按客户端
	 * 行组并行计数在 SECTION_1/SECTION_2（S0==1），完成边检查**其余行**已满（任一顺序、最后补齐的
	 * 一杀推进；未满时超额击杀无路线），段完成清零两段计数；恶灵段（S0==2）复用 SECTION_1，
	 * 末段满计数进领奖投影（QE-051）。
	 * 18990 (the {talk, hunt} mixed chain with parallel targets inside one block, client
	 * SECTION-aligned shape): zombie and princess count in parallel in SECTION_1/SECTION_2 per the
	 * client's S0==1 row group, completing edges check the OTHER rows' counts (either order — the
	 * last filling kill advances; overkills plan nothing while the block is incomplete), stage
	 * completion resets both counters; the evil-spirit stage (S0==2) reuses SECTION_1, and the
	 * final count rides the reward projection (QE-051).
	 */
	@Test
	void quest18990CountsParallelTargetsInTheirOwnSections() throws Exception {
		CompiledQuestDefinition compiled = load(18990);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		Assertions.assertEquals(0, layout.field("var0").offset());
		Assertions.assertEquals(6, layout.field("var1").offset());
		Assertions.assertEquals(12, layout.field("var2").offset());
		Assertions.assertNull(layout.field("var3"), "quest 18990 must not declare var3");
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3, "var1", 1));
		// 击杀族 = 旧 XML 三目标（220417 僵尸 / 220418 公主 / 220427 恶灵），客户端行名单闭包覆盖。
		// The kill families keep the legacy XML targets (220417 zombie / 220418 princess / 220427
		// evil spirit) via the client row-name closure.
		Set<Integer> s1Targets = new LinkedHashSet<>();
		Set<Integer> s2Targets = new LinkedHashSet<>();
		for (QuestTransition candidate : definition.transitions()) {
			if (!(candidate.event() instanceof QuestEvent.KillNpc kill)) {
				continue;
			}
			if ("s1".equals(candidate.sourceNode())) {
				s1Targets.add(kill.npcId());
			}
			if ("s2".equals(candidate.sourceNode())) {
				s2Targets.add(kill.npcId());
			}
		}
		Assertions.assertEquals(Set.of(220417, 220418), s1Targets, "the S0==1 group counts zombie and princess");
		Assertions.assertEquals(Set.of(220427), s2Targets, "the S0==2 stage counts the evil spirit");
		// 并行段行边形：僵尸行完成边只门其余行（var2），继续边加 var1；公主行对称。
		// Parallel-row edge pair: the zombie completing edge gates only the OTHER row (var2) while
		// its continuing edge increments var1; the princess row mirrors it.
		QuestTransition zombieContinue = definition.transitions().stream()
			.filter(candidate -> "s1".equals(candidate.sourceNode())
				&& candidate.event().equals(new QuestEvent.KillNpc(220417))
				&& Integer.valueOf(1).equals(candidate.priority()))
			.findFirst().orElseThrow();
		Assertions.assertEquals(List.of(new QuestCondition.VariableBelow("var1", 1)), zombieContinue.conditions());
		QuestTransition princessContinue = definition.transitions().stream()
			.filter(candidate -> "s1".equals(candidate.sourceNode())
				&& candidate.event().equals(new QuestEvent.KillNpc(220418))
				&& Integer.valueOf(1).equals(candidate.priority()))
			.findFirst().orElseThrow();
		Assertions.assertEquals(List.of(new QuestCondition.VariableBelow("var2", 1)), princessContinue.conditions());
		// 信件推进先落 S0==1 行组（客户端在 SECTION_0==1 时才计数僵尸/公主行）。
		// The letter advance first lands the S0==1 row group (the client counts the zombie and
		// princess rows only while SECTION_0==1).
		QuestTransition talkAdvance = definition.transitions().stream()
			.filter(candidate -> "started".equals(candidate.sourceNode()) && "s1".equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc && !candidate.actions().isEmpty())
			.findFirst().orElseThrow(() -> new AssertionError("missing letter advance edge started->s1"));
		// 正向走链 1（僵尸先）：僵尸 1/1、公主行仍 0/1；公主补齐推进并清零两段；恶灵进领奖。
		// Walk 1 (zombie first): zombie 1/1 with the princess row still 0/1; the princess kill
		// advances and resets both counters; the evil-spirit kill enters reward.
		QuestSnapshot state = new QuestSnapshot(7, 18990, QuestStatus.START, 0, Map.of());
		state = apply(compiled, state, talkAdvance.event());
		state = apply(compiled, state, new QuestEvent.KillNpc(220417));
		Assertions.assertEquals(Map.of("var0", 1, "var1", 1, "var2", 0), unpack(state, layout),
			"the zombie kill must fill SECTION_1 only");
		assertNoRoute(compiled, state, new QuestEvent.KillNpc(220417));
		state = apply(compiled, state, new QuestEvent.KillNpc(220418));
		Assertions.assertEquals(Map.of("var0", 2, "var1", 0, "var2", 0), unpack(state, layout),
			"the last filling kill must advance the row and reset both sections");
		// 正向走链 2（公主先）：顺序无关。 / Walk 2 (princess first): the order is irrelevant.
		QuestSnapshot reversed = new QuestSnapshot(7, 18990, QuestStatus.START, 0, Map.of());
		reversed = apply(compiled, reversed, talkAdvance.event());
		reversed = apply(compiled, reversed, new QuestEvent.KillNpc(220418));
		Assertions.assertEquals(Map.of("var0", 1, "var1", 0, "var2", 1), unpack(reversed, layout),
			"the princess kill must fill SECTION_2 only");
		reversed = apply(compiled, reversed, new QuestEvent.KillNpc(220417));
		Assertions.assertEquals(Map.of("var0", 2, "var1", 0, "var2", 0), unpack(reversed, layout),
			"either order must reach the evil-spirit row");
		// 恶灵段（复用 SECTION_1，count 1）满计数进领奖投影。 / The evil-spirit stage (reused
		// SECTION_1, count 1) rides its full count into the reward projection.
		state = apply(compiled, state, new QuestEvent.KillNpc(220427));
		Assertions.assertEquals(QuestStatus.REWARD, state.status());
		Assertions.assertEquals(Map.of("var0", 3, "var1", 1, "var2", 0), unpack(state, layout),
			"the reward projection must keep the final stage's kill count");
	}

	/**
	 * 10010/20010（EnterWorld 接取副本混合链，客户端 SECTION 对齐形）：进世界事件 + 世界 id 条件
	 * 发放（无接取 NPC）；简报梯落 S0==1、39 整组检查落 S0==2、SETPRO3 落 S0==3（hunt 源行，
	 * 三首领任一击杀进 S0==4）、FOBJ 报告阅读梯（select5 五页）SETPRO5 落 S0==5、
	 * 1009 报告进领奖行 6 并授予报告凭证（doc_quest_10010e）。
	 * 10010/20010 (EnterWorld-acquired instance mixed chains, client SECTION-aligned shape): the
	 * world-enter event with the world-id condition grants the quest (no acquire npc); the briefing
	 * ladder lands S0==1, the 39 group check S0==2, SETPRO3 S0==3 (the hunt's own row — any one of
	 * the three leaders advances to S0==4), the FOBJ report-reading ladder (the five-page select5)
	 * lands S0==5 via SETPRO5, and the 1009 report enters reward row 6 granting the report voucher.
	 */
	@Test
	void quest10010And20010GrantOnWorldEntryAndWalkTheInstanceReportChain() throws Exception {
		for (int questId : List.of(10010, 20010)) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			Assertions.assertEquals(0, layout.field("var0").offset());
			Assertions.assertEquals(6, layout.field("var1").offset());
			// 发放路由：EnterWorld + WorldIs(302340000) 直接进 START，无接取 NPC。
			// The grant route: EnterWorld + WorldIs(302340000) lands START with no acquire npc.
			QuestTransition grant = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.EnterWorld
					&& "unaccepted".equals(transition.sourceNode())
					&& "started".equals(transition.targetNode()))
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " misses the world grant"));
			Assertions.assertEquals(List.of(new QuestCondition.WorldIs(302340000, true),
				new QuestCondition.StartEligible()), grant.conditions());
			Assertions.assertFalse(definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
				.filter(transition -> "unaccepted".equals(transition.sourceNode()))
				.findFirst().isPresent(), "quest " + questId + " must not keep an acquire npc route");
			assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
			assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
			assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
			assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
			assertNode(definition, "s5", QuestStatus.START, Map.of("var0", 5));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 6));

			// 简报梯：started 的 SEPTO1 推进边（动作 id 从信件梯登记读）落 S0==1。
			// The briefing ladder: the started advance edge (action id from the letter registry)
			// lands S0==1.
			QuestTransition briefing = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode())
					&& "s1".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc
					&& !transition.actions().isEmpty())
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " misses the briefing advance"));
			QuestSnapshot state = apply(compiled, new QuestSnapshot(7, questId, QuestStatus.START, 0, Map.of()),
				briefing.event());
			Assertions.assertEquals(Map.of("var0", 1, "var1", 0), unpack(state, layout),
				() -> "quest " + questId + " briefing must land the first journal row");

			// 39 整组检查（四采集物）落 S0==2；缺货兜底自环。
			// The 39 group check (four collectibles) lands S0==2; missing goods fall back.
			QuestTransition collect = definition.transitions().stream()
				.filter(transition -> "s1".equals(transition.sourceNode()) && "s2".equals(transition.targetNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id())
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " misses the group check"));
			Map<Integer, Integer> required = new LinkedHashMap<>();
			definition.metadata().itemRequirements().forEach(item ->
				required.put(item.itemId(), item.count()));
			Assertions.assertEquals(required.keySet(),
				collect.conditions().stream()
					.filter(condition -> condition instanceof QuestCondition.HasItem)
					.map(condition -> ((QuestCondition.HasItem) condition).itemId())
					.collect(java.util.stream.Collectors.toSet()),
				() -> "quest " + questId + " check must demand the whole retail group");
			Assertions.assertTrue(collect.actions().stream()
					.anyMatch(action -> action instanceof QuestAction.SetVariable(var field, var value)
						&& field.equals("var0") && value == 2),
				() -> "quest " + questId + " check must advance the ladder");

			// 带整组采集物走 39 检查落 S0==2，SETPRO3 推进落 hunt 源行 S0==3。
			// With the full collectible group the 39 check lands S0==2 and the SETPRO3 advance
			// lands the hunt's own row S0==3.
			QuestSnapshot withGroup = new QuestSnapshot(7, questId, state.status(), state.packedVariables(),
				required);
			state = apply(compiled, withGroup, new QuestEvent.TalkToNpc(
				((QuestEvent.TalkToNpc) collect.event()).npcId(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
			Assertions.assertEquals(Map.of("var0", 2, "var1", 0), unpack(state, layout),
				() -> "quest " + questId + " the group check must land the collect row");
			QuestTransition huntOpen = definition.transitions().stream()
				.filter(transition -> "s2".equals(transition.sourceNode())
					&& "s3".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc
					&& !transition.actions().isEmpty())
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " misses the s2 advance"));
			state = apply(compiled, state, huntOpen.event());
			Assertions.assertEquals(Map.of("var0", 3, "var1", 0), unpack(state, layout),
				() -> "quest " + questId + " SETPRO3 must land the hunt row");

			// hunt 段在 S0==3：三首领任一击杀（count 1）进 S0==4（客户端 SECTION_0==3; SECTION_1<1）。
			// The hunt sits on S0==3: any one of the three leaders (count 1) advances to S0==4.
			Set<Integer> huntTargets = new LinkedHashSet<>();
			for (QuestTransition candidate : definition.transitions()) {
				if ("s3".equals(candidate.sourceNode())
						&& candidate.event() instanceof QuestEvent.KillNpc kill) {
					huntTargets.add(kill.npcId());
				}
			}
			Assertions.assertEquals(3, huntTargets.size(),
				() -> "quest " + questId + " must expose the three boss leaders");
			state = apply(compiled, state, new QuestEvent.KillNpc(huntTargets.iterator().next()));
			Assertions.assertEquals(Map.of("var0", 4, "var1", 0), unpack(state, layout),
				() -> "quest " + questId + " one leader kill must open the report row");

			// 1009 报告进领奖行 6 并授予报告凭证。 / The 1009 report enters reward row 6 with the
			// report voucher.
			QuestTransition report = definition.transitions().stream()
				.filter(transition -> "s5".equals(transition.sourceNode())
					&& "reward".equals(transition.targetNode()))
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " misses the report edge"));
			Assertions.assertTrue(report.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id());
			Assertions.assertEquals(List.of(new QuestAction.SetVariable("var0", 6),
				new QuestAction.GiveItem(
					definition.metadata().questWorkItems().getFirst().itemId(), 1)), report.actions(),
				() -> "quest " + questId + " report must grant the report voucher");
		}
	}

	/**
	 * 16823/26823（EnterArea 接取副本混合链，EnterArea 步交织形）：升级 + 区域任务结束事件自动
	 * 接取（无接取 NPC）；S0==0 起始 hunt 六守卫 5 杀（SECTION_1 计数、count-1 门）进 S0==1，
	 * 感官区 a 进场 S0==2（客户端无 CutScene 声明，遗留 939 过场裁剪）、Weatha01 对话 S0==3、
	 * 感官区 b 进场 S0==4，蛇女首领 1 杀直达领奖行 5 且投影携带末段满计数（QE-051）。
	 * 16823/26823 (EnterArea-acquired instance mixed chains, the EA-interleaved shape): level-up
	 * and zone-mission-end events auto-grant (no acquire npc); the opening hunt on S0==0 needs
	 * five guard kills (the SECTION_1 counter, count-1 gate) to reach S0==1, sensory area a lands
	 * S0==2 (no client CutScene declaration — the legacy 939 movie is clipped), the Weatha01 talk
	 * S0==3, sensory area b S0==4, and the single matriarch kill reaches reward row 5 with the
	 * final section's full count projected (QE-051).
	 */
	@Test
	void quest16823And26823AutoGrantOnAreaLifecycleAndWalkTheSensoryChain() throws Exception {
		for (int questId : List.of(16823, 26823)) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			Assertions.assertEquals(0, layout.field("var0").offset());
			Assertions.assertEquals(6, layout.field("var1").offset());
			// 自动接取：LevelUp + ZoneMissionEnd（无接取 NPC），StartEligible 门禁收尾。
			// Auto grant: level-up and zone-mission-end (no acquire npc), StartEligible gating last.
			Assertions.assertFalse(definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
				.filter(transition -> "unaccepted".equals(transition.sourceNode()))
				.findFirst().isPresent(), "quest " + questId + " must not keep an acquire npc route");
			for (QuestEvent grant : List.of(new QuestEvent.LevelUp(), new QuestEvent.ZoneMissionEnd())) {
				QuestTransition edge = definition.transitions().stream()
					.filter(transition -> QuestEvent.matches(transition.event(), grant)
						&& "unaccepted".equals(transition.sourceNode())
						&& "started".equals(transition.targetNode()))
					.findFirst()
					.orElseThrow(() -> new AssertionError("quest " + questId + " misses grant " + grant.type()));
				Assertions.assertEquals(QuestCondition.StartEligible.class, edge.conditions().getFirst().getClass(),
					() -> "quest " + questId + " grant must lead with StartEligible (prerequisites follow)");
			}
			// 过场 939 仅存遗留 XML（客户端 HTML 无 CutScene 声明）——真端形不附电影。
			// Movie 939 only ever existed in the legacy XML (no client CutScene declaration) — the
			// retail shape attaches no movie.
			Assertions.assertTrue(definition.transitions().stream()
				.flatMap(transition -> transition.afterCommit().stream())
				.noneMatch(action -> action instanceof AfterCommitAction.PlayMovie(int movieId, var type)
					&& movieId == 939),
				() -> "quest " + questId + " must not replay the clipped 939 movie");
			assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
			assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
			assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
			assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
			assertNode(definition, "s4", QuestStatus.START, Map.of("var0", 4));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 5, "var1", 1));

			// 起始 hunt：六守卫任一 5 杀（SECTION_1 count-1 门），第五杀落 S0==1 并清段。
			// The opening hunt: five kills among the six guards (the SECTION_1 count-1 gate); the
			// fifth kill lands S0==1 and clears the section.
			Set<Integer> guards = new LinkedHashSet<>();
			for (QuestTransition candidate : definition.transitions()) {
				if ("started".equals(candidate.sourceNode())
						&& candidate.event() instanceof QuestEvent.KillNpc kill) {
					guards.add(kill.npcId());
				}
			}
			Assertions.assertEquals(6, guards.size(), () -> "quest " + questId + " must expose six guards");
			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START, 0, Map.of());
			for (int done = 1; done <= 5; done++) {
				state = apply(compiled, state, new QuestEvent.KillNpc(guards.iterator().next()));
				int finished = done;
				Assertions.assertEquals(finished == 5 ? Map.of("var0", 1, "var1", 0)
						: Map.of("var0", 0, "var1", finished), unpack(state, layout),
					() -> "quest " + questId + " guard kill " + finished + " must track SECTION_1");
			}

			// 感官区 a 进场推进（无对话段、无过场）。 / Sensory area a advances on zone entry (no
			// dialog stage, no movie).
			QuestTransition areaA = definition.transitions().stream()
				.filter(transition -> "s1".equals(transition.sourceNode()) && "s2".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.EnterZone)
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " misses area a entry"));
			state = apply(compiled, state, areaA.event());
			Assertions.assertEquals(Map.of("var0", 2, "var1", 0), unpack(state, layout),
				() -> "quest " + questId + " area a must land the talk row");
			// Weatha01 对话推进。 / The Weatha01 talk advance.
			QuestTransition talk = definition.transitions().stream()
				.filter(transition -> "s2".equals(transition.sourceNode()) && "s3".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc
					&& !transition.actions().isEmpty())
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " misses the talk advance"));
			state = apply(compiled, state, talk.event());
			Assertions.assertEquals(Map.of("var0", 3, "var1", 0), unpack(state, layout),
				() -> "quest " + questId + " the talk must land area b's row");
			// 感官区 b 进场推进。 / Sensory area b advances on zone entry.
			QuestTransition areaB = definition.transitions().stream()
				.filter(transition -> "s3".equals(transition.sourceNode()) && "s4".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.EnterZone)
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " misses area b entry"));
			state = apply(compiled, state, areaB.event());
			Assertions.assertEquals(Map.of("var0", 4, "var1", 0), unpack(state, layout),
				() -> "quest " + questId + " area b must land the boss row");
			// 蛇女首领 1 杀直达领奖行，投影携带末段满计数；单一日志名可展开为多个同名刷怪点模板，
			// 击杀任一实例都计满该段。
			// One matriarch kill reaches the reward row with the final section's full count
			// projected; a single journal name fans out to every same-name spawn template and any
			// instance kill fills the section.
			Set<Integer> bosses = new LinkedHashSet<>();
			for (QuestTransition candidate : definition.transitions()) {
				if ("s4".equals(candidate.sourceNode())
						&& candidate.event() instanceof QuestEvent.KillNpc kill) {
					bosses.add(kill.npcId());
				}
			}
			Assertions.assertTrue(bosses.size() >= 1, () -> "quest " + questId + " must expose the boss");
			state = apply(compiled, state, new QuestEvent.KillNpc(bosses.iterator().next()));
			Assertions.assertEquals(Map.of("var0", 5, "var1", 1), unpack(state, layout),
				() -> "quest " + questId + " the boss kill must reach reward with the full count");
			Assertions.assertEquals(QuestStatus.REWARD, state.status());
		}
	}

	/**
	 * 13956/23956（进世界接取 hunt/EA 链）与 16831/26831（EnterArea 接取纯 EA 链）：前者
	 * 进世界 + WorldIs 发放后 EA→三首领任一击杀→EA×3 直落领奖行 5；后者升级/区域任务结束
	 * 自动接取后三段区域推进直达领奖行 3——两条链全程零对话段，区名经登记表解析。
	 * 13956/23956 (the world-acquired hunt/EA chain) and 16831/26831 (the EnterArea-acquired
	 * pure-EA chain): the former grants on world entry + WorldIs then walks EA, any-one-of-three
	 * boss kills and three more EAs straight to reward row 5; the latter auto-grants on level-up /
	 * zone-mission-end then advances through three zone entries to reward row 3 — both chains run
	 * without any dialog stage, their zone names resolved through the registry.
	 */
	@Test
	void quest13956And16831WalkWorldAndPureAreaChainsWithoutDialogStages() throws Exception {
		for (int questId : List.of(13956, 23956)) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			Assertions.assertEquals(0, layout.field("var0").offset());
			Assertions.assertEquals(6, layout.field("var1").offset());
			QuestTransition grant = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.EnterWorld
					&& "unaccepted".equals(transition.sourceNode())
					&& "started".equals(transition.targetNode()))
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " misses the world grant"));
			Assertions.assertEquals(List.of(new QuestCondition.WorldIs(302340000, true),
				new QuestCondition.StartEligible()), grant.conditions());
			// EA→boss→EA 链：s1 首领行任一击杀（count 1）进 s2，随后 EA×3 直落领奖行 5。
			// The EA, boss, EA chain: any leader kill on s1 (count 1) opens s2 and three EAs land
			// reward row 5.
			assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
			assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
			assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 5, "var1", 1));
			Set<Integer> bosses = new LinkedHashSet<>();
			for (QuestTransition candidate : definition.transitions()) {
				if ("s1".equals(candidate.sourceNode())
						&& candidate.event() instanceof QuestEvent.KillNpc kill) {
					bosses.add(kill.npcId());
				}
			}
			Assertions.assertEquals(3, bosses.size(), () -> "quest " + questId + " must expose three bosses");
			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START, 0, Map.of());
			// 先进感官区 a（started→s1），首领行才可达。 / Enter sensory area a first (started to s1);
			// the boss row is only reachable from there.
			QuestTransition areaIn = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()) && "s1".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.EnterZone)
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " misses area a entry"));
			state = apply(compiled, state, areaIn.event());
			state = apply(compiled, state, new QuestEvent.KillNpc(bosses.iterator().next()));
			Assertions.assertEquals(Map.of("var0", 2, "var1", 0), unpack(state, layout),
				() -> "quest " + questId + " one boss kill must open the second area row");
			List<QuestTransition> areas = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.EnterZone)
				.filter(transition -> "s2".equals(transition.sourceNode())
					|| "s3".equals(transition.sourceNode()) || "s4".equals(transition.sourceNode()))
				.sorted(java.util.Comparator.comparingInt(transition ->
					Integer.parseInt(transition.sourceNode().substring(1))))
				.toList();
			Assertions.assertEquals(3, areas.size(), () -> "quest " + questId + " must expose three EAs");
			int row = 2;
			for (QuestTransition area : areas) {
				state = apply(compiled, state, area.event());
				final int landing = ++row;
				Assertions.assertEquals(landing, unpack(state, layout).get("var0"),
					() -> "quest " + questId + " zone entry must advance to row " + landing);
			}
			Assertions.assertEquals(QuestStatus.REWARD, state.status());
		}
		for (int questId : List.of(16831, 26831)) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			Assertions.assertFalse(definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
				.filter(transition -> "unaccepted".equals(transition.sourceNode()))
				.findFirst().isPresent(), "quest " + questId + " must not keep an acquire npc route");
			// 自动接取后连续三段区域推进直达领奖行 3。 / After the auto grant three consecutive zone
			// entries reach reward row 3.
			assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
			assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
			assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3));
			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START, 0, Map.of());
			for (int landing = 1; landing <= 3; landing++) {
				final int target = landing;
				QuestTransition area = definition.transitions().stream()
					.filter(transition -> transition.event() instanceof QuestEvent.EnterZone
						&& (("started".equals(transition.sourceNode()) && target == 1)
							|| ("s" + (target - 1)).equals(transition.sourceNode())))
					.findFirst()
					.orElseThrow(() -> new AssertionError("quest " + questId + " misses area " + target));
				state = apply(compiled, state, area.event());
				Assertions.assertEquals(target, unpack(state, definition.progressLayout()).get("var0"),
					() -> "quest " + questId + " area " + target + " must land row " + target);
			}
			Assertions.assertEquals(QuestStatus.REWARD, state.status());
		}
	}

	/**
	 * 24153：客户端 SECTION_0..4 是 5 只冰冻独眼巨人的独立计数，全部由 SECTION_5==0 门控，
	 * 与旧 handler 的 setQuestVarById(0..4) / setQuestVarById(5,1->0) 同值。
	 * 24153: client SECTION_0..4 are the five independent cyclops counters, all gated by
	 * SECTION_5==0 — the same values the legacy handler wrote.
	 */
	@Test
	void quest24153DeclaresFiveClientCountersBehindTheSectionFiveGate() throws Exception {
		CompiledQuestDefinition compiled = load(24153);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		for (int index = 0; index <= 5; index++) {
			final int fieldIndex = index;
			BitField field = layout.field("var" + fieldIndex);
			Assertions.assertNotNull(field, () -> "quest 24153 must declare var" + fieldIndex);
			Assertions.assertEquals(6 * fieldIndex, field.offset(),
				() -> "var" + fieldIndex + " must map its client SECTION");
			if (index < 5) {
				/* 五段计数占满各自的客户端 SECTION（6 位）：真端网格直接沿用 SECTION 位宽。 */
				/* The five counters own their full 6-bit client SECTION, as the retail grid does. */
				Assertions.assertEquals(63, field.maxValue(),
					() -> "var" + fieldIndex + " must span its client SECTION");
			} else {
				Assertions.assertEquals(1, field.maxValue(), () -> "var5 is the 0/1 briefing flag");
			}
		}
		Map<String, Integer> zero = Map.of("var0", 0, "var1", 0, "var2", 0, "var3", 0, "var4", 0, "var5", 0);
		Map<String, Integer> full = Map.of("var0", 1, "var1", 1, "var2", 1, "var3", 1, "var4", 1, "var5", 0);
		Map<String, Integer> raised = Map.of("var0", 0, "var1", 0, "var2", 0, "var3", 0, "var4", 0, "var5", 1);
		Assertions.assertEquals("started", stateNode(definition, QuestStatus.START, raised));
		/* 五段独立计数 → 真端网格是 2^5 的完整乘积，零段节点的网格名即 a0b0c0d0e0。 */
		/* Five independent counters -> the retail grid is the full 2^5 product; its zero node is a0b0c0d0e0. */
		String hunted = stateNode(definition, QuestStatus.START, zero);
		Assertions.assertEquals("a0b0c0d0e0", hunted);
		Assertions.assertEquals("reward", stateNode(definition, QuestStatus.REWARD, full));
		Map<Integer, String> counters = new LinkedHashMap<>();
		counters.put(213730, "var0");
		counters.put(213788, "var1");
		counters.put(213789, "var2");
		counters.put(213790, "var3");
		counters.put(213791, "var4");
		for (Map.Entry<Integer, String> entry : counters.entrySet()) {
			QuestTransition route = definition.transitions().stream()
				.filter(transition -> hunted.equals(transition.sourceNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.KillNpc kill
					&& kill.npcId() == entry.getKey())
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest 24153 misses the kill route " + entry.getKey()));
			Map<String, Integer> advanced = new LinkedHashMap<>(zero);
			advanced.put(entry.getValue(), 1);
			Assertions.assertEquals(Map.copyOf(advanced), node(definition, route.targetNode()).projection().variables(),
				() -> "kill route " + entry.getKey() + " must advance only " + entry.getValue());
			Assertions.assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				route.afterCommit(),
				() -> "kill route " + entry.getKey() + " must stay PACKET_ONLY");
		}
		QuestSnapshot state = new QuestSnapshot(7, 24153, QuestStatus.START, layout.pack(Map.of("var5", 1)), Map.of());
		/* P0-2 规范形：简报对话一步清门（QUEST_SELECT 直达网格零段），SETPRO2 按钮路由随 select2 页链删除。
		   Canonical since P0-2: the briefing talk clears the gate in one step (QUEST_SELECT lands on the
		   zero grid node); the SETPRO2 button route is gone with the select2 page chain. */
		state = apply(compiled, state, new QuestEvent.TalkToNpc(204784, QuestDialogAction.QUEST_SELECT.id()));
		Assertions.assertEquals(0, unpack(state, layout).get("var5"),
			"the Delris dialog must clear the SECTION_5 gate so the client counts the five cyclopes");
		for (int npcId : List.of(213730, 213788, 213789, 213790, 213791)) {
			state = apply(compiled, state, new QuestEvent.KillNpc(npcId));
		}
		Map<String, Integer> variables = unpack(state, layout);
		Assertions.assertEquals(List.of(1, 1, 1, 1, 1),
			List.of(variables.get("var0"), variables.get("var1"), variables.get("var2"),
				variables.get("var3"), variables.get("var4")));
		Assertions.assertEquals(QuestStatus.START, state.status());
		Assertions.assertFalse(definition.transitions().stream()
			.anyMatch(transition -> transition.sourceNode() == null
				&& transition.event() instanceof QuestEvent.TalkToNpc),
			"quest 24153 must not keep a source-less talk repair edge");
		/* P0-2 规范形：满段交付 = QUEST_SELECT（1009 中转删除）。
		   Canonical since P0-2: the full-node delivery is QUEST_SELECT (the 1009 hop is gone). */
		state = apply(compiled, state, new QuestEvent.TalkToNpc(204787, QuestDialogAction.QUEST_SELECT.id()));
		Assertions.assertEquals(QuestStatus.REWARD, state.status());
		Assertions.assertEquals(Map.of("var0", 1, "var1", 1, "var2", 1, "var3", 1, "var4", 1, "var5", 0),
			unpack(state, layout));
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static String stateNode(QuestDefinition definition, QuestStatus status, Map<String, Integer> state) {
		List<String> labels = definition.nodes().stream()
			.filter(node -> node.projection().status() == status)
			.filter(node -> node.projection().variables().equals(state))
			.map(QuestNode::label)
			.toList();
		Assertions.assertEquals(1, labels.size(), () -> "quest " + definition.id() + " must declare exactly one "
			+ status + " node projecting " + state + ", got " + labels);
		return labels.getFirst();
	}

	/**
	 * 25304（三族混合链，DD_TALK_COLLECT_HUNT_CHAIN，客户端 SECTION 对齐形）：0 找帕赫曼 ->
	 * 1 交出雕花 -> 2 守护哥尔哈 60 点 -> 3 回报帕赫曼 -> 4 向斯库顿报告。var0 = 行阶梯（SECTION_0）、
	 * var1 = 当前段击杀计数（SECTION_1，偏移 6、段完成清零）。
	 * 25304 (three-kind mixed chain, DD_TALK_COLLECT_HUNT_CHAIN, client SECTION-aligned shape):
	 * every journal row from the Fachmann dialog up to the Skuldun report is reachable; var0 is
	 * the row ladder (SECTION_0) and var1 the current stage's kill counter (SECTION_1, offset 6,
	 * reset on stage completion).
	 */
	@Test
	void quest25304RebuildsEveryJournalRowUpToTheSkuldunReport() throws Exception {
		CompiledQuestDefinition compiled = load(25304);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		Assertions.assertEquals(0, layout.field("var0").offset());
		Assertions.assertEquals(6, layout.field("var1").offset());
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 4));
		QuestTransition patternCheck = definition.transitions().stream()
			.filter(transition -> "s1".equals(transition.sourceNode()) && "s2".equals(transition.targetNode()))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(805340,
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id())))
			.findFirst().orElseThrow();
		Assertions.assertEquals(List.of(new QuestCondition.HasItem(182215850, 1)), patternCheck.conditions());
		Assertions.assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), patternCheck.afterCommit());
		QuestSnapshot state = new QuestSnapshot(7, 25304, QuestStatus.START, 0, Map.of());
		state = apply(compiled, state, new QuestEvent.TalkToNpc(805340, QuestDialogAction.SETPRO1.id()));
		Assertions.assertEquals(1, unpack(state, layout).get("var0"), "SETPRO1 must unlock the crafted-pattern row");
		QuestSnapshot withoutPattern = apply(compiled, state, new QuestEvent.TalkToNpc(805340,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		Assertions.assertEquals(1, unpack(withoutPattern, layout).get("var0"),
			"a missing pattern must keep the journal on row 1");
		QuestSnapshot withPattern = new QuestSnapshot(7, 25304, QuestStatus.START, state.packedVariables(),
			Map.of(182215850, 1));
		state = apply(compiled, withPattern, new QuestEvent.TalkToNpc(805340,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		Assertions.assertEquals(2, unpack(state, layout).get("var0"), "handing the pattern over must open the Gorha row");
		for (int kill = 1; kill <= 59; kill++) {
			final int killIndex = kill;
			state = apply(compiled, state, new QuestEvent.KillNpc(233902));
			Assertions.assertEquals(Map.of("var0", 2, "var1", killIndex), unpack(state, layout),
				() -> "kill " + killIndex + " must advance SECTION_1 on the Gorha row");
		}
		// 第 60 杀完成边清零 SECTION_1 并落到回报行（段计数复位，阶梯保留）。
		// The 60th kill's completing edge clears SECTION_1 and lands the report row (stage counter
		// reset, ladder kept).
		state = apply(compiled, state, new QuestEvent.KillNpc(233902));
		Assertions.assertEquals(Map.of("var0", 3, "var1", 0), unpack(state, layout),
			"60 Gorha kills must open the Fachmann-report row with the counter reset");
		QuestSnapshot withPrototype = new QuestSnapshot(7, 25304, QuestStatus.START, state.packedVariables(),
			Map.of(182215850, 1));
		state = apply(compiled, withPrototype, new QuestEvent.TalkToNpc(805340, QuestDialogAction.SET_SUCCEED.id()));
		Assertions.assertEquals(QuestStatus.REWARD, state.status());
		Assertions.assertEquals(Map.of("var0", 4, "var1", 0), unpack(state, layout));
	}

	/**
	 * 25604（DataDriven 形）：s2 的 SECTION_1 计数事件（703125 x3，count-1 门，非末段完成清零）、
	 * Groma 对话推进（登记按钮 10003）与末段 39 整组检查直达领奖行 5（reward 无满计数—— hunt
	 * 非末段，客户端登记表末行无 SECTION_1 门）。
	 * 25604 (the DataDriven shape): the s2 SECTION_1 counting event (703125 x3, count-1 gate,
	 * cleared on the non-final stage's completion), the Groma talk advance (registered button
	 * 10003) and the final 39 group check landing reward row 5 (no full-count projection — the
	 * hunt is not the final step and the client registry's last row carries no SECTION_1 gate).
	 */
	@Test
	void quest25604KeepsTheKillCounterAndProjectsTheReportRow() throws Exception {
		CompiledQuestDefinition compiled = load(25604);
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		Assertions.assertEquals(6, layout.field("var1").offset());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 5));
		boolean countingEvent = definition.transitions().stream()
			.filter(transition -> "s2".equals(transition.sourceNode()) && "s3".equals(transition.targetNode()))
			.anyMatch(transition -> transition.event() instanceof QuestEvent.KillNpc kill
				&& kill.npcId() == 703125);
		Assertions.assertTrue(countingEvent, "quest 25604 must keep the SECTION_1 counting event on the s2 kill row");
		QuestSnapshot state = new QuestSnapshot(7, 25604, QuestStatus.START, layout.pack(Map.of("var0", 2, "var1", 0)),
			Map.of());
		for (int kill = 1; kill <= 3; kill++) {
			final int killIndex = kill;
			state = apply(compiled, state, new QuestEvent.KillNpc(703125));
			Map<String, Integer> variables = unpack(state, layout);
			Assertions.assertEquals(killIndex == 3 ? 0 : killIndex, variables.get("var1"),
				() -> "kill " + killIndex + " must advance SECTION_1 until the stage completes");
			Assertions.assertEquals(killIndex == 3 ? 3 : 2, variables.get("var0"),
				() -> "kill " + killIndex + " must keep the client journal on the expected row");
		}
		state = apply(compiled, state, new QuestEvent.TalkToNpc(806173, 10003));
		Assertions.assertEquals(4, unpack(state, layout).get("var0"), "the Groma talk must open the clue row");
		QuestSnapshot withClue = new QuestSnapshot(7, 25604, QuestStatus.START, state.packedVariables(),
			Map.of(182216003, 5));
		state = apply(compiled, withClue, new QuestEvent.TalkToNpc(806173,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		Assertions.assertEquals(QuestStatus.REWARD, state.status());
		Assertions.assertEquals(Map.of("var0", 5, "var1", 0), unpack(state, layout));
	}

	/**
	 * 14252/24252（三段顺序链网格 a0b0c0..a1b1c1）：前段未满后段不计数（负例探针），
	 * 满链态报告无门禁、满链后多余击杀无路线。
	 * 14252/24252 (the three-stage sequential-chain grid a0b0c0..a1b1c1): later stages never count
	 * before earlier ones fill (negative probes), the full-chain report is ungated, and extra kills
	 * after the full chain plan nothing.
	 */
	@Test
	void quest14252And24252WalkTheSequentialStageChainBeforeTheReport() throws Exception {
		for (Case testCase : List.of(new Case(14252, 832824, 236924, 237263, 237275),
				new Case(24252, 832820, 236924, 237263, 237275))) {
			CompiledQuestDefinition compiled = load(testCase.questId());
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			Assertions.assertEquals(0, layout.field("var0").offset());
			Assertions.assertEquals(6, layout.field("var1").offset());
			Assertions.assertEquals(12, layout.field("var2").offset());
			assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0, "var2", 0));
			assertNode(definition, "a0b0c0", QuestStatus.START, Map.of("var0", 0, "var1", 0, "var2", 0));
			assertNode(definition, "a1b0c0", QuestStatus.START, Map.of("var0", 1, "var1", 0, "var2", 0));
			assertNode(definition, "a1b1c0", QuestStatus.START, Map.of("var0", 1, "var1", 1, "var2", 0));
			assertNode(definition, "a1b1c1", QuestStatus.START, Map.of("var0", 1, "var1", 1, "var2", 1));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 1, "var2", 1));
			assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0, "var2", 0));
			// P0-2 顺序链规范形：满链 QUEST_SELECT 无门禁直翻领奖（1009 中转删除）。
			// Canonical sequential shape: the full chain's QUEST_SELECT flips reward ungated
			// (no 1009 hop).
			QuestTransition report = definition.transitions().stream()
				.filter(transition -> "a1b1c1".equals(transition.sourceNode())
					&& "reward".equals(transition.targetNode()))
				.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(testCase.reportNpc(),
					QuestDialogAction.QUEST_SELECT.id())))
				.findFirst().orElseThrow();
			Assertions.assertEquals(List.of(), report.conditions(),
				() -> "quest " + testCase.questId() + " full-chain delivery must stay ungated");
			Assertions.assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
					definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
				report.afterCommit());
			QuestSnapshot zero = new QuestSnapshot(7, testCase.questId(), QuestStatus.START, 0, Map.of());
			assertNoRoute(compiled, zero, new QuestEvent.KillNpc(testCase.rowTwoMob()));
			assertNoRoute(compiled, zero, new QuestEvent.KillNpc(testCase.rowThreeMob()));
			QuestSnapshot state = apply(compiled, zero, new QuestEvent.KillNpc(testCase.rowOneMob()));
			Assertions.assertEquals(Map.of("var0", 1, "var1", 0, "var2", 0), unpack(state, layout),
				() -> "quest " + testCase.questId() + " stage 1 must fill first");
			state = apply(compiled, state, new QuestEvent.KillNpc(testCase.rowTwoMob()));
			Assertions.assertEquals(Map.of("var0", 1, "var1", 1, "var2", 0), unpack(state, layout),
				() -> "quest " + testCase.questId() + " stage 2 only counts after stage 1");
			state = apply(compiled, state, new QuestEvent.KillNpc(testCase.rowThreeMob()));
			Assertions.assertEquals(QuestStatus.START, state.status(),
				() -> "quest " + testCase.questId() + " full chain must stay START before the report");
			Assertions.assertEquals(Map.of("var0", 1, "var1", 1, "var2", 1), unpack(state, layout));
			assertNoRoute(compiled, state, new QuestEvent.KillNpc(testCase.rowOneMob()));
			state = apply(compiled, state, new QuestEvent.TalkToNpc(testCase.reportNpc(),
				QuestDialogAction.QUEST_SELECT.id()));
			Assertions.assertEquals(QuestStatus.REWARD, state.status());
		}
	}

	/**
	 * 10101/20101：击杀推进只推行阶梯（packed 恰为段值，无高位污染负例）。
	 * 10101/20101: kills advance the row ladder only (packed exactly the row value — the
	 * high-bit-corruption negative probe).
	 */
	@Test
	void quest10101And20101WalkPhaseStepsWithoutCorruptingPackedStep() throws Exception {
		for (int questId : List.of(10101, 20101)) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			Assertions.assertNotNull(layout.field("var0"), () -> "quest " + questId + " must declare var0");
			Assertions.assertNull(layout.field("var2"), () -> "quest " + questId + " must NOT declare var2");
			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START, layout.pack(Map.of("var0", 2)),
				Map.of());
			state = apply(compiled, state, new QuestEvent.KillNpc(234680));
			Assertions.assertEquals(Map.of("var0", 3), unpack(state, layout));
			Assertions.assertEquals(3, state.packedVariables(),
				() -> "quest " + questId + " step must be exactly 3, not corrupted by high bits");
			state = apply(compiled, state, new QuestEvent.KillNpc(234680));
			Assertions.assertEquals(Map.of("var0", 4), unpack(state, layout));
			Assertions.assertEquals(4, state.packedVariables(),
				() -> "quest " + questId + " step must be exactly 4, not corrupted by high bits");
		}
	}

	/** 10101/20101：SET_SUCCEED 进领奖且不幻发物品（无 GiveItem 路线）。 */
	/** 10101/20101: SET_SUCCEED enters reward without phantom item grants. */
	@Test
	void quest10101And20101AdvanceToRewardWithoutPhantomItem() throws Exception {
		Map<Integer, Integer> npcs = Map.of(10101, 802357, 20101, 802361);
		for (Map.Entry<Integer, Integer> entry : npcs.entrySet()) {
			int questId = entry.getKey();
			int npcId = entry.getValue();
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START, layout.pack(Map.of("var0", 8)),
				Map.of());
			state = apply(compiled, state, new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SET_SUCCEED.id()));
			Assertions.assertEquals(QuestStatus.REWARD, state.status(),
				() -> "quest " + questId + " should advance to REWARD status upon SET_SUCCEED with npc " + npcId);
			Assertions.assertEquals(Map.of("var0", 8), unpack(state, layout),
				() -> "quest " + questId + " should retain var0 at 8 upon SET_SUCCEED with npc " + npcId);
		}
	}

	/** 10101/20101：入侵计划 39 检查只锁计划物品，推进到 var0=8。 */
	/** 10101/20101: the invasion-plan 39 check requires only the plan item and advances var0 to 8. */
	@Test
	void quest10101And20101SubmitInvasionPlanOnlyRequiresPlanItem() throws Exception {
		Map<Integer, Integer> npcs = Map.of(10101, 802357, 20101, 802361);
		Map<Integer, Integer> items = Map.of(10101, 182215452, 20101, 182215453);
		for (Map.Entry<Integer, Integer> entry : npcs.entrySet()) {
			int questId = entry.getKey();
			int npcId = entry.getValue();
			int planItemId = items.get(questId);
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START, layout.pack(Map.of("var0", 7)),
				Map.of(planItemId, 1));
			state = apply(compiled, state, new QuestEvent.TalkToNpc(npcId,
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
			Assertions.assertEquals(QuestStatus.START, state.status());
			Assertions.assertEquals(Map.of("var0", 8), unpack(state, layout),
				() -> "quest " + questId + " should advance var0 to 8 upon presenting invasion plan");
		}
	}

	/** 10526/20526：SET_SUCCEED 进领奖并把行阶梯推到 12（无幻发物品）。 */
	/** 10526/20526: SET_SUCCEED enters reward and bumps the row ladder to 12 (no phantom grants). */
	@Test
	void quest10526And20526AdvanceToRewardWithoutPhantomItem() throws Exception {
		Map<Integer, Integer> npcs = Map.of(10526, 806292, 20526, 806297);
		Map<Integer, Integer> items = Map.of(10526, 182216074, 20526, 182216086);
		for (Map.Entry<Integer, Integer> entry : npcs.entrySet()) {
			int questId = entry.getKey();
			int npcId = entry.getValue();
			int itemId = items.get(questId);
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			QuestSnapshot state = new QuestSnapshot(7, questId, QuestStatus.START, layout.pack(Map.of("var0", 11)),
				Map.of(itemId, 1));
			state = apply(compiled, state, new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SET_SUCCEED.id()));
			Assertions.assertEquals(QuestStatus.REWARD, state.status(),
				() -> "quest " + questId + " should advance to REWARD status upon SET_SUCCEED with npc " + npcId);
			Assertions.assertEquals(Map.of("var0", 12), unpack(state, layout),
				() -> "quest " + questId + " should set var0 to 12 upon SET_SUCCEED with npc " + npcId);
		}
	}

	/** 中段行推进必须携带可见性刷新（客户端任务书行变化的同步合同）。 */
	/** Mid-route journal-row advances must carry a visibility refresh (the sync contract for
	 * client journal row changes). */
	@Test
	void midRouteJournalRowAdvancesCarryAVisibilityRefresh() throws Exception {
		for (RowAdvance advance : List.of(new RowAdvance(10101, "s3", "s4"), new RowAdvance(10101, "s4", "s5"),
				new RowAdvance(10101, "s5", "s6"), new RowAdvance(10101, "s6", "s7"), new RowAdvance(10101, "s7", "s8"),
				new RowAdvance(20101, "s3", "s4"), new RowAdvance(20101, "s4", "s5"), new RowAdvance(20101, "s5", "s6"),
				new RowAdvance(20101, "s6", "s7"), new RowAdvance(20101, "s7", "s8"),
				new RowAdvance(14021, "s6", "s7"), new RowAdvance(24014, "s4", "s5"))) {
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
			Assertions.assertTrue(refreshed, () -> "quest " + advance.questId() + " "
				+ advance.source() + "->" + advance.target() + " advances the journal row and must refresh client visibility");
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
			Optional<QuestMutationPlan> plan = QuestMutationPlanner.plan(definition, snapshot, event, transition);
			if (plan.isPresent()) {
				QuestMutationPlan mutation = plan.orElseThrow();
				return new QuestSnapshot(7, definition.id(), mutation.nextStatus(),
					mutation.nextPackedVariables(), Map.of());
			}
		}
		throw new AssertionError("no route for quest " + definition.id() + " event " + event);
	}

	private static void assertNoRoute(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestEvent event) {
		Assertions.assertTrue(definition.transitionsFor(event.type()).stream()
			.noneMatch(transition -> QuestMutationPlanner.plan(definition, snapshot, event, transition).isPresent()),
			() -> "quest " + definition.id() + " must not route " + event + " at "
				+ definition.definition().progressLayout().unpack(snapshot.packedVariables()));
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
		Assertions.assertEquals(status, node.projection().status(), () -> "status mismatch on node " + label);
		Assertions.assertEquals(variables, node.projection().variables(),
			() -> "variables mismatch on node " + label);
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		// Retired quests live in git history only: use the production view (XML dir + retail overlay).
		return ProductionQuestDefinitions.definition(questId);
	}

	/** 14252/24252 双阵营字段（报告 NPC 与三段击杀目标）。 */
	/** Faction fields for 14252/24252 (report npc plus the three stage kill targets). */
	private record Case(int questId, int reportNpc, int rowOneMob, int rowTwoMob, int rowThreeMob) {
	}

	/** 中段行推进用例（任务 + 源/目标节点）。 */
	/** One mid-route row-advance case (quest plus source/target nodes). */
	private record RowAdvance(int questId, String source, String target) {
	}
}
