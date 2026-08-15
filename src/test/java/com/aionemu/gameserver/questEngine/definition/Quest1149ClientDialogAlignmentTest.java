package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1149ClientDialogAlignmentTest {
	private static final int START_NPC = 203145;

	@Test
	void followsTheClientMultiPageAcceptDialog() throws Exception {
		List<QuestTransition> transitions = definition().definition().transitions();

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			talk(transitions, "unaccepted", QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id())),
			talk(transitions, "unaccepted", QuestDialogAction.SELECT1_1).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())),
			talk(transitions, "unaccepted", QuestDialogAction.ASK_QUEST_ACCEPT).afterCommit());

		QuestTransition accept = talk(transitions, "unaccepted", QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("started", accept.targetNode());
		assertTrue(accept.conditions().contains(new QuestCondition.StartEligible()));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())),
			accept.afterCommit());

		QuestTransition refuse = talk(transitions, "unaccepted", QuestDialogAction.QUEST_REFUSE_1);
		assertEquals("unaccepted", refuse.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_REFUSE_1.id())),
			refuse.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			talk(transitions, "unaccepted", QuestDialogAction.FINISH_DIALOG).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			talk(transitions, "started", QuestDialogAction.FINISH_DIALOG).afterCommit());

		assertFalse(transitions.stream().flatMap(transition -> transition.afterCommit().stream())
			.anyMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog page
				&& page.dialogId() == QuestDialogAction.ASK_QUEST_ACCEPT.id()));
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source, QuestDialogAction action) {
		return transitions.stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == START_NPC && Integer.valueOf(action.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1149.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1149.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
