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
 * 验证永恒摇篮双阵营重逢任务的自动接取、累计击杀、对话交接、电影和领奖生产合同。
 * 当前 26823 的额外守卫目标与最终目标保留为待更强零售证据确认的生产值。
 * Verifies the auto-start, accumulated kills, dialog handoff, movies, and reward production contracts for the
 * faction-paired Cradle reunion quests. The additional guard targets and final target currently used by quest 26823
 * remain production values pending stronger retail evidence.
 */
class QuestCradleReunionProductionFlowTest {
	private static final int GUARD_KILL_COUNT = 5;
	private static final String FIRST_ZONE = "IDETERNITY_02_Q16823_A_301550000";
	private static final String SECOND_ZONE = "IDETERNITY_02_Q16823_B_301550000";
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(16823, "k", Set.of(220607, 220608, 220610, 220611), 220607,
			806284, 806285, 939, 220540, 940),
		new QuestContract(26823, "s", Set.of(220613, 220614, 220615, 220616, 220617, 220618), 220613,
			806289, 806290, 941, 220593, 942));

	@TestFactory
	Stream<DynamicTest> preservesProductionProgressDialogAndRewardFlow() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNodes(definition, contract.nodePrefix());
		assertAutoStartRoutes(definition, contract.questId() - 1);

		QuestEvent guards = new QuestEvent.KillNpcSet(contract.guardNpcIds());
		QuestTransition count = transition(definition, "started", "started", guards);
		assertEquals(1, count.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableBelow("var1", GUARD_KILL_COUNT - 1)), count.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), count.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			count.afterCommit());

		String stage1 = contract.stage(1);
		QuestTransition finalGuard = transition(definition, "started", stage1, guards);
		assertEquals(0, finalGuard.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableAtLeast("var1", GUARD_KILL_COUNT - 1)), finalGuard.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var1", 0),
			new QuestAction.SetVariable("var0", 1)), finalGuard.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			finalGuard.afterCommit());

		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START,
			Map.of("var0", 0, "var1", 0), definition);
		for (int kills = 1; kills < GUARD_KILL_COUNT; kills++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.KillNpc(contract.sampleGuardNpcId()));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", 0, "var1", kills), unpack(definition, snapshot));
		}

		QuestMutationPlan guardCompletion = dispatch(compiled, snapshot,
			new QuestEvent.KillNpc(contract.sampleGuardNpcId()));
		snapshot = nextSnapshot(snapshot, guardCompletion);
		assertEquals(Map.of("var0", 1, "var1", 0), unpack(definition, snapshot));
		assertEquals(finalGuard.actions(), guardCompletion.requiredActions());

		QuestMutationPlan firstZone = dispatch(compiled, snapshot, new QuestEvent.EnterZone(FIRST_ZONE));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.PlayMovie(contract.firstMovieId())), firstZone.afterCommit());
		snapshot = nextSnapshot(snapshot, firstZone);
		assertEquals(Map.of("var0", 2, "var1", 0), unpack(definition, snapshot));

		QuestEvent select = new QuestEvent.TalkToNpc(
			contract.handoffNpcId(), QuestDialogAction.QUEST_SELECT.id());
		QuestMutationPlan selectPlan = dispatch(compiled, snapshot, select);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_1.id())),
			selectPlan.afterCommit());
		assertNoMatch(compiled, snapshot, new QuestEvent.TalkToNpc(
			contract.handoffNpcId(), QuestDialogAction.SETPRO1.id()));

		QuestEvent handoff = new QuestEvent.TalkToNpc(
			contract.handoffNpcId(), QuestDialogAction.SETPRO3.id());
		QuestMutationPlan handoffPlan = dispatch(compiled, snapshot, handoff);
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), handoffPlan.afterCommit());
		snapshot = nextSnapshot(snapshot, handoffPlan);
		assertEquals(Map.of("var0", 3, "var1", 0), unpack(definition, snapshot));

		QuestMutationPlan secondZone = dispatch(compiled, snapshot, new QuestEvent.EnterZone(SECOND_ZONE));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			secondZone.afterCommit());
		snapshot = nextSnapshot(snapshot, secondZone);
		assertEquals(Map.of("var0", 4, "var1", 0), unpack(definition, snapshot));

		QuestMutationPlan finalKill = dispatch(compiled, snapshot, new QuestEvent.KillNpc(contract.finalNpcId()));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.PlayMovie(contract.finalMovieId())), finalKill.afterCommit());
		snapshot = nextSnapshot(snapshot, finalKill);
		assertEquals(QuestStatus.REWARD, snapshot.status());
		assertEquals(Map.of("var0", 5, "var1", 0), unpack(definition, snapshot));

		assertRewardAndCompletion(compiled, definition, snapshot, contract);
	}

	private static void assertNodes(QuestDefinition definition, String nodePrefix) {
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		for (int stage = 1; stage <= 4; stage++) {
			assertNode(definition, nodePrefix + stage, QuestStatus.START,
				Map.of("var0", stage, "var1", 0));
		}
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 5, "var1", 0));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0));
	}

	private static void assertAutoStartRoutes(QuestDefinition definition, int prerequisiteQuestId) {
		QuestTransition levelUp = transition(definition, "unaccepted", "started", new QuestEvent.LevelUp());
		assertEquals(List.of(
			new QuestCondition.StartEligible(),
			new QuestCondition.QuestsFinished(Set.of(prerequisiteQuestId))), levelUp.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			levelUp.afterCommit());

		QuestTransition zoneMissionEnd = transition(definition, "unaccepted", "started",
			new QuestEvent.ZoneMissionEnd());
		assertEquals(List.of(new QuestCondition.StartEligible()), zoneMissionEnd.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			zoneMissionEnd.afterCommit());
	}

	private static void assertRewardAndCompletion(CompiledQuestDefinition compiled,
			QuestDefinition definition, QuestSnapshot reward, QuestContract contract) {
		QuestTransition success = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.rewardNpcId(), QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			success.afterCommit());

		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.rewardNpcId(), QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestEvent completionEvent = new QuestEvent.TalkToNpc(
			contract.rewardNpcId(), QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		QuestTransition completion = transition(definition, "reward", "complete", completionEvent);
		List<QuestAction> expectedActions = List.of(
			new QuestAction.GrantReward("GOLD", 0, 405720, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 40757397, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000475, 2),
			new QuestAction.CompleteQuest(0));
		assertEquals(expectedActions, completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

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
		try (InputStream input = QuestCradleReunionProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/**
	 * 保存双阵营任务共享流程中的任务专用生产值。
	 * 争议目标仅锁定当前值，不声明零售权威。
	 * Holds quest-specific production values for the faction-paired flow; disputed targets lock current values without
	 * claiming retail authority.
	 */
	private record QuestContract(int questId, String nodePrefix, Set<Integer> guardNpcIds,
			int sampleGuardNpcId, int handoffNpcId, int rewardNpcId, int firstMovieId,
			int finalNpcId, int finalMovieId) {
		private String stage(int number) {
			return nodePrefix + number;
		}
	}
}
