package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestDialogMigrationEquivalenceTest {
	private static final Set<String> EXPECTED_BEHAVIOR_CHANGES = Set.of(
		"1993.xml", "1994.xml", "2993.xml", "2994.xml",
		"80292.xml", "80293.xml", "80296.xml", "80297.xml");

	@Test
	@EnabledIfSystemProperty(named = "quest.dialog.migration.source", matches = ".+")
	void migratedDefinitionsPreserveCanonicalIr() throws Exception {
		Path source = Path.of(System.getProperty("quest.dialog.migration.source"));
		Path target = Path.of(System.getProperty("quest.dialog.migration.target"));
		List<Path> sourceFiles;
		try (var files = Files.list(source)) {
			sourceFiles = files.filter(path -> path.getFileName().toString().endsWith(".xml")).sorted().toList();
		}
		assertEquals(6246, sourceFiles.size());
		Map<String, String> differences = new LinkedHashMap<>();
		for (Path sourceFile : sourceFiles) {
			Path targetFile = target.resolve(sourceFile.getFileName());
			String fileName = sourceFile.getFileName().toString();
			if (!Files.isRegularFile(targetFile)) {
				differences.put(fileName, "target file is missing");
				continue;
			}
			try (InputStream oldInput = Files.newInputStream(sourceFile);
					InputStream newInput = Files.newInputStream(targetFile)) {
				QuestDefinition before;
				QuestDefinition after;
				try {
					before = QuestDefinitionXmlCompiler.parse(oldInput);
					after = QuestDefinitionXmlCompiler.parse(newInput);
				} catch (RuntimeException e) {
					throw new AssertionError(fileName, e);
				}
				if (!before.nodes().isEmpty()) {
					try (InputStream oldCompiledInput = Files.newInputStream(sourceFile);
							InputStream newCompiledInput = Files.newInputStream(targetFile)) {
						try {
							before = QuestDefinitionXmlCompiler.compile(oldCompiledInput).definition();
							after = QuestDefinitionXmlCompiler.compile(newCompiledInput).definition();
						} catch (RuntimeException e) {
							throw new AssertionError(fileName, e);
						}
					}
				}
				String difference = QuestXmlMigrationVerifier.IrDiffer.firstDifference(before, after);
				if (difference != null) {
					differences.put(fileName, difference);
				}
			}
		}
		assertEquals(EXPECTED_BEHAVIOR_CHANGES, differences.keySet(), differences.toString());
	}
}
