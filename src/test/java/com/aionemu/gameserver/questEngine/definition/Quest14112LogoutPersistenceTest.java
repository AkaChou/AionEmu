package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest14112LogoutPersistenceTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/14112.xml");

	@Test
	void keepsPoisonousBubblegutKillProgressAcrossLogout() throws Exception {
		var definition = load().definition();

		QuestTransition kill = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc event
				&& event.npcId() == 210318)
			.findFirst().orElseThrow();
		assertEquals("started", kill.sourceNode());
		assertEquals("k1", kill.targetNode());

		assertFalse(definition.transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("k1")
				&& transition.event() instanceof QuestEvent.LogOut));
		assertTrue(definition.transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("k1")
				&& transition.targetNode().equals("reward")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203195));
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
