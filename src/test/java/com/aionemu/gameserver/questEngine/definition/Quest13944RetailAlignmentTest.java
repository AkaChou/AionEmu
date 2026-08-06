package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Retail-anchored structural coverage for the Union fortress hunt owner 13944. */
class Quest13944RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/13944.xml");

	@Test
	void preservesUnionMetadataThreeKillsAndDianTurnIn() throws Exception {
		QuestDefinition definition;
		try (InputStream input = Files.newInputStream(XML)) {
			definition = QuestDefinitionXmlCompiler.compile(input).definition();
		}
		QuestMetadata metadata = definition.metadata();
		assertEquals("[Union] Eliminate the Divine Fortress Commander", metadata.name());
		assertEquals(1803577, metadata.displayNameId());
		assertEquals(66, metadata.minLevel());
		assertEquals("SEEN_MARKER", metadata.category());
		assertEquals(Set.of("ELYOS"), metadata.permittedRaces());
		assertEquals(List.of(new QuestReward("EXP", 0, 93626245), new QuestReward("ITEM", 188058110, 1)),
			metadata.rewards());
		assertEquals(List.of(new QuestKill(1, List.of(884940, 884986))), metadata.kills());

		List<QuestTransition> transitions = definition.transitions();
		assertEquals(3, transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc kill
				&& kill.npcId() == 884940)
			.count());
		assertEquals(3, transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc kill
				&& kill.npcId() == 884986)
			.count());
		assertEquals(Set.of(835722), transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 1009)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(java.util.stream.Collectors.toSet()));
	}
}
