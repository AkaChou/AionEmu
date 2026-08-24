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
 * 锁定任务 25670 的四物品报告、确认页和唯一奖励归属。
 * Locks quest 25670's four-item report, confirmation pages, and unique reward owner.
 */
class Quest25670ClientDialogAlignmentTest {
	private static final int START_NPC = 806116;
	private static final int REPORT_NPC = 806105;
	private static final int CONFIRM_OBJECT = 731794;
	private static final List<Integer> ITEMS = List.of(182216194, 182216195, 182216196, 182216197);

	@Test
	void followsTheRetailReportAndRewardOwnerChain() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(4, definition.metadata().drops().size());
		assertTrue(definition.metadata().drops().stream().allMatch(drop ->
			ITEMS.contains(drop.itemId()) && drop.chance() == 100 && drop.collectingStep() == 1));

		QuestTransition handoff = route(definition, "s0", "s1", REPORT_NPC, QuestDialogAction.SETPRO1);
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), handoff.afterCommit());

		QuestTransition report = route(definition, "s1", "s2", REPORT_NPC,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM);
		assertEquals(ITEMS.stream().map(item -> new QuestAction.RemoveItem(item, 1)).toList(), report.actions());
		assertEquals(List.of(
			new QuestCondition.HasItem(182216194, 1),
			new QuestCondition.HasItem(182216195, 1),
			new QuestCondition.HasItem(182216196, 1),
			new QuestCondition.HasItem(182216197, 1)), report.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), report.afterCommit());

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())),
			route(definition, "s2", "s2", CONFIRM_OBJECT, QuestDialogAction.USE_OBJECT).afterCommit());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()),
			route(definition, "s2", "reward", CONFIRM_OBJECT, QuestDialogAction.SET_SUCCEED).afterCommit());

		assertTrue(routes(definition, "unaccepted", REPORT_NPC).isEmpty());
		assertTrue(routes(definition, "reward", REPORT_NPC).isEmpty());
		QuestTransition preview = route(definition, "reward", "reward", START_NPC, QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		QuestTransition completion = route(definition, "reward", "complete", START_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 164700, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 15469350, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0)), completion.actions());
	}

	@Test
	void productionJourneyCompletesFromTheProductionOwner() throws Exception {
		CompiledQuestDefinition definition = definition();
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));
		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId).stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(action.id()).equals(talk.dialogId()))
			.filter(transition -> target.equals(transition.targetNode()))
			.toList();
		assertEquals(1, routes.size(), source + " -> " + target + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Quest25670ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/25670.xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition 25670.xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
