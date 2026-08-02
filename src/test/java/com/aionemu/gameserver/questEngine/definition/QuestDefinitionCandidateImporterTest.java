package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDefinitionCandidateImporterTest {
	private static final String CATALOG_ONLY = """
			<quest-definition id="%d" version="1" ownership="CATALOG_ONLY">
			  <evidence><ref source="test" locator="quest/%d" statement="fixture"/></evidence>
			  <metadata name="q-%d" display-name-id="1" min-level="0" max-level="1" category="QUEST"/>
			</quest-definition>
			""";

	@Test
	void batchImportIsDeterministicAndRejectsDuplicateOwners() {
		QuestCatalog catalog = QuestDefinitionCandidateImporter.compile(List.of(
				new QuestDefinitionCandidateImporter.Source("b.xml", xml(1002)),
				new QuestDefinitionCandidateImporter.Source("a.xml", xml(1001))));
		assertEquals(List.of(1001, 1002), catalog.all().stream().map(CompiledQuestDefinition::id).toList());

		assertEquals("DUPLICATE_OWNER", assertThrows(QuestCompilationException.class,
				() -> QuestDefinitionCandidateImporter.compile(List.of(
						new QuestDefinitionCandidateImporter.Source("a.xml", xml(1001)),
						new QuestDefinitionCandidateImporter.Source("b.xml", xml(1001))))).code());
	}

	@Test
	void emptyOrMissingCandidateFailsClosed() {
		assertEquals("EMPTY_CANDIDATE_SET", assertThrows(QuestCompilationException.class,
				() -> QuestDefinitionCandidateImporter.compile(List.of())).code());
		assertEquals("CANDIDATE_RESOURCE_MISSING", assertThrows(QuestCompilationException.class,
				() -> QuestDefinitionCandidateImporter.compileClasspath(List.of("missing.xml"), getClass().getClassLoader())).code());
	}

	@Test
	void analysisDraftMayBeSyntaxCheckedButCannotEnterCandidateCatalog() {
		String draft = """
				<quest-definition id="1003" version="1" ownership="ANALYSIS_DRAFT">
				  <evidence><ref source="tool" locator="owner/1003" statement="mechanical draft"/></evidence>
				  <metadata name="draft" display-name-id="0" min-level="1" max-level="1" category="QUEST"/>
				  <nodes><node label="started"><project status="START"/></node></nodes>
				  <transitions><transition source="started" target="started">
				    <event><talk-to-npc npc-id="700001"/></event>
				  </transition></transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(new java.io.ByteArrayInputStream(
			draft.getBytes(StandardCharsets.UTF_8)));
		assertEquals(QuestOwnership.ANALYSIS_DRAFT, compiled.ownership());
		assertEquals("CANDIDATE_OWNERSHIP_FORBIDDEN", assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionCandidateImporter.compile(List.of(
				new QuestDefinitionCandidateImporter.Source("draft.xml", draft.getBytes(StandardCharsets.UTF_8))))).code());
		assertEquals("ANALYSIS_DRAFT_CATALOG_FORBIDDEN", assertThrows(QuestCompilationException.class,
			() -> new ImmutableQuestCatalog(List.of(compiled))).code());
	}

	private static byte[] xml(int id) {
		return (CATALOG_ONLY.formatted(id, id, id)).getBytes(StandardCharsets.UTF_8);
	}
}
