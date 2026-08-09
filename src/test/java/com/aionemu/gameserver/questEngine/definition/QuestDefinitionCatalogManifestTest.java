package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDefinitionCatalogManifestTest {
	@Test
	void packagedProductionCatalogCompiles() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quest_definition_catalog.xml")) {
			QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(input, getClass().getClassLoader());
			assertFalse(catalog.executables().isEmpty());
			assertTrue(catalog.entries().size() > catalog.executables().size());
			assertTrue(catalog.entries().stream().allMatch(entry -> catalog.findMetadata(entry.id()).isPresent()));
			assertTrue(catalog.findExecutable(11036).orElseThrow().definition().transitions().stream()
				.anyMatch(transition -> transition.event() instanceof QuestEvent.CanAct canAct
					&& canAct.templateId() == 700610));
			assertTrue(catalog.findExecutable(11143).orElseThrow().definition().transitions().stream()
				.anyMatch(transition -> transition.event() instanceof QuestEvent.CanAct canAct
					&& canAct.templateId() == 700909));
		}
	}

	@Test
	void emptyDuplicateAndMigrationAnnotatedCatalogsFailClosed() {
		assertEquals("INVALID_PRODUCTION_CATALOG", error("<quest-definition-catalog version=\"2\"/>").code());
		assertEquals("DUPLICATE_CATALOG_OWNER", error("<quest-definition-catalog version=\"2\">"
			+ "<definition id=\"1\" resource=\"one.xml\" mode=\"EXECUTABLE\"/>"
			+ "<definition id=\"1\" resource=\"two.xml\" mode=\"METADATA_ONLY\"/>"
			+ "</quest-definition-catalog>").code());
		assertEquals("INVALID_PRODUCTION_CATALOG", error("<quest-definition-catalog version=\"2\" "
			+ "ownership=\"CURRENT\"><definition id=\"1\" resource=\"one.xml\" mode=\"EXECUTABLE\"/>"
			+ "</quest-definition-catalog>").code());
		assertEquals("INVALID_CATALOG_VERSION", error("<quest-definition-catalog version=\"1\">"
			+ "<definition id=\"1\" resource=\"one.xml\" mode=\"EXECUTABLE\"/>"
			+ "</quest-definition-catalog>").code());
	}

	@Test
	void missingResourceAndDefinitionIdMismatchFailClosed() {
		String missing = "<quest-definition-catalog version=\"2\">"
			+ "<definition id=\"1\" resource=\"missing.xml\" mode=\"EXECUTABLE\"/></quest-definition-catalog>";
		assertEquals("CATALOG_RESOURCE_MISSING", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCatalogManifest.compile(bytes(missing), getClass().getClassLoader())).code());

		String mismatch = "<quest-definition-catalog version=\"2\">"
			+ "<definition id=\"2\" resource=\"quest-definition-fixtures/one.xml\" mode=\"EXECUTABLE\"/>"
			+ "</quest-definition-catalog>";
		assertEquals("CATALOG_ID_MISMATCH", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCatalogManifest.compile(bytes(mismatch), getClass().getClassLoader())).code());
	}

	@Test
	void metadataOnlyEntriesExposeMetadataButNeverExecutionRoutes() {
		String manifest = "<quest-definition-catalog version=\"2\">"
			+ "<definition id=\"1\" resource=\"quest-definition-fixtures/one.xml\" mode=\"EXECUTABLE\"/>"
			+ "<definition id=\"990002\" resource=\"quest-definition-fixtures/metadata-only.xml\" mode=\"METADATA_ONLY\"/>"
			+ "</quest-definition-catalog>";
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(bytes(manifest), getClass().getClassLoader());

		assertEquals(2, catalog.entries().size());
		assertEquals(List.of(1), catalog.executables().stream().map(CompiledQuestDefinition::id).toList());
		assertEquals(QuestCatalogEntryMode.METADATA_ONLY, catalog.findEntry(990002).orElseThrow().mode());
		assertEquals("metadata-only", catalog.findMetadata(990002).orElseThrow().name());
		assertTrue(catalog.findExecutable(990002).isEmpty());
	}

	private static QuestCompilationException error(String xml) {
		return assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCatalogManifest.load(bytes(xml)));
	}

	private static ByteArrayInputStream bytes(String xml) {
		return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
	}
}
