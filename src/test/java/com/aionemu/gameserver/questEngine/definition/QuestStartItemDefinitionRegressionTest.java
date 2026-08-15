package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

class QuestStartItemDefinitionRegressionTest {

	@Test
	void startItemNpcsExposeTheDialogRoutesUsedAfterReading() throws Exception {
		for (ExpectedRoute expected : List.of(
			new ExpectedRoute(1197, 700004, "unaccepted", QuestDialogAction.USE_OBJECT.id()),
			new ExpectedRoute(1198, 700009, "unaccepted", QuestDialogAction.USE_OBJECT.id()),
			new ExpectedRoute(1323, 730032, "unaccepted", QuestDialogAction.USE_OBJECT.id()),
			new ExpectedRoute(1559, 700513, "started", QuestDialogAction.USE_OBJECT.id()),
			new ExpectedRoute(1582, 700196, "unaccepted", QuestDialogAction.QUEST_SELECT.id()))) {
			CompiledQuestDefinition definition = definition(expected.questId());
			assertTrue(definition.definition().transitions().stream()
				.filter(transition -> expected.source().equals(transition.sourceNode()))
				.anyMatch(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == expected.npcId() && talk.dialogId() == expected.dialogId()),
				"quest " + expected.questId() + " NPC " + expected.npcId());
		}
	}

	private CompiledQuestDefinition definition(int questId) throws Exception {
		String path = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = getClass().getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record ExpectedRoute(int questId, int npcId, String source, int dialogId) {
	}
}
