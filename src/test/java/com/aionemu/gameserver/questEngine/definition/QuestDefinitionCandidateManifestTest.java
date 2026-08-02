package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDefinitionCandidateManifestTest {
	@TempDir
	Path tempDir;

	@Test
	void packagedCandidateManifestCompilesAndMatchesEveryEntry() {
		try (InputStream input = getClass().getResourceAsStream(
			"/quest-definition-candidates/manifest.xml")) {
			QuestCatalog catalog = QuestDefinitionCandidateManifest.compile(input, getClass().getClassLoader());
			assertEquals(java.util.List.of(1, 2), catalog.all().stream()
				.map(CompiledQuestDefinition::id).toList());
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	@Test
	void compiledCandidateManifestLoadsAndVerifiesOwnership() {
		String manifest = "<quest-definition-candidate-manifest version=\"1\" ownership=\"COMPILED_CANDIDATE\">"
			+ "<definition id=\"1115\" resource=\"quest-definition-candidates/simpletalk-1115.xml\"/>"
			+ "</quest-definition-candidate-manifest>";
		QuestCatalog catalog = QuestDefinitionCandidateManifest.compile(
			new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8)), getClass().getClassLoader());
		assertEquals(1, catalog.all().size());
		CompiledQuestDefinition definition = catalog.all().stream().findFirst().orElseThrow();
		assertEquals(1115, definition.id());
		assertEquals(QuestOwnership.COMPILED_CANDIDATE, definition.ownership());
	}

	@Test
	void manifestOwnershipMismatchFailsClosed() {
		// manifest 声明 COMPILED_CANDIDATE，但定义实际是 CATALOG_ONLY。
		String manifest = "<quest-definition-candidate-manifest version=\"1\" ownership=\"COMPILED_CANDIDATE\">"
			+ "<definition id=\"1\" resource=\"quest-definition-candidates/one.xml\"/>"
			+ "</quest-definition-candidate-manifest>";
		assertEquals("CANDIDATE_OWNERSHIP_MISMATCH", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCandidateManifest.compile(
					new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8)), getClass().getClassLoader())).code());
	}

	@Test
	void forbiddenOwnershipFailsClosed() {
		String manifest = "<quest-definition-candidate-manifest version=\"1\" ownership=\"UNRESOLVED\">"
			+ "<definition id=\"1\" resource=\"one.xml\"/></quest-definition-candidate-manifest>";
		assertEquals("CANDIDATE_MANIFEST_OWNERSHIP_FORBIDDEN", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCandidateManifest.load(
					new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8)))).code());
	}

	@Test
	void compileFilesLoadsFromRootsAndMissingFailsClosed() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<quest-definition id=\"7\" version=\"1\" ownership=\"COMPILED_CANDIDATE\">"
			+ "<evidence><ref source=\"TEST\" locator=\"compileFiles\" "
			+ "statement=\"filesystem candidate load test\"/></evidence>"
			+ "<metadata name=\"generated\" display-name-id=\"0\" min-level=\"1\" max-level=\"2147483647\" category=\"QUEST\"/>"
			+ "<progress><bit-field name=\"var0\" offset=\"0\" width=\"6\" min=\"0\" max=\"63\" "
			+ "persistence=\"PERSISTENT\" scope=\"LOCAL\"/></progress>"
			+ "<nodes><node label=\"started\"><project status=\"START\"><vars><var name=\"var0\" value=\"0\"/>"
			+ "</vars></project></node></nodes>"
			+ "<transitions><transition source=\"started\" target=\"started\" priority=\"1\">"
			+ "<event><talk-to-npc npc-id=\"203075\"/></event>"
			+ "<actions><set-status status=\"REWARD\"/></actions>"
			+ "</transition></transitions>"
			+ "</quest-definition>";
		Path root = tempDir.resolve("candidates");
		Files.createDirectories(root);
		Files.write(root.resolve("generated-7.xml"), xml.getBytes(StandardCharsets.UTF_8));

		String manifest = "<quest-definition-candidate-manifest version=\"1\" ownership=\"COMPILED_CANDIDATE\">"
			+ "<definition id=\"7\" resource=\"generated-7.xml\"/>"
			+ "</quest-definition-candidate-manifest>";
		QuestCatalog catalog = QuestDefinitionCandidateManifest.compileFiles(
			new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8)), List.of(root));
		assertEquals(List.of(7), catalog.all().stream().map(CompiledQuestDefinition::id).toList());

		String missing = "<quest-definition-candidate-manifest version=\"1\" ownership=\"COMPILED_CANDIDATE\">"
			+ "<definition id=\"8\" resource=\"generated-8.xml\"/>"
			+ "</quest-definition-candidate-manifest>";
		assertEquals("CANDIDATE_RESOURCE_MISSING", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCandidateManifest.compileFiles(
					new ByteArrayInputStream(missing.getBytes(StandardCharsets.UTF_8)), List.of(root))).code());
	}

	@Test
	void unsafeOrDuplicateManifestEntriesFailClosed() {
		String unsafe = "<quest-definition-candidate-manifest version=\"1\" ownership=\"CATALOG_ONLY\">"
			+ "<definition id=\"1\" resource=\"../one.xml\"/></quest-definition-candidate-manifest>";
		assertEquals("INVALID_CANDIDATE_MANIFEST", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCandidateManifest.load(new ByteArrayInputStream(unsafe.getBytes(StandardCharsets.UTF_8)))).code());

		String duplicate = "<quest-definition-candidate-manifest version=\"1\" ownership=\"CATALOG_ONLY\">"
			+ "<definition id=\"1\" resource=\"one.xml\"/><definition id=\"1\" resource=\"two.xml\"/>"
			+ "</quest-definition-candidate-manifest>";
		assertEquals("DUPLICATE_CANDIDATE_ID", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCandidateManifest.load(new ByteArrayInputStream(duplicate.getBytes(StandardCharsets.UTF_8)))).code());
	}

	@Test
	void missingResourceAndDefinitionIdMismatchFailClosed() {
		String missing = "<quest-definition-candidate-manifest version=\"1\" ownership=\"CATALOG_ONLY\">"
			+ "<definition id=\"1\" resource=\"quest-definition-candidates/missing.xml\"/>"
			+ "</quest-definition-candidate-manifest>";
		assertEquals("CANDIDATE_RESOURCE_MISSING", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCandidateManifest.compile(
					new ByteArrayInputStream(missing.getBytes(StandardCharsets.UTF_8)), getClass().getClassLoader())).code());

		String mismatch = "<quest-definition-candidate-manifest version=\"1\" ownership=\"CATALOG_ONLY\">"
			+ "<definition id=\"3\" resource=\"quest-definition-candidates/one.xml\"/>"
			+ "</quest-definition-candidate-manifest>";
		assertEquals("CANDIDATE_ID_MISMATCH", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCandidateManifest.compile(
					new ByteArrayInputStream(mismatch.getBytes(StandardCharsets.UTF_8)), getClass().getClassLoader())).code());
	}
}
