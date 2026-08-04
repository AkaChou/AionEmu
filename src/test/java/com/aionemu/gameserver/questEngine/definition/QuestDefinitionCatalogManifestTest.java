package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDefinitionCatalogManifestTest {
	@Test
	void packagedProductionQuestsCompileFromDirectory() throws Exception {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compileFromQuestsDirectory(getClass().getClassLoader());
		// Every quests/*.xml compiles and the ids are positive and unique (ImmutableQuestCatalog enforces uniqueness).
		assertFalse(catalog.all().isEmpty());
		assertTrue(catalog.all().stream().map(CompiledQuestDefinition::id).allMatch(id -> id > 0));
	}

	@Test
	void emptyDuplicateAndMigrationAnnotatedCatalogsFailClosed() {
		assertEquals("INVALID_PRODUCTION_CATALOG", error("<quest-definition-catalog version=\"1\"/>").code());
		assertEquals("DUPLICATE_CATALOG_OWNER", error("<quest-definition-catalog version=\"1\">"
			+ "<definition id=\"1\" resource=\"one.xml\"/>"
			+ "<definition id=\"1\" resource=\"two.xml\"/>"
			+ "</quest-definition-catalog>").code());
		assertEquals("INVALID_PRODUCTION_CATALOG", error("<quest-definition-catalog version=\"1\" "
			+ "ownership=\"CURRENT\"><definition id=\"1\" resource=\"one.xml\"/>"
			+ "</quest-definition-catalog>").code());
	}

	@Test
	void missingResourceAndDefinitionIdMismatchFailClosed() {
		String missing = "<quest-definition-catalog version=\"1\">"
			+ "<definition id=\"1\" resource=\"missing.xml\"/></quest-definition-catalog>";
		assertEquals("CATALOG_RESOURCE_MISSING", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCatalogManifest.compile(bytes(missing), getClass().getClassLoader())).code());

		String mismatch = "<quest-definition-catalog version=\"1\">"
			+ "<definition id=\"2\" resource=\"quest-definition-fixtures/one.xml\"/>"
			+ "</quest-definition-catalog>";
		assertEquals("CATALOG_ID_MISMATCH", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCatalogManifest.compile(bytes(mismatch), getClass().getClassLoader())).code());
	}

	private static QuestCompilationException error(String xml) {
		return assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCatalogManifest.load(bytes(xml)));
	}

	private static ByteArrayInputStream bytes(String xml) {
		return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
	}
}
