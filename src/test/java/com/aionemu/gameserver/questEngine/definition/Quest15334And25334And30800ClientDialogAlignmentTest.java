package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
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

		// 15334/25334 已转 DataDriven 链形：ItemPlay 边推进（3000ms 遗留标准时长）、无条件、
		// PACKET_ONLY；工作物品的完成清理由 planner 追加（定义不再携带显式 RemoveItem）。
		// 30800 仍为遗留 XML（LevelUpLogIn 轴暂缓），保留 UseItem + 显式 RemoveItem 形。
		// 15334/25334 moved to the DataDriven chain shape: an ItemPlay edge advances (the 3000ms
		// legacy standard duration), unconditional with PACKET_ONLY; the planner appends the
		// work-item completion cleanup (the definition no longer carries an explicit RemoveItem).
		// 30800 stays on the legacy XML (the LevelUpLogIn axis is deferred), keeping the UseItem
		// plus explicit RemoveItem shape.
		boolean dataDriven = questId != 30800;
		QuestTransition itemUse = transition(definition, "started", "reward", dataDriven
			? new QuestEvent.ItemPlay(itemId, 3000)
			: new QuestEvent.UseItem(itemId));
		if (dataDriven) {
			assertEquals(List.of(), itemUse.conditions());
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), itemUse.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.PACKET_ONLY)), itemUse.afterCommit());
		} else {
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), itemUse.conditions());
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), itemUse.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), itemUse.afterCommit());
		}

		QuestTransition useObject = talk(definition, "reward", "reward", npcId, QuestDialogAction.USE_OBJECT);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(dataDriven
			? QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()
			: QuestDialogPage.DEFAULT_SUCCESS.id())), useObject.afterCommit());
		QuestTransition report = talk(definition, "reward", "reward", npcId, QuestDialogAction.SELECT_QUEST_REWARD);
		assertTrue(report.actions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), report.afterCommit());

		QuestTransition completion = talk(definition, "reward", "complete", npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		List<QuestAction> expectedActions = new java.util.ArrayList<>();
		if (!dataDriven) {
			expectedActions.add(new QuestAction.RemoveItem(itemId, 1));
		}
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

	// 退役任务统一走生产视图（真端 overlay 合成；旧 XML 只在 git 历史里）。
	// Retired quests resolve through the production view (retail overlay; the old XML lives in
	// git history only).
	private static CompiledQuestDefinition definition(int questId) throws Exception {
		return ProductionQuestDefinitions.definition(questId);
	}
}
