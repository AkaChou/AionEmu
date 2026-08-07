package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class QuestDefinitionJavaDslParityTest {
	@Test
	void everyRetainedXmlDefinitionHasAnEquivalentJavaDslOwner() {
		ClassLoader loader = getClass().getClassLoader();
		QuestCatalog xmlCatalog = QuestDefinitionDirectoryLoader.compileXml(loader);
		QuestCatalog javaCatalog = QuestDefinitionJavaCatalog.compile();

		assertEquals(xmlCatalog.all().size(), javaCatalog.all().size());
		for (CompiledQuestDefinition xmlDefinition : xmlCatalog.all()) {
			CompiledQuestDefinition javaDefinition = javaCatalog.find(xmlDefinition.id()).orElseThrow(
				() -> new AssertionError("missing Java DSL owner for quest " + xmlDefinition.id()));
			assertEquals(xmlDefinition.definition(), javaDefinition.definition(),
				"XML and Java DSL differ for quest " + xmlDefinition.id());
		}
	}

	@Test
	void directoryAndManifestUseTheJavaOwnerWhenBothSourcesExist() throws Exception {
		ClassLoader loader = getClass().getClassLoader();
		QuestCatalog javaCatalog = QuestDefinitionJavaCatalog.compile();
		QuestCatalog directoryCatalog = QuestDefinitionDirectoryLoader.compile(loader);
		assertSame(javaCatalog.find(1000).orElseThrow(), directoryCatalog.find(1000).orElseThrow());

		try (InputStream input = loader.getResourceAsStream(
			"aion/data/static_data/quest_definition/quest_definition_catalog.xml")) {
			QuestCatalog manifestCatalog = QuestDefinitionCatalogManifest.compile(input, loader);
			assertSame(javaCatalog.find(1000).orElseThrow(), manifestCatalog.find(1000).orElseThrow());
		}
	}
}
