package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 锁定三条物品使用任务的报告页、物品扣除时机和原生奖励结算链。
 * Locks report-page ordering, item-removal timing, and native reward-completion chains for three item-use quests.
 */
class Quest15334And25334And30800ClientDialogAlignmentTest {
	@Test
	void returnsToEachNpcBeforeOpeningTheNativeRewardWindow() throws Exception {
		assertContract(15334, 805330, 182215919, List.of(
			new QuestAction.GrantReward("ITEM", 182215833, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)));
		assertContract(25334, 805342, 182215920, List.of(
			new QuestAction.GrantReward("ITEM", 182215848, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)));
		assertContract(30800, 834987, 182216169, List.of(
			new QuestAction.GrantReward("EXP", 0, 8841600, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 182216169, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)));
	}

	@Test
	void productionJourneysUseTheReportPageBeforeCompleting() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		for (int questId : List.of(15334, 25334, 30800)) {
			CompiledQuestDefinition definition = definition(questId);
			QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
			assertTrue(planned.planned(), () -> questId + " " + planned.failure());
			assertTrue(planned.plan().steps().stream().anyMatch(step ->
				step.kind() == QuestProductionJourneyPlanner.StepKind.USE_OBJECT));
			assertTrue(planned.plan().steps().stream().anyMatch(step ->
				step.kind() == QuestProductionJourneyPlanner.StepKind.NATIVE_REWARD_ACTION));

			QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
				.execute(definition, oracle, planned.plan());
			assertTrue(executed.completed(), () -> questId + " " + executed.failure());
		}
	}

	private static void assertContract(int questId, int npcId, int itemId, List<QuestAction> completionActions)
			throws Exception {
		QuestDefinition definition = definition(questId).definition();
		for (QuestAction action : completionActions) {
			if (action instanceof QuestAction.GrantReward reward) {
				assertTrue(definition.metadata().rewards().contains(
					new QuestReward(reward.kind(), reward.id(), reward.amount())));
			}
		}

		QuestTransition itemUse = transition(definition, "started", "reward", new QuestEvent.UseItem(itemId));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), itemUse.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), itemUse.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), itemUse.afterCommit());

		QuestTransition useObject = talk(definition, "reward", "reward", npcId, QuestDialogAction.USE_OBJECT);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			useObject.afterCommit());
		QuestTransition report = talk(definition, "reward", "reward", npcId, QuestDialogAction.SELECT_QUEST_REWARD);
		assertTrue(report.actions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), report.afterCommit());

		QuestTransition completion = talk(definition, "reward", "complete", npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		List<QuestAction> expectedActions = new java.util.ArrayList<>();
		expectedActions.add(new QuestAction.RemoveItem(itemId, 1));
		expectedActions.addAll(completionActions);
		assertEquals(expectedActions, completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), completion.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action.id()));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Quest15334And25334And30800ClientDialogAlignmentTest.class
			.getResourceAsStream("/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
