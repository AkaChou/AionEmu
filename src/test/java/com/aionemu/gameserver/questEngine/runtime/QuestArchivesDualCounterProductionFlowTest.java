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
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证档案馆双计数任务可按任意顺序完成，并由最后一次有效击杀立即进入领奖。
 * Verifies order-agnostic Archives counters and immediate reward entry on the final valid kill.
 */
class QuestArchivesDualCounterProductionFlowTest {
	private static final int SOLDIER_COUNT = 30;
	private static final int BOSS_COUNT = 2;
	private static final Set<Integer> SOLDIER_NPC_IDS = Set.of(
		220306, 220309, 220312, 220315, 220318, 220324, 220327, 220330);
	private static final Set<Integer> BOSS_NPC_IDS = Set.of(
		857450, 857452, 857454, 857456, 857458, 857459);
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(16806, 806148),
		new QuestContract(26806, 806149));

	@TestFactory
	Stream<DynamicTest> preservesIndependentCountersAndRewardFlow() {
		return CONTRACTS.stream().flatMap(contract -> Stream.of(
			DynamicTest.dynamicTest("quest " + contract.questId() + " soldiers first",
				() -> assertContract(contract, true)),
			DynamicTest.dynamicTest("quest " + contract.questId() + " bosses first",
				() -> assertContract(contract, false))));
	}

	private static void assertContract(QuestContract contract, boolean soldiersFirst) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD,
			Map.of("var0", 1, "var1", SOLDIER_COUNT, "var2", BOSS_COUNT));
		assertRoutes(definition);

		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START,
			Map.of("var0", 0, "var1", 0, "var2", 0), definition);
		if (soldiersFirst) {
			snapshot = dispatchSoldiers(compiled, snapshot, false);
			assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(220306));
			snapshot = dispatchBosses(compiled, snapshot, true);
		} else {
			snapshot = dispatchBosses(compiled, snapshot, false);
			assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(857450));
			snapshot = dispatchSoldiers(compiled, snapshot, true);
		}
		assertEquals(QuestStatus.REWARD, snapshot.status());
		assertEquals(Map.of("var0", 1, "var1", SOLDIER_COUNT, "var2", BOSS_COUNT),
			definition.progressLayout().unpack(snapshot.packedVariables()));

		assertRewardAndCompletion(compiled, definition, contract);
	}

	private static void assertRoutes(QuestDefinition definition) {
		QuestEvent soldiers = new QuestEvent.KillNpcSet(SOLDIER_NPC_IDS);
		assertCounter(transition(definition, "started", "started", soldiers, 2),
			List.of(new QuestCondition.VariableBelow("var1", SOLDIER_COUNT - 1)), "var1");
		assertCounter(transition(definition, "started", "started", soldiers, 1),
			List.of(
				new QuestCondition.QuestVariableIs("var1", SOLDIER_COUNT - 1),
				new QuestCondition.VariableBelow("var2", BOSS_COUNT)), "var1");
		assertFinalCounter(transition(definition, "started", "reward", soldiers, 0),
			List.of(
				new QuestCondition.QuestVariableIs("var1", SOLDIER_COUNT - 1),
				new QuestCondition.VariableAtLeast("var2", BOSS_COUNT)), "var1");

		QuestEvent bosses = new QuestEvent.KillNpcSet(BOSS_NPC_IDS);
		assertCounter(transition(definition, "started", "started", bosses, 2),
			List.of(new QuestCondition.VariableBelow("var2", BOSS_COUNT - 1)), "var2");
		assertCounter(transition(definition, "started", "started", bosses, 1),
			List.of(
				new QuestCondition.QuestVariableIs("var2", BOSS_COUNT - 1),
				new QuestCondition.VariableBelow("var1", SOLDIER_COUNT)), "var2");
		assertFinalCounter(transition(definition, "started", "reward", bosses, 0),
			List.of(
				new QuestCondition.VariableAtLeast("var1", SOLDIER_COUNT),
				new QuestCondition.QuestVariableIs("var2", BOSS_COUNT - 1)), "var2");
	}

	private static void assertCounter(QuestTransition transition, List<QuestCondition> conditions,
			String field) {
		assertEquals(conditions, transition.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), transition.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition.afterCommit());
	}

	private static void assertFinalCounter(QuestTransition transition, List<QuestCondition> conditions,
			String field) {
		assertEquals(conditions, transition.conditions());
		assertEquals(List.of(
			new QuestAction.IncrementVariable(field, 1),
			new QuestAction.SetVariable("var0", 1)), transition.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			transition.afterCommit());
	}

	private static QuestSnapshot dispatchSoldiers(CompiledQuestDefinition compiled,
			QuestSnapshot snapshot, boolean completesQuest) {
		for (int kills = 1; kills <= SOLDIER_COUNT; kills++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.KillNpc(220306));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(completesQuest && kills == SOLDIER_COUNT ? QuestStatus.REWARD : QuestStatus.START,
				snapshot.status());
			assertEquals(kills,
				compiled.definition().progressLayout().unpack(snapshot.packedVariables()).get("var1"));
		}
		return snapshot;
	}

	private static QuestSnapshot dispatchBosses(CompiledQuestDefinition compiled,
			QuestSnapshot snapshot, boolean completesQuest) {
		for (int kills = 1; kills <= BOSS_COUNT; kills++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.KillNpc(857450));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(completesQuest && kills == BOSS_COUNT ? QuestStatus.REWARD : QuestStatus.START,
				snapshot.status());
			assertEquals(kills,
				compiled.definition().progressLayout().unpack(snapshot.packedVariables()).get("var2"));
		}
		return snapshot;
	}

	private static void assertRewardAndCompletion(CompiledQuestDefinition compiled,
			QuestDefinition definition, QuestContract contract) {
		QuestTransition success = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.QUEST_SELECT.id()), null);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			success.afterCommit());

		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.SELECT_QUEST_REWARD.id()), null);
		assertEquals(List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			preview.afterCommit());

		QuestEvent completionEvent = new QuestEvent.TalkToNpc(
			contract.reportNpcId(), QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		QuestTransition completion = transition(definition, "reward", "complete", completionEvent, null);
		List<QuestAction> expectedActions = List.of(
			new QuestAction.GrantReward("EXP", 0, 74815776, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 166100009, 10),
			new QuestAction.CompleteQuest(0));
		assertEquals(expectedActions, completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		QuestSnapshot reward = snapshot(contract.questId(), QuestStatus.REWARD,
			Map.of("var0", 1, "var1", SOLDIER_COUNT, "var2", BOSS_COUNT), definition);
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, reward, completionEvent, completion)
			.orElseThrow();
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(Map.of("var0", 0, "var1", 0, "var2", 0),
			definition.progressLayout().unpack(plan.nextPackedVariables()));
		assertEquals(expectedActions, plan.requiredActions());
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

	private static void assertNoMatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		assertTrue(compiled.definition().transitions().stream().noneMatch(transition ->
			QuestMutationPlanner.plan(compiled, snapshot, event, transition).isPresent()));
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory());
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status,
			Map<String, Integer> variables, QuestDefinition definition) {
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
		try (InputStream input = QuestArchivesDualCounterProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record QuestContract(int questId, int reportNpcId) {
	}
}
