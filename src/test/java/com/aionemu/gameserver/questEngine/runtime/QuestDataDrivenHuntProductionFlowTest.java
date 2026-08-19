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
 * 验证档案馆 data-driven hunt 任务的累计击杀、末次清零和领奖合同。
 * Verifies accumulated kills, final-kill reset, and reward contracts for Archives data-driven hunts.
 */
class QuestDataDrivenHuntProductionFlowTest {
	private static final int KILL_COUNT = 30;
	private static final Set<Integer> TARGET_NPC_IDS = Set.of(
		220305, 220308, 220311, 220314, 220317, 220323, 220326, 220329);
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(16805, 806148),
		new QuestContract(26805, 806149));

	@TestFactory
	Stream<DynamicTest> preservesRetailHuntAndRewardFlow() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 0));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0));

		QuestEvent configuredTargets = new QuestEvent.KillNpcSet(TARGET_NPC_IDS);
		QuestTransition count = transition(definition, "started", "started", configuredTargets);
		assertEquals(1, count.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", KILL_COUNT - 1)), count.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), count.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			count.afterCommit());

		QuestTransition finalKill = transition(definition, "started", "reward", configuredTargets);
		assertEquals(0, finalKill.priority());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", KILL_COUNT - 1)),
			finalKill.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var1", 0),
			new QuestAction.SetVariable("var0", 1)), finalKill.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			finalKill.afterCommit());

		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START,
			Map.of("var0", 0, "var1", 0), definition);
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(220327));
		for (int kills = 1; kills < KILL_COUNT; kills++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.KillNpc(220305));
			assertEquals(QuestStatus.START, plan.nextStatus());
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(Map.of("var0", 0, "var1", kills),
				definition.progressLayout().unpack(snapshot.packedVariables()));
		}

		QuestMutationPlan completedHunt = dispatch(compiled, snapshot, new QuestEvent.KillNpc(220305));
		assertEquals(QuestStatus.REWARD, completedHunt.nextStatus());
		assertEquals(Map.of("var0", 1, "var1", 0),
			definition.progressLayout().unpack(completedHunt.nextPackedVariables()));
		assertEquals(finalKill.actions(), completedHunt.requiredActions());

		assertRewardAndCompletion(compiled, definition, contract);
	}

	private static void assertRewardAndCompletion(CompiledQuestDefinition compiled,
			QuestDefinition definition, QuestContract contract) {
		QuestTransition success = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), success.conditions());
		assertEquals(List.of(), success.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			success.afterCommit());

		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(), preview.conditions());
		assertEquals(List.of(), preview.actions());
		assertEquals(List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			preview.afterCommit());

		QuestEvent completionEvent = new QuestEvent.TalkToNpc(
			contract.reportNpcId(), QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		QuestTransition completion = transition(definition, "reward", "complete", completionEvent);
		List<QuestAction> expectedActions = List.of(
			new QuestAction.GrantReward("EXP", 0, 69327360, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 166100009, 10),
			new QuestAction.CompleteQuest(0));
		assertEquals(expectedActions, completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		QuestSnapshot reward = snapshot(contract.questId(), QuestStatus.REWARD,
			Map.of("var0", 1, "var1", 0), definition);
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, reward, completionEvent, completion)
			.orElseThrow();
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(Map.of("var0", 0, "var1", 0),
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
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestDataDrivenHuntProductionFlowTest.class.getResourceAsStream(
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
