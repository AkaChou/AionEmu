package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the sequential item-check contracts for quests 3082 and 2925.
 */
class SequentialItemCheckQuestFamilyTest {

	@Test
	void quest3082ChecksCollectedItemsBeforeDousingTheFlame() throws Exception {
		QuestDefinition definition = definition(3082);
		assertEquals(List.of(new QuestItemRequirement(182208060, 1)),
			definition.metadata().questWorkItems());
		assertNode(definition, "collect0", QuestStatus.START, 0);
		assertNode(definition, "collected", QuestStatus.START, 1);
		assertNode(definition, "ready", QuestStatus.START, 2);
		assertNode(definition, "reward", QuestStatus.REWARD, 2);

		assertPage(definition, "unaccepted", 798116,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
		assertPage(definition, "collect0", 204030,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertTalk(definition, "collect0", "collected", 204030,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
			List.of(
				new QuestCondition.HasItem(182208058, 5),
				new QuestCondition.HasItem(182208059, 5),
				new QuestCondition.HasItem(169400077, 1)),
			List.of(
				new QuestAction.GiveItem(182208060, 1),
				new QuestAction.RemoveItem(182208058, 5),
				new QuestAction.RemoveItem(182208059, 5),
				new QuestAction.RemoveItem(169400077, 1)),
			List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			0);
		assertTalk(definition, "collect0", "collect0", 204030,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.CHECK_USER_ITEM_FAIL.id())), 1);
		assertTalk(definition, "collect0", "collect0", 204030,
			QuestDialogAction.FINISH_DIALOG, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));

		assertPage(definition, "collected", 204030,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "collected", 204030,
			QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertTalk(definition, "collected", "ready", 204030,
			QuestDialogAction.SETPRO2, List.of(), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
		assertTalk(definition, "collected", "collected", 204030,
			QuestDialogAction.FINISH_DIALOG, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));

		QuestTransition douse = talk(definition, "ready", 700416,
			QuestDialogAction.USE_OBJECT, List.of());
		assertEquals("reward", douse.targetNode());
		assertEquals(List.of(new QuestAction.RemoveItem(182208060, QuestAction.RemoveItem.ALL)),
			douse.actions());
		assertTrue(douse.afterCommit().stream().anyMatch(action ->
			action instanceof AfterCommitAction.SpawnNpc spawn
				&& spawn.templateId() == 700417));
		assertEquals(List.of(
			douse.afterCommit().get(0),
			new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), douse.afterCommit());

		assertCompletion(definition, "reward", 798155,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1, 0, 2);
	}

	@Test
	void quest2925RunsTheLegacySixStageConfession() throws Exception {
		QuestDefinition definition = definition(2925);
		assertNode(definition, "talk0", QuestStatus.START, 0);
		assertNode(definition, "talk1", QuestStatus.START, 1);
		assertNode(definition, "talk2", QuestStatus.START, 2);
		assertNode(definition, "talk3", QuestStatus.START, 3);
		assertNode(definition, "talk4", QuestStatus.START, 4);
		assertNode(definition, "talk5", QuestStatus.START, 5);
		assertNode(definition, "reward", QuestStatus.REWARD, 5);

		assertPage(definition, "unaccepted", 204261,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
		assertTalk(definition, "talk0", "talk0", 204235,
			QuestDialogAction.QUEST_SELECT,
			List.of(new QuestCondition.EquippedItem(110100288, 1, true)), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())), 0);
		assertTalk(definition, "talk0", "talk0", 204235,
			QuestDialogAction.QUEST_SELECT, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_2.id())), 1);
		assertChain(definition, "talk0", "talk1", 204235,
			QuestDialogPage.SELECT1, QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1,
			QuestDialogAction.SETPRO1, List.of(),
			List.of(new QuestCondition.EquippedItem(110100288, 1, true)));

		assertChain(definition, "talk1", "talk2", 204261,
			QuestDialogPage.SELECT2, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1,
			QuestDialogAction.SETPRO2,
			List.of(new QuestAction.UnequipItem(110100288, 1)), List.of());

		assertChain(definition, "talk2", "talk3", 204127,
			QuestDialogPage.SELECT3, QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1,
			QuestDialogAction.SETPRO3, List.of(), List.of());
		assertChain(definition, "talk3", "talk4", 204193,
			QuestDialogPage.SELECT4, QuestDialogAction.SELECT4_1, QuestDialogPage.SELECT4_1,
			QuestDialogAction.SETPRO4, List.of(), List.of());
		assertChain(definition, "talk4", "talk5", 204235,
			QuestDialogPage.SELECT5, QuestDialogAction.SELECT5_1, QuestDialogPage.SELECT5_1,
			QuestDialogAction.SETPRO5, List.of(), List.of());

		assertPage(definition, "talk5", 204261,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT6);
		assertTalk(definition, "talk5", "reward", 204261,
			QuestDialogAction.SELECT6_1, List.of(), List.of(),
			List.of(
				new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())));
		assertCompletion(definition, "reward", 204261,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1, 0, 3);
	}

	private static void assertChain(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogPage firstPage, QuestDialogAction pageAction,
			QuestDialogPage secondPage,
			QuestDialogAction advanceAction, List<QuestAction> actions,
			List<QuestCondition> firstConditions) {
		assertTalk(definition, source, source, npcId,
			QuestDialogAction.QUEST_SELECT, firstConditions, List.of(),
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
		assertPage(definition, source, npcId,
			QuestDialogAction.SELECT_QUEST_REWARD, rewardPage);
		QuestTransition completion = talk(definition, source, npcId,
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
		try (InputStream input = SequentialItemCheckQuestFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
