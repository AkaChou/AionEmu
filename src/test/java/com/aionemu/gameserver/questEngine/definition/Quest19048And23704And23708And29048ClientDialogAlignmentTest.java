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
 * 锁定四个同构任务的报告页前置、物品扣除时机和原生奖励结算链。
 * Locks report-page ordering, item-removal timing, and native reward-completion chains for four isomorphic quests.
 */
class Quest19048And23704And23708And29048ClientDialogAlignmentTest {
	private static final List<Integer> QUEST_IDS = List.of(19048, 23704, 23708, 29048);

	@Test
	void returnsToEachRewardNpcBeforeOpeningTheNativeRewardWindow() throws Exception {
		assertContract(19048, 798303, 182212216, List.of(
			new QuestAction.GrantReward("EXP", 0, 37405, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 188508000, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)));
		assertContract(23704, 802343, 182215533, List.of(
			new QuestAction.GrantReward("GOLD", 0, 150660, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 3446553, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000231, 2, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 162000124, 11, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)));
		assertContract(23708, 802344, 182215535, List.of(
			new QuestAction.GrantReward("GOLD", 0, 150660, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 3446553, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000231, 2, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 169000010, 561, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)));
		assertContract(29048, 798304, 182212217, List.of(
			new QuestAction.GrantReward("EXP", 0, 37405, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 188508000, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)));
	}

	@Test
	void productionJourneysUseTheReportPageBeforeCompleting() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		for (int questId : QUEST_IDS) {
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
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())), useObject.afterCommit());
		QuestTransition report = talk(definition, "reward", "reward", npcId, QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(new QuestAction.RemoveItem(itemId, 1)), report.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), report.afterCommit());

		QuestTransition completion = talk(definition, "reward", "complete", npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals(completionActions, completion.actions());
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
		try (InputStream input = Quest19048And23704And23708And29048ClientDialogAlignmentTest.class
			.getResourceAsStream("/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
