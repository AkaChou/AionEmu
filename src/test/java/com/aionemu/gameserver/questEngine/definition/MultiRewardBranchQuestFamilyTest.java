package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the legacy multi-reward branch contracts for quests 2345, 2123, and 21075.
 */
class MultiRewardBranchQuestFamilyTest {

	@Test
	void quest2345KeepsBothOrderTypesAndRewardGroups() throws Exception {
		QuestDefinition definition = definition(2345);
		assertEquals(2, definition.metadata().rewardGroups().size());
		assertNode(definition, "collect0", QuestStatus.START, 0);
		assertNode(definition, "choice", QuestStatus.START, 1);
		assertNode(definition, "reward0", QuestStatus.REWARD, 10);
		assertNode(definition, "reward1", QuestStatus.REWARD, 20);

		assertPage(definition, "collect0", 798084,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertTalk(definition, "collect0", "choice", 798084,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
			List.of(new QuestCondition.HasItem(182204136, 3)),
			List.of(new QuestAction.RemoveItem(182204136, 3)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), 0);
		assertTalk(definition, "collect0", "collect0", 798084,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.CHECK_USER_ITEM_FAIL.id())), 1);

		assertPage(definition, "choice", 798084,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "choice", 798084,
			QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertPage(definition, "choice", 798084,
			QuestDialogAction.SELECT2_2, QuestDialogPage.SELECT2_2);
		assertTalk(definition, "choice", "reward0", 798084,
			QuestDialogAction.SETPRO10, List.of(),
			List.of(new QuestAction.GiveItem(182204137, 1)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));
		assertTalk(definition, "choice", "reward1", 798084,
			QuestDialogAction.SETPRO20, List.of(),
			List.of(new QuestAction.GiveItem(182204138, 1)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));

		assertNpcReward(definition, "reward0", 204304, 182204137,
			QuestDialogPage.SELECT3, QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1, 0);
		assertNpcReward(definition, "reward1", 204304, 182204138,
			QuestDialogPage.SELECT4, QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2, 1);
	}

	@Test
	void quest2123KeepsThreeOfferingsAndRewardGroups() throws Exception {
		QuestDefinition definition = definition(2123);
		assertEquals(3, definition.metadata().rewardGroups().size());
		assertNode(definition, "started", QuestStatus.START, 0);
		assertNode(definition, "reward0", QuestStatus.REWARD, 1);
		assertNode(definition, "reward1", QuestStatus.REWARD, 2);
		assertNode(definition, "reward2", QuestStatus.REWARD, 3);

		assertPage(definition, "unaccepted", 203550,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertPage(definition, "unaccepted", 203550,
			QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1);
		assertPage(definition, "unaccepted", 203550,
			QuestDialogAction.SELECT1_1_1, QuestDialogPage.SELECT1_1_1);
		assertPage(definition, "started", 203550,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);

		assertOffering(definition, QuestDialogAction.SETPRO1, 182203121,
			"reward0", QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertOffering(definition, QuestDialogAction.SETPRO2, 182203122,
			"reward1", QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);
		assertOffering(definition, QuestDialogAction.SETPRO3, 182203123,
			"reward2", QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW3);
		for (QuestDialogAction action : List.of(QuestDialogAction.SETPRO1,
				QuestDialogAction.SETPRO2, QuestDialogAction.SETPRO3)) {
			assertTalk(definition, "started", "started", 203550, action, List.of(), List.of(),
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())), 1);
		}

		assertCompletion(definition, "reward0", 203550,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1, 0, false);
		assertCompletion(definition, "reward1", 203550,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2, 1, false);
		assertCompletion(definition, "reward2", 203550,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW3, 2, false);
	}

	@Test
	void quest21075KeepsBothTrustBranchesAndRewardGroups() throws Exception {
		QuestDefinition definition = definition(21075);
		assertEquals(2, definition.metadata().rewardGroups().size());
		assertNode(definition, "started0", QuestStatus.START, 0);
		assertNode(definition, "branchMisorin", QuestStatus.START, 1);
		assertNode(definition, "branchLusena", QuestStatus.START, 2);
		assertNode(definition, "rewardMisorin", QuestStatus.REWARD, 1);
		assertNode(definition, "rewardLusena", QuestStatus.REWARD, 2);

		assertPage(definition, "unaccepted", 799409,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
		assertPage(definition, "started0", 798392,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertPage(definition, "started0", 798392,
			QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1);
		assertTalk(definition, "started0", "branchMisorin", 798392,
			QuestDialogAction.SETPRO1, List.of(),
			List.of(new QuestAction.GiveItem(182207917, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
		assertTalk(definition, "started0", "branchLusena", 798392,
			QuestDialogAction.SETPRO2, List.of(),
			List.of(new QuestAction.GiveItem(182207917, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));

		assertPage(definition, "branchMisorin", 799410,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "branchMisorin", 799410,
			QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertTalk(definition, "branchMisorin", "rewardMisorin", 799410,
			QuestDialogAction.SET_SUCCEED, List.of(),
			List.of(new QuestAction.RemoveItem(182207917, QuestAction.RemoveItem.ALL)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));

		assertPage(definition, "branchLusena", 204138,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
		assertPage(definition, "branchLusena", 204138,
			QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
		assertTalk(definition, "branchLusena", "rewardLusena", 204138,
			QuestDialogAction.SET_SUCCEED, List.of(),
			List.of(new QuestAction.RemoveItem(182207917, QuestAction.RemoveItem.ALL)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));

		assertCompletion(definition, "rewardMisorin", 799409,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1, 0, true);
		assertCompletion(definition, "rewardLusena", 799409,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2, 1, true);
	}

	private static void assertNpcReward(QuestDefinition definition, String source, int npcId,
			int workItem, QuestDialogPage page, QuestDialogPage rewardPage, int rewardGroup) {
		assertTalk(definition, source, source, npcId,
			QuestDialogAction.QUEST_SELECT, List.of(),
			List.of(new QuestAction.RemoveItem(workItem, QuestAction.RemoveItem.ALL)),
			List.of(new AfterCommitAction.ShowQuestDialog(page.id())));
		assertCompletion(definition, source, npcId, rewardPage, rewardGroup, false);
	}

	private static void assertOffering(QuestDefinition definition, QuestDialogAction action,
			int itemId, String target, QuestDialogPage rewardPage) {
		QuestTransition transition = talk(definition, "started", 203550, action,
			List.of(new QuestCondition.HasItem(itemId, 1)));
		assertEquals(target, transition.targetNode());
		assertEquals(List.of(
			new QuestAction.RemoveItem(182203121, QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(182203122, QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(182203123, QuestAction.RemoveItem.ALL)),
			transition.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(rewardPage.id())), transition.afterCommit());
	}

	private static void assertCompletion(QuestDefinition definition, String source, int npcId,
			QuestDialogPage rewardPage, int rewardGroup, boolean selectableChoices) {
		assertPage(definition, source, npcId,
			QuestDialogAction.SELECT_QUEST_REWARD, rewardPage);
		QuestTransition completion = talk(definition, source, npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(rewardGroup)));
		long grantedRewards = completion.actions().stream()
			.filter(QuestAction.GrantReward.class::isInstance)
			.count();
		if (selectableChoices) {
			assertEquals(3, grantedRewards);
		} else {
			assertTrue(grantedRewards > 0);
		}
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertTalk(definition, source, source, npcId, action, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(page.id())), null);
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
		try (InputStream input = MultiRewardBranchQuestFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
