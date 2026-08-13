package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDefinitionCatalogManifestTest {
	@TempDir
	Path tempDirectory;

	@Test
	void externalProductionCatalogCompiles() {
		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest_definition"));
		assertFalse(catalog.executables().isEmpty());
		assertTrue(catalog.entries().size() > catalog.executables().size());
		assertTrue(catalog.entries().stream().allMatch(entry -> catalog.findMetadata(entry.id()).isPresent()));
		assertTrue(catalog.findExecutable(11036).orElseThrow().definition().transitions().stream()
			.anyMatch(transition -> transition.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == 700610));
		assertTrue(catalog.findExecutable(11143).orElseThrow().definition().transitions().stream()
			.anyMatch(transition -> transition.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == 700909));
		assertRepeatStartDialogs(catalog);
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

	@Test
	void externalGameDataDirectoryCompilesWithoutPackagedQuestResources() throws Exception {
		Path quests = Files.createDirectories(tempDirectory.resolve("quests"));
		copyResource("/aion/data/static_data/quest_definition/quest_definition.xsd",
			tempDirectory.resolve("quest_definition.xsd"));
		copyResource("/aion/data/static_data/quest_definition/quest_definition_catalog.xsd",
			tempDirectory.resolve("quest_definition_catalog.xsd"));
		copyResource("/quest-definition-fixtures/one.xml", quests.resolve("1.xml"));
		copyResource("/quest-definition-fixtures/metadata-only.xml", quests.resolve("990002.xml"));
		Files.writeString(tempDirectory.resolve("quest_definition_catalog.xml"), """
			<quest-definition-catalog version="2">
			  <definition id="1" resource="aion/data/static_data/quest_definition/quests/1.xml" mode="EXECUTABLE"/>
			  <definition id="990002" resource="aion/data/static_data/quest_definition/quests/990002.xml" mode="METADATA_ONLY"/>
			</quest-definition-catalog>
			""");

		QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(tempDirectory);

		assertEquals(List.of(1), catalog.executables().stream().map(CompiledQuestDefinition::id).toList());
		assertEquals("metadata-only", catalog.findMetadata(990002).orElseThrow().name());
	}

	private void copyResource(String resource, Path target) throws Exception {
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			Files.copy(java.util.Objects.requireNonNull(input, resource), target);
		}
	}

	private static QuestCompilationException error(String xml) {
		return assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCatalogManifest.load(bytes(xml)));
	}

	private static ByteArrayInputStream bytes(String xml) {
		return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
	}

	private static void assertRepeatStartDialogs(QuestCatalog catalog) {
		int checked = 0;
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			if (definition.metadata().repeatPolicy().maxRepeatCount() <= 1) {
				continue;
			}
			Map<String, QuestStatus> statuses = new HashMap<>();
			for (QuestNode node : definition.nodes()) {
				statuses.put(node.label(), node.projection().status());
			}
			Set<Integer> startNpcs = definition.transitions().stream()
				.filter(transition -> statuses.get(transition.sourceNode()) == QuestStatus.NONE)
				.filter(transition -> statuses.get(transition.targetNode()) == QuestStatus.START)
				.filter(transition -> transition.conditions().contains(new QuestCondition.StartEligible()))
				.map(QuestTransition::event)
				.filter(QuestEvent.TalkToNpc.class::isInstance)
				.map(QuestEvent.TalkToNpc.class::cast)
				.filter(talk -> talk.dialogId() != null && (talk.dialogId() == 1002 || talk.dialogId() == 20000))
				.map(QuestEvent.TalkToNpc::npcId)
				.collect(Collectors.toSet());
			List<String> completeNodes = definition.nodes().stream()
				.filter(node -> node.projection().status() == QuestStatus.COMPLETE)
				.map(QuestNode::label).toList();
			for (QuestTransition opening : definition.transitions()) {
				if (statuses.get(opening.sourceNode()) != QuestStatus.NONE
						|| statuses.get(opening.targetNode()) != QuestStatus.NONE
						|| !(opening.event() instanceof QuestEvent.TalkToNpc talk)
						|| !startNpcs.contains(talk.npcId()) || talk.dialogId() == null
						|| !opening.actions().isEmpty()
						|| !opening.afterCommit().stream().allMatch(action ->
							action instanceof AfterCommitAction.ShowQuestDialog
								|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
								|| action instanceof AfterCommitAction.ShowDialogWindow
								|| action instanceof AfterCommitAction.CloseDialog)) {
					continue;
				}
				for (String complete : completeNodes) {
					checked++;
					assertTrue(definition.transitions().stream().anyMatch(transition ->
						complete.equals(transition.sourceNode())
							&& complete.equals(transition.targetNode())
							&& transition.event().equals(opening.event())
							&& transition.conditions().contains(new QuestCondition.StartEligible())
							&& transition.afterCommit().equals(opening.afterCommit())),
						"missing repeat dialog route: quest=" + compiled.id() + " source=" + complete
							+ " npc=" + talk.npcId() + " dialog=" + talk.dialogId());
				}
			}
		}
		assertTrue(checked > 0, "repeat dialog audit must inspect production routes");
	}
}
