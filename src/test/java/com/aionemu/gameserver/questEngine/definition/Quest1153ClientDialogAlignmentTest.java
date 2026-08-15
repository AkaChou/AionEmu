package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1153ClientDialogAlignmentTest {
	private static final int HIANU = 203150;
	private static final int SOUP = 182200527;

	@Test
	void deliversTheSoupToHianuThroughTheClientRewardPage() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestTransition> transitions = compiled.definition().transitions();

		assertEquals(1102323, compiled.definition().metadata().displayNameId());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
			talk(transitions, "started", QuestDialogAction.QUEST_SELECT).afterCommit());

		QuestTransition delivery = talk(transitions, "started", QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", delivery.targetNode());
		assertEquals(List.of(new QuestCondition.HasItem(SOUP, 1)), delivery.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(SOUP, 1)), delivery.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			delivery.afterCommit());

		QuestTransition completion = talk(transitions, "reward", QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source,
			QuestDialogAction action) {
		return transitions.stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == HIANU && Integer.valueOf(action.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1153.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1153.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
