package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 3100 的工作物品交接、唯一领奖 NPC 与 Aion 5.8 客户端页面链。
 * Verifies quest 3100's work-item handoff, sole reward NPC, and Aion 5.8 client page chain.
 */
class Quest3100ClientDialogAlignmentTest {
	private static final int START_NPC_ID = 203792;
	private static final int HANDOFF_NPC_ID = 798168;
	private static final int REWARD_NPC_ID = 798169;
	private static final int WORK_ITEM_ID = 182208072;
	private static final long EXP_REWARD = 6937236;
	private static final int FIXED_ITEM_REWARD_ID = 162000066;
	private static final List<Integer> SELECTABLE_REWARD_IDS = List.of(
		114101118, 114301128, 114501077, 114601031, 114501565);
	private static final List<QuestDialogAction> SELECTABLE_REWARD_ACTIONS = List.of(
		QuestDialogAction.SELECTED_QUEST_REWARD1,
		QuestDialogAction.SELECTED_QUEST_REWARD2,
		QuestDialogAction.SELECTED_QUEST_REWARD3,
		QuestDialogAction.SELECTED_QUEST_REWARD4,
		QuestDialogAction.SELECTED_QUEST_REWARD5);

	@Test
	void followsTheRetailWorkItemHandoffAndSoleRewardOwner() throws Exception {
		QuestDefinition definition = definition().definition();

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
		assertEquals(List.of(new QuestItemRequirement(WORK_ITEM_ID, 1)), definition.metadata().questWorkItems());
		assertTrue(routes(definition, "unaccepted", HANDOFF_NPC_ID).isEmpty());
		assertTrue(routes(definition, "unaccepted", REWARD_NPC_ID).isEmpty());

		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_ACCEPT_1,
				QuestDialogAction.QUEST_ACCEPT_SIMPLE)) {
			QuestTransition accept = route(definition, "unaccepted", START_NPC_ID, action);
			assertEquals("started", accept.targetNode());
			assertEquals(List.of(new QuestAction.GiveItem(WORK_ITEM_ID, 1)), accept.actions());
			assertEquals(action == QuestDialogAction.QUEST_ACCEPT_1
					? List.of(
						new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
						new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id()))
					: List.of(
						new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
						new AfterCommitAction.CloseDialog()), accept.afterCommit());
		}

		assertPage(definition, "started", HANDOFF_NPC_ID, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "started", HANDOFF_NPC_ID, QuestDialogAction.SELECT2_1,
			QuestDialogPage.SELECT2_1);
		assertTrue(routes(definition, "started", START_NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty());
		assertTrue(routes(definition, "started", REWARD_NPC_ID, QuestDialogAction.QUEST_SELECT).isEmpty());

		QuestTransition handoff = route(definition, "started", HANDOFF_NPC_ID, QuestDialogAction.SETPRO1);
		assertEquals("reward", handoff.targetNode());
		assertEquals(List.of(), handoff.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(WORK_ITEM_ID, 1)), handoff.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), handoff.afterCommit());

		assertPage(definition, "reward", REWARD_NPC_ID, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
		for (QuestDialogAction action : List.of(QuestDialogAction.USE_OBJECT,
				QuestDialogAction.SELECT_QUEST_REWARD)) {
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				route(definition, "reward", REWARD_NPC_ID, action).afterCommit());
		}
		assertTrue(routes(definition, "reward", START_NPC_ID).isEmpty());
		assertTrue(routes(definition, "reward", HANDOFF_NPC_ID).isEmpty());

		for (int index = 0; index < SELECTABLE_REWARD_IDS.size(); index++) {
			assertCompletion(definition, SELECTABLE_REWARD_ACTIONS.get(index),
				SELECTABLE_REWARD_IDS.get(index));
		}
	}

	@Test
	void completesThroughTheProductionHeadlessJourney() throws Exception {
		CompiledQuestDefinition definition = definition();
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));

		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	private static void assertCompletion(QuestDefinition definition, QuestDialogAction action,
			int selectableRewardId) {
		QuestTransition completion = route(definition, "reward", REWARD_NPC_ID, action);
		assertEquals("complete", completion.targetNode());
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, EXP_REWARD, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", FIXED_ITEM_REWARD_ID, 5),
			new QuestAction.GrantReward("ITEM", selectableRewardId, 1),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())), completion.afterCommit());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest 3100 " + source + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return routes(definition, source, npcId).stream()
			.filter(transition -> Integer.valueOf(action.id()).equals(
				((QuestEvent.TalkToNpc) transition.event()).dialogId()))
			.toList();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Quest3100ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/3100.xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition 3100.xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
