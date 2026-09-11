package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the two reward branches and Kistig dialog chain for Silentera Canyon quest 30155.
 */
class StoneToFleshDialogBranchFamilyTest {

	private static final int QUEST_ID = 30155;
	private static final int NEP = 799234;
	private static final int KISTIG = 204433;
	private static final int VILI = 204304;
	private static final int MEDICINE = 182209252;

	@Test
	void quest30155RestoresTheKistigNepAndViliDialogBranches() throws Exception {
		QuestDefinition definition = definition(QUEST_ID);
		assertEquals(List.of(new QuestItemRequirement(MEDICINE, 1)),
			definition.metadata().questWorkItems());
		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		assertNode(definition, "started0", QuestStatus.START, 0);
		assertNode(definition, "started1", QuestStatus.START, 1);
		assertNode(definition, "started2", QuestStatus.START, 2);
		assertNode(definition, "started3", QuestStatus.START, 3);
		assertNode(definition, "reward2", QuestStatus.REWARD, 2);
		assertNode(definition, "reward3", QuestStatus.REWARD, 3);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);

		assertTalk(definition, "unaccepted", "started0", NEP,
			QuestDialogAction.QUEST_ACCEPT_1, List.of(new QuestCondition.StartEligible()),
			List.of(), List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())));

		assertPage(definition, "started0", KISTIG,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "started0", KISTIG,
			QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertPage(definition, "started0", KISTIG,
			QuestDialogAction.SELECT2_1_1, QuestDialogPage.SELECT2_1_1);
		assertTalk(definition, "started0", "started1", KISTIG,
			QuestDialogAction.SETPRO1, List.of(),
			List.of(new QuestAction.GiveItem(MEDICINE, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));

		assertPage(definition, "started1", NEP,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
		assertTalk(definition, "started1", "started2", NEP,
			QuestDialogAction.SETPRO2, List.of(), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())));
		assertTalk(definition, "started2", "reward2", NEP,
			QuestDialogAction.SELECT_QUEST_REWARD, List.of(),
			List.of(new QuestAction.RemoveItem(MEDICINE, 1)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2.id())));
		assertPage(definition, "reward2", NEP,
			QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);
		assertCompletion(definition, "reward2", NEP, QuestDialogAction.SELECTED_QUEST_NOREWARD,
			true);

		assertPage(definition, "started1", VILI,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT4);
		assertTalk(definition, "started1", "started1", VILI,
			QuestDialogAction.FINISH_DIALOG, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestSelectionDialog(
				QuestDialogPage.SELECT_QUEST.id())));
		assertTalk(definition, "started1", "started3", VILI,
			QuestDialogAction.SETPRO3, List.of(), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())));
		assertTalk(definition, "started3", "reward3", VILI,
			QuestDialogAction.SELECT_QUEST_REWARD, List.of(),
			List.of(new QuestAction.RemoveItem(MEDICINE, 1)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
		assertPage(definition, "reward3", VILI,
			QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertCompletion(definition, "reward3", VILI, QuestDialogAction.SELECTED_QUEST_REWARD1,
			false);
	}

	private static void assertCompletion(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, boolean closesDialog) {
		QuestTransition completion = talk(definition, source, npcId, action, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().stream().anyMatch(QuestAction.GrantReward.class::isInstance));
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			closesDialog
				? new AfterCommitAction.CloseDialog()
				: new AfterCommitAction.ShowQuestSelectionDialog(
					QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, npcId, action, List.of());
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			transition.afterCommit());
	}

	private static void assertTalk(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(target, transition.targetNode());
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
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
		try (InputStream input = StoneToFleshDialogBranchFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
