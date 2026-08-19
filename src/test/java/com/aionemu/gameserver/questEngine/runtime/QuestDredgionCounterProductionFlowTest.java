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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证钱特拉德雷金任务的击杀与结算计数可独立、任意顺序推进。
 * Verifies independent, order-agnostic kill and settlement counters for the Chantra Dredgion quests.
 */
class QuestDredgionCounterProductionFlowTest {
	private static final int KILL_NPC_ID = 216866;
	private static final int KILL_COUNT = 15;
	private static final int SETTLEMENT_COUNT = 6;
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(3725, 798928),
		new QuestContract(4725, 799226));

	@TestFactory
	Stream<DynamicTest> preservesIndependentRetailCountersAndTurnInFlow() {
		return CONTRACTS.stream().flatMap(contract -> Stream.of(
			DynamicTest.dynamicTest("quest " + contract.questId() + " kills first",
				() -> assertContract(contract, true)),
			DynamicTest.dynamicTest("quest " + contract.questId() + " settlements first",
				() -> assertContract(contract, false))));
	}

	private static void assertContract(QuestContract contract, boolean killsFirst) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "reward", QuestStatus.REWARD,
			Map.of("var1", SETTLEMENT_COUNT, "var2", KILL_COUNT));

		QuestTransition kill = transition(definition, "started", "started",
			new QuestEvent.KillNpc(KILL_NPC_ID));
		assertCounter(kill, new QuestCondition.VariableBelow("var2", KILL_COUNT), "var2");
		QuestTransition settlement = transition(definition, "started", "started",
			new QuestEvent.DredgionReward());
		assertCounter(settlement, new QuestCondition.VariableBelow("var1", SETTLEMENT_COUNT), "var1");

		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START,
			Map.of("var1", 0, "var2", 0), definition);
		if (killsFirst) {
			snapshot = advanceKills(compiled, snapshot);
			snapshot = advanceSettlements(compiled, snapshot);
		} else {
			snapshot = advanceSettlements(compiled, snapshot);
			snapshot = advanceKills(compiled, snapshot);
		}
		assertEquals(Map.of("var1", SETTLEMENT_COUNT, "var2", KILL_COUNT),
			definition.progressLayout().unpack(snapshot.packedVariables()));
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(KILL_NPC_ID));
		assertNoMatch(compiled, snapshot, new QuestEvent.DredgionReward());

		assertTurnIn(definition, contract);
		QuestMutationPlan report = dispatch(compiled, snapshot,
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(QuestStatus.REWARD, report.nextStatus());
		assertEquals(Map.of("var1", SETTLEMENT_COUNT, "var2", KILL_COUNT),
			definition.progressLayout().unpack(report.nextPackedVariables()));
	}

	private static QuestSnapshot advanceKills(CompiledQuestDefinition compiled, QuestSnapshot snapshot) {
		for (int count = 1; count <= KILL_COUNT; count++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(KILL_NPC_ID)));
			assertEquals(count, compiled.definition().progressLayout().unpack(snapshot.packedVariables()).get("var2"));
		}
		return snapshot;
	}

	private static QuestSnapshot advanceSettlements(CompiledQuestDefinition compiled, QuestSnapshot snapshot) {
		for (int count = 1; count <= SETTLEMENT_COUNT; count++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.DredgionReward()));
			assertEquals(count, compiled.definition().progressLayout().unpack(snapshot.packedVariables()).get("var1"));
		}
		return snapshot;
	}

	private static void assertCounter(QuestTransition transition, QuestCondition condition, String field) {
		assertEquals(List.of(condition), transition.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), transition.actions());
		assertNull(transition.priority());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition.afterCommit());
	}

	private static void assertTurnIn(QuestDefinition definition, QuestContract contract) {
		List<QuestCondition> completed = List.of(
			new QuestCondition.QuestVariableIs("var1", SETTLEMENT_COUNT),
			new QuestCondition.QuestVariableIs("var2", KILL_COUNT));
		QuestTransition page = transition(definition, "started", "started",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(completed, page.conditions());
		assertEquals(List.of(), page.actions());
		assertNull(page.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			page.afterCommit());

		QuestTransition report = transition(definition, "started", "reward",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(completed, report.conditions());
		assertEquals(List.of(), report.actions());
		assertNull(report.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(), completion.conditions());
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 3953541, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000469, 5),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertNull(completion.priority());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
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
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestDredgionCounterProductionFlowTest.class.getResourceAsStream(
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
