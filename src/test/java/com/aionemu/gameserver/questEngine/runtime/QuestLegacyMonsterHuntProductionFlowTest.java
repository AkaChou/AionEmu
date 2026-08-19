package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证旧 monster-hunt 模板的末次击杀、独立计数和报告 owner 合同。
 * Verifies final-kill, independent-counter, and report-owner contracts from the legacy monster-hunt template.
 */
class QuestLegacyMonsterHuntProductionFlowTest {
	private static final Set<Integer> QUEST_1842_REGULAR_TARGETS = Set.of(
		215094, 215095, 215096, 215097, 215098, 215099, 215100, 215101, 215102, 215103,
		215104, 215105, 215106, 215107, 215108, 215109, 215110, 215111, 215112, 215113,
		215114, 215115, 215116, 215117, 215118, 215119, 215120, 215121, 215122, 215123,
		215124, 215125, 215126, 215127, 215128, 215129, 215130, 215131, 215132, 215133,
		215135, 215136, 215285, 215286, 215287, 215288, 215289, 215290, 215291, 215292,
		215293, 215294, 215295, 215296, 215297, 215298, 215299, 215300, 215301, 215302,
		215303, 215304, 215305, 215306, 215307, 215308, 215309, 215310, 215311, 215312,
		215313, 215314, 215315, 215316);
	private static final Set<Integer> QUEST_18951_TARGETS = IntStream.rangeClosed(236100, 236220)
		.boxed().collect(Collectors.toUnmodifiableSet());
	private static final List<MonsterHuntContract> SIMPLE_HUNTS = List.of(
		new MonsterHuntContract(18314, Set.of(702656, 730373), 730373, 7),
		new MonsterHuntContract(18951, QUEST_18951_TARGETS, 236100, 25),
		new MonsterHuntContract(18972, Set.of(235824, 235825), 235824, 6),
		new MonsterHuntContract(18973, Set.of(235867, 235868), 235867, 6),
		new MonsterHuntContract(18974, Set.of(235881), 235881, 6));

	@TestFactory
	Stream<DynamicTest> simpleMonsterHuntsReachRewardOnTheRetailFinalKill() {
		return SIMPLE_HUNTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertSimpleMonsterHunt(contract)));
	}

	@Test
	void quest1842PreservesIndependentObjectivesAndReportsToTheRetailEndNpc() throws Exception {
		CompiledQuestDefinition compiled = load(1842);
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "ready", QuestStatus.START, Map.of("var0", 80, "var1", 1));
		assertQuest1842Routes(definition);

		assertQuest1842Order(compiled, true);
		assertQuest1842Order(compiled, false);
		assertReport(definition, "ready", 278503);
	}

	@Test
	void quest21120ReachesTurnInOnTheTenthKill() throws Exception {
		CompiledQuestDefinition compiled = load(21120);
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "m1", QuestStatus.START, Map.of("var0", 10));
		QuestEvent configuredEvent = new QuestEvent.KillNpcSet(Set.of(216102, 216103));
		assertCounterRoute(transition(definition, "started", "started", configuredEvent), 1,
			List.of(new QuestCondition.VariableBelow("var0", 9)), "var0");
		assertCounterRoute(transition(definition, "started", "m1", configuredEvent), 0,
			List.of(new QuestCondition.QuestVariableIs("var0", 9)), "var0");

		QuestEvent event = new QuestEvent.KillNpc(216102);
		QuestSnapshot snapshot = snapshot(21120, QuestStatus.START, Map.of("var0", 0), definition);
		for (int count = 1; count <= 10; count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, event);
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count), definition.progressLayout().unpack(snapshot.packedVariables()));
		}
		assertNoMatch(compiled, snapshot, event);
		assertReport(definition, "m1", 799291);
		QuestMutationPlan report = dispatch(compiled, snapshot,
			new QuestEvent.TalkToNpc(799291, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(QuestStatus.REWARD, report.nextStatus());
		assertEquals(Map.of("var0", 10), definition.progressLayout().unpack(report.nextPackedVariables()));
	}

	private static void assertSimpleMonsterHunt(MonsterHuntContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", contract.requiredKills()));

		QuestEvent configuredEvent = new QuestEvent.KillNpcSet(contract.targetNpcIds());
		QuestTransition continuing = transition(definition, "started", "started", configuredEvent);
		assertEquals(1, continuing.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", contract.requiredKills() - 1)),
			continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition completion = transition(definition, "started", "reward", configuredEvent);
		assertEquals(0, completion.priority());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", contract.requiredKills() - 1)),
			completion.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", contract.requiredKills())),
			completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			completion.afterCommit());

		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START,
			Map.of("var0", 0), definition);
		for (int count = 1; count < contract.requiredKills(); count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
		}

		QuestMutationPlan finalKill = dispatch(compiled, snapshot,
			new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
		assertEquals(QuestStatus.REWARD, finalKill.nextStatus());
		assertEquals(Map.of("var0", contract.requiredKills()),
			definition.progressLayout().unpack(finalKill.nextPackedVariables()));
		assertEquals(completion.actions(), finalKill.requiredActions());
	}

	private static void assertQuest1842Routes(QuestDefinition definition) {
		QuestEvent regular = new QuestEvent.KillNpcSet(QUEST_1842_REGULAR_TARGETS);
		assertCounterRoute(transition(definition, "started", "started", regular, 2), 2,
			List.of(new QuestCondition.VariableBelow("var0", 79)), "var0");
		assertCounterRoute(transition(definition, "started", "started", regular, 1), 1,
			List.of(
				new QuestCondition.QuestVariableIs("var0", 79),
				new QuestCondition.VariableBelow("var1", 1)), "var0");
		assertCounterRoute(transition(definition, "started", "ready", regular), 0,
			List.of(
				new QuestCondition.QuestVariableIs("var0", 79),
				new QuestCondition.VariableAtLeast("var1", 1)), "var0");

		QuestEvent general = new QuestEvent.KillNpc(215134);
		assertCounterRoute(transition(definition, "started", "started", general), 1,
			List.of(
				new QuestCondition.VariableBelow("var1", 1),
				new QuestCondition.VariableBelow("var0", 80)), "var1");
		assertCounterRoute(transition(definition, "started", "ready", general), 0,
			List.of(
				new QuestCondition.VariableBelow("var1", 1),
				new QuestCondition.VariableAtLeast("var0", 80)), "var1");
	}

	private static void assertCounterRoute(QuestTransition transition, int priority,
			List<QuestCondition> conditions, String field) {
		assertEquals(priority, transition.priority());
		assertEquals(conditions, transition.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), transition.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition.afterCommit());
	}

	private static void assertQuest1842Order(CompiledQuestDefinition compiled, boolean generalFirst) {
		QuestDefinition definition = compiled.definition();
		QuestSnapshot snapshot = snapshot(1842, QuestStatus.START, Map.of("var0", 0, "var1", 0), definition);
		if (generalFirst) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(215134)));
			assertEquals(Map.of("var0", 0, "var1", 1), definition.progressLayout().unpack(snapshot.packedVariables()));
		}
		for (int count = 1; count <= 80; count++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(215094)));
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(count, definition.progressLayout().unpack(snapshot.packedVariables()).get("var0"));
		}
		if (!generalFirst) {
			assertEquals(Map.of("var0", 80, "var1", 0),
				definition.progressLayout().unpack(snapshot.packedVariables()));
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(215134)));
		}
		assertEquals(QuestStatus.START, snapshot.status());
		assertEquals(Map.of("var0", 80, "var1", 1), definition.progressLayout().unpack(snapshot.packedVariables()));
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(215094));
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(215134));
		QuestMutationPlan report = dispatch(compiled, snapshot,
			new QuestEvent.TalkToNpc(278503, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(QuestStatus.REWARD, report.nextStatus());
		assertEquals(Map.of("var0", 80, "var1", 1),
			definition.progressLayout().unpack(report.nextPackedVariables()));
	}

	private static void assertReport(QuestDefinition definition, String source, int npcId) {
		QuestTransition page = transition(definition, source, source,
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), page.conditions());
		assertEquals(List.of(), page.actions());
		assertNull(page.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			page.afterCommit());

		QuestTransition report = transition(definition, source, "reward",
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertNull(report.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
	}

	private static QuestMutationPlan dispatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestMutationPlan> plans = compiled.definition().transitions().stream()
			.map(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).orElse(null))
			.filter(Objects::nonNull)
			.toList();
		assertEquals(1, plans.size(), () -> compiled.id() + " " + event + " "
			+ compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		return plans.getFirst();
	}

	private static void assertNoMatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot, QuestEvent event) {
		assertTrue(compiled.definition().transitions().stream().noneMatch(transition ->
			QuestMutationPlanner.plan(compiled, snapshot, event, transition).isPresent()));
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory());
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status, Map<String, Integer> variables,
			QuestDefinition definition) {
		return new QuestSnapshot(7, questId, status, definition.progressLayout().pack(variables), Map.of());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return transition(definition, source, target, event, null);
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event, Integer priority) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> priority == null || Objects.equals(candidate.priority(), priority))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event + " " + priority);
		return matches.getFirst();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestLegacyMonsterHuntProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/**
	 * 保存简单 monster-hunt 的目标和阈值差异。
	 * Holds target and threshold differences for simple monster hunts.
	 */
	private record MonsterHuntContract(int questId, Set<Integer> targetNpcIds, int sampleTargetNpcId,
			int requiredKills) {
	}
}
