package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 防止 Aion 5.8 客户端 999 级禁用占位重新成为生产任务 owner。
 * Prevents level-999 placeholders disabled by the Aion 5.8 client from becoming production quest owners again.
 */
class DisabledClientQuestPlaceholderCatalogTest {
	private static final Path DEFINITION_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition");
	private static final Path QUEST_DIRECTORY = DEFINITION_DIRECTORY.resolve("quests");
	private static final List<Integer> DISABLED_PLACEHOLDERS = List.of(
		2285, 3959, 4963, 10036, 18316, 18395, 19055, 19056, 20036, 20038,
		28316, 28395, 29055, 29056, 80313, 80314);
	private static final Map<Integer, List<QuestStartCondition>> SUPPORT_ORDER_CONDITIONS = Map.of(
		10031, List.of(
			new QuestStartCondition("unfinished", 10025, 0),
			new QuestStartCondition("unfinished", 14062, 0)),
		20031, List.of(
			new QuestStartCondition("unfinished", 20025, 0),
			new QuestStartCondition("unfinished", 24062, 0)));

	@Test
	void disabledClientPlaceholdersAreNotProductionOwners() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(DEFINITION_DIRECTORY);

		for (int questId : DISABLED_PLACEHOLDERS) {
			assertTrue(catalog.findEntry(questId).isEmpty(),
				() -> "client-disabled placeholder " + questId + " must not be registered");
		}
	}

	@Test
	void disabledClientPlaceholderDefinitionsAreNotPackaged() {
		for (int questId : DISABLED_PLACEHOLDERS) {
			Path definition = QUEST_DIRECTORY.resolve(questId + ".xml");
			assertFalse(Files.exists(definition), () -> "disabled placeholder definition remains packaged: " + definition);
		}
	}

	@Test
	void supportOrderPrerequisitesStayOnTheClientEligibleQuest() throws Exception {
		for (Map.Entry<Integer, List<QuestStartCondition>> entry : SUPPORT_ORDER_CONDITIONS.entrySet()) {
			QuestDefinition definition = compile(entry.getKey());
			assertEquals(50, definition.metadata().minLevel(), "quest " + entry.getKey() + " minimum level");
			assertEquals(entry.getValue(), definition.metadata().startConditions(),
				"quest " + entry.getKey() + " start conditions");
		}
	}

	private static QuestDefinition compile(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))) {
			QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();
			assertNotNull(definition);
			return definition;
		}
	}
}
