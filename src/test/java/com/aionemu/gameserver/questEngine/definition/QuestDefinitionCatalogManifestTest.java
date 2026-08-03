package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDefinitionCatalogManifestTest {
	@Test
	void packagedProductionCatalogContainsTheLiveTypedOwners() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quest_definition_catalog.xml")) {
			QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(input, getClass().getClassLoader());
			assertEquals(List.of(1101, 1102, 1103, 1104, 1105, 1106, 1108, 1109, 1110, 1116, 1117, 1118, 1119,
				1121, 1124, 1125, 1126, 1129, 1206, 1207), catalog.all().stream().map(CompiledQuestDefinition::id).toList());
			assertEquals(QuestOwnership.RETAIL_ALIGNED, catalog.find(1101).orElseThrow().ownership());
			assertEquals(QuestOwnership.RETAIL_ALIGNED, catalog.find(1102).orElseThrow().ownership());
		}
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
