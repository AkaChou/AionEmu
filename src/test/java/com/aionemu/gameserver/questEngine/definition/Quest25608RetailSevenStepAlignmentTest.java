package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 25608 [Group] Oh, Bother 的 7 步真端行对齐。
 * <p>客户端 quest_q25608.html 的 quest_summary 有 7 个可见槽位（0/3/6/9/12/15/18），retail
 * 定义同样是 7 步：TALK 806177 → TALK 806197 → ENTER_AREA 206534 → HUNT 241235 x10 →
 * TALK 806197 → ENTER_AREA 206542 → COLLECT_ITEM 805964。旧迁移跳过了两个 ENTER_AREA 行，
 * 把 HUNT 放在 var0=2、把交付行拆成 var0=4/5，导致行 2 与行 5 永远没有状态、领奖态停在行 5。
 * 修复后 step0..step6 与行 0..6 一一对应，reward 投影为领奖行 6，旧 REWARD/var0=5 存档通过
 * 无 source 的 enter-world 或 QUEST_SELECT 自愈边回到行 6；两个 ENTER_AREA 触发器注册在
 * zones_quest.xml，坐标取自客户端 DF6 mission level 的 sensory NPC。</p>
 * <p>Locks the seven-step retail journal alignment for quest 25608: the client quest_summary has
 * seven visible slots matching the retail steps TALK, TALK, ENTER_AREA 206534, HUNT x10, TALK,
 * ENTER_AREA 206542 and COLLECT_ITEM. The old migration skipped both ENTER_AREA rows and left the
 * reward state on row 5. The repaired definition projects step0..step6 onto journal rows 0..6,
 * projects REWARD onto the reward row 6, heals stale REWARD/var0=5 saves, and registers both
 * sensory zones that were extracted from the client DF6 mission level.</p>
 */
class Quest25608RetailSevenStepAlignmentTest {

	private static final String ZONE_A = "DF6_SENSORY_AREA_Q25608_A_DYNAMIC_ENV_220110000";
	private static final String ZONE_B = "DF6_SENSORY_AREA_Q25608_B_DYNAMIC_ENV_220110000";
	private static final int TURN_IN_NPC = 805964;
	private static final int HUNT_NPC = 241235;
	private static final int QUEST_ITEM = 182216007;
	private static final int REWARD_ROW = 6;
	private static final int STALE_REWARD_ROW = 5;

	@Test
	void sevenRetailStepsCoverEveryClientJournalRow() {
		QuestDefinition definition = load().definition();

		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "step1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "step2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "step3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "step4", QuestStatus.START, Map.of("var0", 4));
		assertNode(definition, "step5", QuestStatus.START, Map.of("var0", 5));
		assertNode(definition, "step6", QuestStatus.START, Map.of("var0", 6));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", REWARD_ROW));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		Set<Integer> journalRows = definition.nodes().stream()
			.map(QuestNode::projection)
			.filter(projection -> projection.status() == QuestStatus.START
				|| projection.status() == QuestStatus.REWARD)
			.map(projection -> projection.variables().get("var0"))
			.collect(Collectors.toSet());
		assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6), journalRows,
			"客户端每一行都必须有 START/REWARD 状态");
	}

	@Test
	void enterAreaRowsAdvanceThroughRegisteredClientZones() throws Exception {
		CompiledQuestDefinition compiled = load();

		QuestTransition firstArea = transition(compiled, "step2", "step3");
		assertEquals(new QuestEvent.EnterZone(ZONE_A), firstArea.event());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), firstArea.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 3)), firstArea.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			firstArea.afterCommit());

		QuestTransition secondArea = transition(compiled, "step5", "step6");
		assertEquals(new QuestEvent.EnterZone(ZONE_B), secondArea.event());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 5)), secondArea.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 6)), secondArea.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			secondArea.afterCommit());

		String zones = Files.readString(
			Path.of("src/main/resources/aion/data/static_data/zones/zones_quest.xml"));
		assertTrue(zones.contains("name=\"" + ZONE_A + "\" area_type=\"SPHERE\" zone_type=\"SUB\""),
			"zones_quest.xml 必须注册任务 25608 的第一个 sensory zone");
		assertTrue(zones.contains("<sphere x=\"1540.3777\" y=\"556.31085\" z=\"310\" r=\"10.0\"/>"),
			"25608 A 区坐标必须来自客户端 DF6 mission level");
		assertTrue(zones.contains("name=\"" + ZONE_B + "\" area_type=\"SPHERE\" zone_type=\"SUB\""),
			"zones_quest.xml 必须注册任务 25608 的第二个 sensory zone");
		assertTrue(zones.contains("<sphere x=\"1513.4391\" y=\"544.90009\" z=\"295.16571\" r=\"10.0\"/>"),
			"25608 B 区坐标必须来自客户端 DF6 mission level");
	}

	@Test
	void huntCounterSelfLoopAdvancesToTheTalkRow() {
		CompiledQuestDefinition compiled = load();

		QuestTransition counter = transition(compiled, "step3", "step3");
		assertEquals(new QuestEvent.KillNpc(HUNT_NPC), counter.event());
		assertEquals(1, counter.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", 9)), counter.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), counter.actions());

		QuestTransition finalKill = transition(compiled, "step3", "step4");
		assertEquals(0, finalKill.priority());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 9)), finalKill.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 4),
			new QuestAction.SetVariable("var1", 0)), finalKill.actions());

		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.START, Map.of("var0", 3, "var1", 9)), finalKill).orElseThrow();
		assertEquals(QuestStatus.START, plan.nextStatus());
		Map<String, Integer> variables =
			compiled.definition().progressLayout().unpack(plan.nextPackedVariables());
		assertEquals(4, variables.get("var0"));
		assertEquals(0, variables.get("var1"));
	}

	@Test
	void rewardRouteOwnsTheTurnInRowAndCompletionPage() {
		CompiledQuestDefinition compiled = load();

		QuestTransition turnIn = transition(compiled, "step6", "reward");
		assertEquals(List.of(new QuestCondition.HasItem(QUEST_ITEM, 1)), turnIn.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(QUEST_ITEM, 1),
			new QuestAction.SetVariable("var0", REWARD_ROW)), turnIn.actions());

		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.START, Map.of("var0", REWARD_ROW), Map.of(QUEST_ITEM, 1)),
			turnIn).orElseThrow();
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(REWARD_ROW,
			compiled.definition().progressLayout().unpack(plan.nextPackedVariables()).get("var0"));

		QuestTransition select = compiled.definition().transitions().stream()
			.filter(candidate -> "reward".equals(candidate.sourceNode())
				&& "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(
				new QuestEvent.TalkToNpc(TURN_IN_NPC, QuestDialogAction.QUEST_SELECT.id())))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			select.afterCommit());

		QuestTransition completion = compiled.definition().transitions().stream()
			.filter(candidate -> "complete".equals(candidate.targetNode()))
			.filter(candidate -> candidate.actions().contains(new QuestAction.CompleteQuest(0)))
			.findFirst().orElseThrow();
		assertEquals("reward", completion.sourceNode());
	}

	@Test
	void staleRewardRowsAreHealedOnEnterWorldAndSelect() {
		CompiledQuestDefinition compiled = load();

		QuestTransition enterWorld = compiled.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.EnterWorld)
			.findFirst().orElseThrow();
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.VariableBelow("var0", REWARD_ROW)), enterWorld.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), enterWorld.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), enterWorld.afterCommit());

		QuestMutationPlan repaired = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.REWARD, Map.of("var0", STALE_REWARD_ROW)), enterWorld).orElseThrow();
		assertEquals(QuestStatus.REWARD, repaired.nextStatus());
		assertEquals(REWARD_ROW,
			compiled.definition().progressLayout().unpack(repaired.nextPackedVariables()).get("var0"));

		QuestTransition selectHeal = compiled.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> candidate.event().equals(
				new QuestEvent.TalkToNpc(TURN_IN_NPC, QuestDialogAction.QUEST_SELECT.id())))
			.findFirst().orElseThrow();
		assertEquals(enterWorld.conditions(), selectHeal.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), selectHeal.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			selectHeal.afterCommit());
		assertEquals(REWARD_ROW, compiled.definition().progressLayout().unpack(
			QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", STALE_REWARD_ROW)),
				selectHeal).orElseThrow().nextPackedVariables()).get("var0"));
	}

	@Test
	void dropMatchesClientCollectProgress() {
		assertTrue(load().definition().metadata().drops().stream().anyMatch(drop ->
				drop.npcId() == 241234
					&& drop.itemId() == QUEST_ITEM
					&& drop.collectingStep() == REWARD_ROW),
			"客户端 quest.xml 的 collect_progress=6 必须与 drop collecting-step 一致");
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
		assertEquals(status, node.projection().status(), label + " status");
		assertEquals(variables, node.projection().variables(), label + " variables");
	}

	private static QuestTransition transition(CompiledQuestDefinition compiled, String source, String target) {
		List<QuestTransition> matches = compiled.definition().transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode()))
			.toList();
		assertEquals(1, matches.size(), source + " -> " + target + " routes");
		return matches.getFirst();
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables) {
		return snapshot(compiled, status, variables, Map.of());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables, Map<Integer, Integer> inventory) {
		Map<String, Integer> packedVariables =
			new LinkedHashMap<>(compiled.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, compiled.id(), status,
			compiled.definition().progressLayout().pack(packedVariables), inventory, Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition load() {
		try (InputStream input = Quest25608RetailSevenStepAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/25608.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 25608.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception exception) {
			throw new AssertionError("failed to compile quest 25608", exception);
		}
	}
}
