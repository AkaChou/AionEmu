package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest30310RetailAlignmentTest {
	@Test
	void dropsMatchRetailQuestData() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/30310.xml");
		try (InputStream input = Files.newInputStream(path)) {
			QuestMetadata metadata = QuestDefinitionXmlCompiler.compile(input).definition().metadata();
			Set<QuestDrop> expected = Set.of(
				new QuestDrop(216003, 182209713, 70, false, 0),
				new QuestDrop(216004, 182209713, 70, false, 0),
				new QuestDrop(215923, 182209713, 70, false, 0),
				new QuestDrop(215924, 182209713, 70, false, 0)
			);
			assertEquals(expected, Set.copyOf(metadata.drops()));
		}
	}
}
