package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1913ClientDialogAlignmentTest {
	private static final int START_NPC = 203758;
	private static final int PROGRESS_NPC = 203726;
	private static final int REPORT_NPC = 203097;

	@Test
	void followsTheClientAcceptProgressReportAndRewardLifecycle() throws Exception {
		List<QuestTransition> transitions = definition().definition().transitions();

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			talk(transitions, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals("started",
			talk(transitions, "unaccepted", START_NPC, QuestDialogAction.QUEST_ACCEPT_1).targetNode());
		assertEquals("unaccepted",
			talk(transitions, "unaccepted", START_NPC, QuestDialogAction.QUEST_REFUSE_1).targetNode());

		QuestTransition progress = talk(transitions, "started", PROGRESS_NPC, QuestDialogAction.SETPRO1);
		assertEquals("started1", progress.targetNode());
		assertTrue(progress.actions().contains(new QuestAction.SetVariable("var0", 1)));
		assertEquals(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			progress.afterCommit().getFirst());
		AfterCommitAction.TeleportPlayer teleport = assertInstanceOf(AfterCommitAction.TeleportPlayer.class,
			progress.afterCommit().get(1));
		assertEquals(210030000, teleport.worldId());
		assertEquals(new AfterCommitAction.CloseDialog(), progress.afterCommit().getLast());

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
			talk(transitions, "started1", REPORT_NPC, QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals("reward",
			talk(transitions, "started1", REPORT_NPC, QuestDialogAction.SELECT_QUEST_REWARD).targetNode());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			talk(transitions, "started1", REPORT_NPC, QuestDialogAction.SELECT_QUEST_REWARD).afterCommit());

		QuestTransition completion = talk(transitions, "reward", REPORT_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source, int npcId,
			QuestDialogAction action) {
		return transitions.stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(action.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1913.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1913.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
