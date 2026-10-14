package com.aionemu.gameserver.questEngine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * QuestDataResources 定位三态：外部运行目录优先 / classpath 回退 / 皆无返回 null。
 * QuestDataResources lookup: external runtime directory first, classpath fallback, null when neither.
 */
class QuestDataResourcesTest {
	private static final String DATA_RESOURCE = "aion/data/static_data/quest/definitions/quest_definition.xsd";
	private static final String DEFINITIONS_RESOURCE = "aion/definitions/compact/quests/limited-quests.xml";

	@TempDir
	private Path dataDir;

	@TempDir
	private Path definitionsDir;

	@Test
	void externalRuntimeDirectoryWinsOverClasspath() throws IOException {
		write(dataDir, "static_data/quest/definitions/quest_definition.xsd", "external-marker");
		withProperties(dataDir, definitionsDir, () -> assertEquals("external-marker", read(DATA_RESOURCE)));
	}

	@Test
	void fallsBackToClasspathWhenExternalFileIsAbsent() throws IOException {
		withProperties(dataDir.resolve("missing"), definitionsDir, () -> assertNotNull(read(DATA_RESOURCE)));
	}

	@Test
	void returnsNullWhenNeitherSourceHasTheResource() throws IOException {
		withProperties(dataDir, definitionsDir,
			() -> assertNull(QuestDataResources.open("aion/data/no/such/resource.xml")));
	}

	@Test
	void definitionsPrefixUsesDefinitionsDirectoryNotDataDirectory() throws IOException {
		write(definitionsDir, "compact/quests/limited-quests.xml", "definitions-marker");
		withProperties(dataDir, definitionsDir, () -> assertEquals("definitions-marker", read(DEFINITIONS_RESOURCE)));
	}

	@Test
	void unsetPropertiesSkipTheExternalBranch() throws IOException {
		withProperties(null, null, () -> assertNotNull(read(DATA_RESOURCE)));
		assertNull(QuestDataResources.open("aion/data/no/such/resource.xml"));
	}

	@Test
	void leadingSlashIsEquivalentToThePlainPath() throws IOException {
		withProperties(dataDir, definitionsDir,
			() -> assertEquals(read(DATA_RESOURCE), read("/" + DATA_RESOURCE)));
	}

	private static void write(Path root, String relativePath, String content) throws IOException {
		Path file = root.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content, StandardCharsets.UTF_8);
	}

	private static String read(String resourcePath) throws IOException {
		try (InputStream input = QuestDataResources.open(resourcePath)) {
			return input == null ? null : new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static void withProperties(Path data, Path definitions, IoBody body) throws IOException {
		String previousData = System.getProperty("aion.game.data.dir");
		String previousDefinitions = System.getProperty("aion.game.definitions.dir");
		try {
			applyProperty("aion.game.data.dir", data == null ? null : data.toString());
			applyProperty("aion.game.definitions.dir", definitions == null ? null : definitions.toString());
			body.run();
		} finally {
			applyProperty("aion.game.data.dir", previousData);
			applyProperty("aion.game.definitions.dir", previousDefinitions);
		}
	}

	private static void applyProperty(String key, String value) {
		if (value == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, value);
		}
	}

	@FunctionalInterface
	private interface IoBody {
		void run() throws IOException;
	}
}
