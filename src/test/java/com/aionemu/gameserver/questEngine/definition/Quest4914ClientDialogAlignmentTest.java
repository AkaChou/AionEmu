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
 * 锁定任务 4914 的中间报告、关闭窗口和最终领奖 NPC owner。
 * Locks quest 4914's intermediate report, dialog close, and final reward-NPC owner.
 */
class Quest4914ClientDialogAlignmentTest {
	private static final int REPORT_NPC = 203385;
	private static final int REWARD_NPC = 204837;

	@Test
	void closesTheIntermediateReportBeforeTheFinalRewardOwner() throws Exception {
		QuestDefinition definition = definition().definition();

		assertPage(definition, "k1", REPORT_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
		QuestTransition report = route(definition, "k1", REPORT_NPC, QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), report.afterCommit());

		assertTrue(routes(definition, "reward", REPORT_NPC).isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			route(definition, "reward", REWARD_NPC, QuestDialogAction.USE_OBJECT).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			route(definition, "reward", REWARD_NPC, QuestDialogAction.SELECT_QUEST_REWARD).afterCommit());
		assertTrue(route(definition, "reward", REWARD_NPC, QuestDialogAction.SELECTED_QUEST_REWARD1)
			.actions().contains(new QuestAction.CompleteQuest(0)));
	}

	@Test
	void productionJourneyReachesTheFinalRewardOwnerBeforeCompleting() throws Exception {
		CompiledQuestDefinition definition = definition();
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));

		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest 4914 " + source + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return routes(definition, source, npcId).stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(action.id()).equals(talk.dialogId()))
			.toList();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Quest4914ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/4914.xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition 4914.xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
