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

		assertPage(definition, QuestDialogAction.USE_OBJECT, QuestDialogPage.SELECT5);
		QuestTransition report = talk(definition, "reward", "reward", QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(new QuestAction.RemoveItem(INTRUSION_MINE, 1)), report.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), report.afterCommit());

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
		QuestTransition transition = talk(definition, "reward", "reward", action);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
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

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Quest13704ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/13704.xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition 13704.xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
