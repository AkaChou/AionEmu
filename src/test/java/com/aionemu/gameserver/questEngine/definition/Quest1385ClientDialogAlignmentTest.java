package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies quest 1385 keeps the client continuation action on the escort NPC. */
class Quest1385ClientDialogAlignmentTest {
	@Test
	void routesTheEscortContinuationToGriffo() {
		QuestDefinition definition = load().definition();

		QuestTransition continuation = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("started"))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 204029 && talk.dialogId() == QuestDialogAction.SELECT2_1.id())
			.findFirst().orElseThrow();

		assertEquals("started", continuation.targetNode());
		assertTrue(continuation.conditions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			continuation.afterCommit());

		assertTrue(definition.transitions().stream()
			.noneMatch(transition -> transition.sourceNode().equals("started")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 204028 && talk.dialogId() == QuestDialogAction.SELECT2_1.id()));
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/1385.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest1385ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
