package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDefinitionCatalogManifestTest {
	@Test
	void packagedProductionCatalogMatchesEveryPackagedQuestDefinitionFile() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quest_definition_catalog.xml")) {
			QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(input, getClass().getClassLoader());
			// The catalog must mirror the packaged quests/*.xml set exactly, so adding a
			// definition never requires touching this assertion.
			assertEquals(packagedQuestFileIds(),
				catalog.all().stream().map(CompiledQuestDefinition::id).toList());
		}
	}

	private static List<Integer> packagedQuestFileIds() throws Exception {
		File dir = new File(QuestDefinitionCatalogManifestTest.class.getResource(
				"/aion/data/static_data/quest_definition/quests").toURI());
		File[] files = dir.listFiles(file -> file.getName().endsWith(".xml"));
		return Arrays.stream(files).map(file -> Integer.parseInt(file.getName().replace(".xml", "")))
			.sorted().toList();
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
