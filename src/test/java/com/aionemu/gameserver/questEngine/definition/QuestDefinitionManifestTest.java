package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDefinitionManifestTest {
	@Test
	void packagedManifestIsExplicitAndKeepsProductionOwnerSwitchOff() {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quest_definition_manifest.xml")) {
			QuestDefinitionManifest manifest = QuestDefinitionManifest.load(input);
			assertEquals(1, manifest.version());
			assertEquals(1, manifest.inputs().size());
			assertEquals("LEGACY_METADATA", manifest.inputs().get(0).family());
			assertEquals("repo:src/main/resources/aion/data/static_data/quest_data/quest_data.xml",
				manifest.inputs().get(0).path());
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	@Test
	void externalAndUnsafeInputPathsFailClosed() {
		String external = "<quest-definition-manifest version=\"1\" production-owner-switch=\"0\">"
			+ "<input id=\"a\" family=\"A\" kind=\"K\" path=\"external:other-project/input.xml\"/>"
			+ "</quest-definition-manifest>";
		assertEquals("EXTERNAL_MANIFEST_PATH_FORBIDDEN", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionManifest.load(new ByteArrayInputStream(external.getBytes(StandardCharsets.UTF_8)))).code());

		String traversal = "<quest-definition-manifest version=\"1\" production-owner-switch=\"0\">"
			+ "<input id=\"a\" family=\"A\" kind=\"K\" path=\"repo:../other-project/input.xml\"/>"
			+ "</quest-definition-manifest>";
		assertEquals("UNSAFE_MANIFEST_PATH", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionManifest.load(new ByteArrayInputStream(traversal.getBytes(StandardCharsets.UTF_8)))).code());
	}

	@Test
	void productionOwnerSwitchAndDuplicateInputsFailClosed() {
		String switched = "<quest-definition-manifest version=\"1\" production-owner-switch=\"1\"><input id=\"a\" family=\"A\" kind=\"K\" path=\"P\"/></quest-definition-manifest>";
		assertEquals("PRODUCTION_OWNER_SWITCH_FORBIDDEN", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionManifest.load(new ByteArrayInputStream(switched.getBytes(StandardCharsets.UTF_8)))).code());

		String duplicate = "<quest-definition-manifest version=\"1\" production-owner-switch=\"0\"><input id=\"a\" family=\"A\" kind=\"K\" path=\"repo:input-a.xml\"/><input id=\"a\" family=\"B\" kind=\"K\" path=\"repo:input-b.xml\"/></quest-definition-manifest>";
		assertEquals("DUPLICATE_MANIFEST_INPUT", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionManifest.load(new ByteArrayInputStream(duplicate.getBytes(StandardCharsets.UTF_8)))).code());
	}
}
