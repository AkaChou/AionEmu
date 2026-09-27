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
 * 锁定任务 13704 的物品使用、Fona 报告页与原生奖励结算链。
 * Locks quest 13704's item use, Fona report page, and native reward-completion chain.
 */
class Quest13704ClientDialogAlignmentTest {
	private static final int FONA = 802331;
	private static final int INTRUSION_MINE = 182215527;

	@Test
	void returnsToFonaBeforeOpeningTheNativeRewardWindowAndConsumesTheMineOnReport() throws Exception {
		QuestDefinition definition = definition().definition();

		QuestTransition itemUse = transition(definition, "started", "reward", new QuestEvent.UseItem(INTRUSION_MINE));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), itemUse.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), itemUse.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), itemUse.afterCommit());

		// P0-2 规范形交付：REWARD 态 USE_OBJECT/QUEST_SELECT 重开分档奖励窗（SELECT5 报告页删除）；
		// 1009 预览通道保留并回收演出道具，窗页同样按档位查表。
		// P0-2 canonical delivery: the reward-state USE_OBJECT/QUEST_SELECT re-opens the tiered
		// reward window (the SELECT5 page is gone); the 1009 preview channel stays and removes the
		// play item, its window likewise resolved by tier.
		int rewardWindow = QuestDialogPage.rewardWindowForTier(
			definition.metadata().rewardGroups().size() - 1).orElseThrow().id();
		assertPage(definition, QuestDialogAction.USE_OBJECT, rewardWindow);
		assertPage(definition, QuestDialogAction.QUEST_SELECT, rewardWindow);
		QuestTransition report = talk(definition, "reward", "reward", QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(new QuestAction.RemoveItem(INTRUSION_MINE, 1)), report.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(rewardWindow)), report.afterCommit());

		QuestTransition completion = talk(definition, "reward", "complete", QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 150660, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 3446553, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000231, 2, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 162000124, 11, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), completion.afterCommit());
	}

	@Test
	void productionJourneyUsesTheFonaReportPageBeforeCompleting() throws Exception {
		CompiledQuestDefinition definition = definition();
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);

		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));
		assertTrue(planned.plan().steps().stream().anyMatch(step ->
			step.kind() == QuestProductionJourneyPlanner.StepKind.USE_OBJECT
				&& step.transition().event().equals(new QuestEvent.TalkToNpc(FONA, QuestDialogAction.USE_OBJECT.id()))));
		assertTrue(planned.plan().steps().stream().anyMatch(step ->
			step.kind() == QuestProductionJourneyPlanner.StepKind.NATIVE_REWARD_ACTION));

		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	private static void assertPage(QuestDefinition definition, QuestDialogAction action, QuestDialogPage page) {
		assertPage(definition, action, page.id());
	}

	private static void assertPage(QuestDefinition definition, QuestDialogAction action, int pageId) {
		QuestTransition transition = talk(definition, "reward", "reward", action);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), transition.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target,
			QuestDialogAction action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(FONA, action.id()));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), "quest 13704 " + source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	/**
	 * 生产视图加载（P1 wave-2 起 13704 已退役）：返回真端合成定义。
	 * Loads through the production view (13704 retired in P1 wave-2): the retail-compiled definition.
	 */
	private static CompiledQuestDefinition definition() {
		return ProductionQuestDefinitions.definition(13704);
	}
}
