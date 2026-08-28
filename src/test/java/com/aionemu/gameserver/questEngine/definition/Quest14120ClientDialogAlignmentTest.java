package com.aionemu.gameserver.questEngine.definition;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 14120 的 Aion 5.8 客户端交付与奖励页面链。
 * Locks quest 14120's Aion 5.8 client turn-in and reward-page flow.
 */
class Quest14120ClientDialogAlignmentTest {
	private static final int START_NPC = 203932;
	private static final int HANDOFF_NPC = 730020;
	private static final int REWARD_NPC = 730019;
	private static final int QUEST_ITEM = 182215478;

	@Test
	void followsTheHandoffAndReopensTheRewardWindowAtTheFinalNpc() throws Exception {
		QuestDefinition definition = load();

		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1);
		assertPage(definition, "started", HANDOFF_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "started", HANDOFF_NPC, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);

		QuestTransition handoff = route(definition, "started", HANDOFF_NPC, QuestDialogAction.SETPRO1);
		assertEquals("v1", handoff.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), handoff.afterCommit());

		QuestTransition report = route(definition, "v1", REWARD_NPC,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(new QuestCondition.HasItem(QUEST_ITEM, 1)), report.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(QUEST_ITEM, 1)), report.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), report.afterCommit());

		QuestTransition rewardPreview = route(definition, "reward", REWARD_NPC, QuestDialogAction.USE_OBJECT);
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 1)), node(definition, "reward").projection());
		assertEquals("reward", rewardPreview.targetNode());
		assertEquals(List.of(), rewardPreview.conditions());
		assertEquals(List.of(), rewardPreview.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), rewardPreview.afterCommit());

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			route(definition, "reward", REWARD_NPC, QuestDialogAction.SELECT_QUEST_REWARD).afterCommit());
		QuestTransition completion = route(definition, "reward", REWARD_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), completion.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == action.id())
			.toList();
		assertEquals(1, routes.size(), "quest 14120 " + source + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> label.equals(node.label()))
			.findFirst().orElseThrow();
	}

	private static QuestDefinition load() throws Exception {
		try (InputStream input = Quest14120ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/14120.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 14120.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
