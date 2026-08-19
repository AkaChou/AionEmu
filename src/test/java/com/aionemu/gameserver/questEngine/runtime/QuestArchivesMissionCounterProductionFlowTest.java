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
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证永恒档案馆主线狩猎任务在第 30 次有效击杀时立即进入领奖，
 * 并保持领奖页面合同。
 * Verifies that Archives mission hunts enter reward on the 30th valid kill and preserve the reward-page contract.
 */
class QuestArchivesMissionCounterProductionFlowTest {
	private static final int KILL_COUNT = 30;
	private static final Set<Integer> LIBRARIANS_ELYOS = Set.of(
		220305, 220308, 220311, 220314, 220317, 220323, 220326, 220327, 220329);
	private static final Set<Integer> LIBRARIANS_ASMODIANS = Set.of(
		220305, 220308, 220311, 220314, 220317, 220323, 220326, 220329);
	private static final Set<Integer> RELIQUARIANS_ASMODIANS = Set.of(
		220307, 220310, 220313, 220316, 220319, 220325, 220328, 220331, 220411);
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(16801, 806148, LIBRARIANS_ELYOS, 220305, true),
		new QuestContract(26801, 806149, LIBRARIANS_ASMODIANS, 220305, true),
		new QuestContract(26803, 806149, RELIQUARIANS_ASMODIANS, 220307, false));

	@TestFactory
	Stream<DynamicTest> completesMissionHuntsOnTheThirtiethKill() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		Map<String, Integer> rewardProjection = contract.incrementOnFinalKill()
			? Map.of("var0", 1)
			: Map.of("var0", 1, "var1", KILL_COUNT);
		assertNode(definition, "reward", QuestStatus.REWARD, rewardProjection);

		QuestEvent targets = new QuestEvent.KillNpcSet(contract.targetNpcIds());
		QuestTransition continuing = transition(definition, "started", "started", targets);
		assertEquals(1, continuing.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableBelow("var1", KILL_COUNT - 1)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition completion = transition(definition, "started", "reward", targets);
		assertEquals(0, completion.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableAtLeast("var1", KILL_COUNT - 1)), completion.conditions());
		assertEquals(expectedFinalKillActions(contract.incrementOnFinalKill()), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			completion.afterCommit());

		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START,
			Map.of("var0", 0, "var1", 0), definition);
		for (int kills = 1; kills < KILL_COUNT; kills++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", 0, "var1", kills), unpack(definition, snapshot));
		}

		QuestMutationPlan finalKill = dispatch(compiled, snapshot,
			new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
		assertEquals(QuestStatus.REWARD, finalKill.nextStatus());
		assertEquals(Map.of("var0", 1, "var1", KILL_COUNT),
			definition.progressLayout().unpack(finalKill.nextPackedVariables()));
		assertEquals(completion.actions(), finalKill.requiredActions());

		assertRewardPages(definition, contract.reportNpcId());
	}

	private static List<QuestAction> expectedFinalKillActions(boolean incrementOnFinalKill) {
		QuestAction counterAction = incrementOnFinalKill
			? new QuestAction.IncrementVariable("var1", 1)
			: new QuestAction.SetVariable("var1", KILL_COUNT);
		return List.of(counterAction, new QuestAction.SetVariable("var0", 1));
	}

	private static void assertRewardPages(QuestDefinition definition, int reportNpcId) {
		QuestTransition success = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(reportNpcId, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			success.afterCommit());

		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(reportNpcId, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
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

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory());
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status,
			Map<String, Integer> variables, QuestDefinition definition) {
		return new QuestSnapshot(7, questId, status, definition.progressLayout().pack(variables), Map.of());
	}

	private static Map<String, Integer> unpack(QuestDefinition definition, QuestSnapshot snapshot) {
		return definition.progressLayout().unpack(snapshot.packedVariables());
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
		try (InputStream input = QuestArchivesMissionCounterProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/**
	 * 保存各任务的目标、报告 NPC 和末次计数动作差异。
	 * Holds per-quest targets, report NPCs, and final-counter action differences.
	 */
	private record QuestContract(int questId, int reportNpcId, Set<Integer> targetNpcIds,
			int sampleTargetNpcId, boolean incrementOnFinalKill) {
	}
}
