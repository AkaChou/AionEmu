package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the Pandaemonium sequential contracts for quests 2911 and 2919.
 */
class PandaemoniumSequentialQuestFamilyTest {

	@Test
	void quest2911KeepsBothBlessingRewardGroups() throws Exception {
		QuestDefinition definition = definition(2911);
		assertEquals(2, definition.metadata().rewardGroups().size());
		assertNode(definition, "started", QuestStatus.START, 0);
		assertNode(definition, "reward0", QuestStatus.REWARD, 1);
		assertNode(definition, "reward1", QuestStatus.REWARD, 2);

		assertPage(definition, "unaccepted", 204079,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertTalk(definition, "unaccepted", "started", 204079,
			QuestDialogAction.QUEST_ACCEPT_1,
			List.of(new QuestCondition.StartEligible()), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())));

		assertPage(definition, "started", 204193,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "started", 204193,
			QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertRewardChoice(definition, "started", "reward0",
			QuestDialogAction.SETPRO1, QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertRewardChoice(definition, "started", "reward1",
			QuestDialogAction.SETPRO2, QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);

		assertCompletion(definition, "reward0", 204193,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1, 0, 1);
		assertCompletion(definition, "reward1", 204193,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2, 1, 2);
	}

	@Test
	void quest2919RunsTheBookOfOblivionChain() throws Exception {
		QuestDefinition definition = definition(2919);
		assertNode(definition, "start0", QuestStatus.START, 0);
		assertNode(definition, "talk1", QuestStatus.START, 1);
		assertNode(definition, "talk2", QuestStatus.START, 2);
		assertNode(definition, "bookReady", QuestStatus.START, 3);
		assertNode(definition, "talk3", QuestStatus.START, 4);
		assertNode(definition, "collected", QuestStatus.START, 6);
		assertNode(definition, "bookInserted", QuestStatus.START, 7);
		assertNode(definition, "reward", QuestStatus.REWARD, 7);

		assertPage(definition, "unaccepted", 204206,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertPage(definition, "unaccepted", 204206,
			QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1);
		assertChain(definition, "start0", "talk1", 204215,
			QuestDialogPage.SELECT2, QuestDialogAction.SELECT2_1,
			QuestDialogPage.SELECT2_1, QuestDialogAction.SETPRO2, List.of());
		assertChain(definition, "talk1", "talk2", 204192,
			QuestDialogPage.SELECT3, QuestDialogAction.SELECT3_1,
			QuestDialogPage.SELECT3_1, QuestDialogAction.SETPRO3, List.of());

		assertTalk(definition, "talk2", "talk2", 700212,
			QuestDialogAction.USE_OBJECT, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())));
		assertTalk(definition, "talk2", "bookReady", 700212,
			QuestDialogAction.SETPRO4, List.of(), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));

		assertTalk(definition, "bookReady", "bookReady", 204206,
			QuestDialogAction.QUEST_SELECT, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())));
		assertTalk(definition, "bookReady", "bookReady", 204206,
			QuestDialogAction.SELECT5_1, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5_1.id())));
		assertTalk(definition, "bookReady", "bookReady", 204206,
			QuestDialogAction.SELECT5_1_1, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5_1_1.id())));
		assertTalk(definition, "bookReady", "talk3", 204206,
			QuestDialogAction.SETPRO5, List.of(), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));

		assertTalk(definition, "talk3", "talk3", 204224,
			QuestDialogAction.QUEST_SELECT, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())));
		assertTalk(definition, "talk3", "collected", 204224,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
			List.of(
				new QuestCondition.HasItem(182207010, 3),
				new QuestCondition.HasItem(182207011, 2),
				new QuestCondition.HasItem(182207012, 1)),
			List.of(
				new QuestAction.RemoveItem(182207010, 3),
				new QuestAction.RemoveItem(182207011, 2),
				new QuestAction.RemoveItem(182207012, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6_2.id())),
			0);
		assertTalk(definition, "talk3", "talk3", 204224,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6_1.id())), 1);
		assertTalk(definition, "collected", "collected", 204224,
			QuestDialogAction.SETPRO6, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));

		assertTalk(definition, "collected", "collected", 700212,
			QuestDialogAction.USE_OBJECT, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT7.id())));
		assertTalk(definition, "collected", "bookInserted", 700212,
			QuestDialogAction.SETPRO7, List.of(),
			List.of(new QuestAction.GiveItem(182207013, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));

		assertTalk(definition, "bookInserted", "bookInserted", 204206,
			QuestDialogAction.USE_OBJECT, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT8.id())));
		assertCompletion(definition, "bookInserted", 204206,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1, 0, 2);
	}

	private static void assertRewardChoice(QuestDefinition definition, String source, String target,
			QuestDialogAction action, QuestDialogPage rewardPage) {
		QuestTransition transition = talk(definition, source, 204193, action, List.of());
		assertEquals(target, transition.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(rewardPage.id())), transition.afterCommit());
	}

	private static void assertChain(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogPage firstPage, QuestDialogAction pageAction,
			QuestDialogPage secondPage, QuestDialogAction advanceAction,
			List<QuestAction> actions) {
		assertTalk(definition, source, source, npcId,
			QuestDialogAction.QUEST_SELECT, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(firstPage.id())));
		assertTalk(definition, source, source, npcId,
			pageAction, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(secondPage.id())));
		assertTalk(definition, source, target, npcId,
			advanceAction, List.of(), actions,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
	}

	private static void assertCompletion(QuestDefinition definition, String source, int npcId,
			QuestDialogPage rewardPage, int rewardGroup, int expectedGrantCount) {
		String completionSource = source;
		if ("bookInserted".equals(source)) {
			QuestTransition openReward = talk(definition, source, npcId,
				QuestDialogAction.SELECT_QUEST_REWARD, List.of());
			assertEquals("reward", openReward.targetNode());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(rewardPage.id())),
				openReward.afterCommit());
			completionSource = "reward";
		} else {
			assertPage(definition, source, npcId,
				QuestDialogAction.SELECT_QUEST_REWARD, rewardPage);
		}
		QuestTransition completion = talk(definition, completionSource, npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(rewardGroup)));
		assertEquals(expectedGrantCount, completion.actions().stream()
			.filter(QuestAction.GrantReward.class::isInstance)
			.count());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertTalk(definition, source, source, npcId, action, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(page.id())));
	}

	private static void assertTalk(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		assertTalk(definition, source, target, npcId, action, conditions, actions, afterCommit,
			null);
	}

	private static void assertTalk(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit, Integer priority) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(target, transition.targetNode());
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
		if (priority != null) {
			assertEquals(priority, transition.priority());
		}
	}

	private static QuestTransition talk(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, List<QuestCondition> conditions) {
		QuestEvent.TalkToNpc event = new QuestEvent.TalkToNpc(npcId, action.id());
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> candidate.conditions().equals(conditions))
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				"missing route " + source + " + NPC " + npcId + " + action " + action.id()));
	}

	private static void assertNode(QuestDefinition definition, String label,
			QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = PandaemoniumSequentialQuestFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
