package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDefinitionDirectoryLoaderTest {
	@Test
	void packagedProductionQuestsCompileFromDirectory() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		// Every quests/<id>.xml compiles and duplicate owners fail in ImmutableQuestCatalog.
		assertFalse(catalog.all().isEmpty());
		assertTrue(catalog.all().stream().map(CompiledQuestDefinition::id).allMatch(id -> id > 0));
	}
}
