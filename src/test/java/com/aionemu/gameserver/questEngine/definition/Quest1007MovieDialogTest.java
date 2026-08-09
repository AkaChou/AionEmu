package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Quest1007MovieDialogTest {
	@Test
	void movieSelectionsRestoreTheContinueDialogAfterPlaybackStarts() throws Exception {
		QuestDefinition definition;
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1007.xml")) {
			assertNotNull(input);
			definition = QuestDefinitionXmlCompiler.compile(input).definition();
		}

		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(92),
			new AfterCommitAction.ShowQuestDialog(1353)),
			afterCommit(definition, 203725, 1353));
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(91),
			new AfterCommitAction.ShowQuestDialog(1694)),
			afterCommit(definition, 203752, 1694));
	}

	private static List<AfterCommitAction> afterCommit(QuestDefinition definition, int npcId, int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(dialogId).equals(talk.dialogId()))
			.findFirst().orElseThrow().afterCommit();
	}
}
