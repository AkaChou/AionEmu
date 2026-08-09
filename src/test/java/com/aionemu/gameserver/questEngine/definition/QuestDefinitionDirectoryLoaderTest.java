package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDefinitionDirectoryLoaderTest {
	@Test
	void packagedProductionQuestsCompileFromDirectory() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		// Every quests/<id>.xml compiles and duplicate owners fail in ImmutableQuestCatalog.
		assertFalse(catalog.executables().isEmpty());
		assertTrue(catalog.executables().stream().map(CompiledQuestDefinition::id).allMatch(id -> id > 0));
		assertEquals(QuestCatalogEntryMode.METADATA_ONLY, catalog.findEntry(50032).orElseThrow().mode());
		assertTrue(catalog.findExecutable(50032).isEmpty());
	}
}
