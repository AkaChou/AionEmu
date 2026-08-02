package com.aionemu.gameserver.questEngine.definition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Candidate-only batch importer; missing or invalid definitions never fall back silently. */
public final class QuestDefinitionCandidateImporter {
	public record Source(String resource, byte[] xml) {
		public Source {
			if (resource == null || resource.isBlank()) {
				throw new IllegalArgumentException("resource must not be blank");
			}
			xml = Objects.requireNonNull(xml, "xml").clone();
			if (xml.length == 0) {
				throw new IllegalArgumentException("definition XML must not be empty");
			}
		}

		@Override
		public byte[] xml() {
			return xml.clone();
		}
	}

	private QuestDefinitionCandidateImporter() {
	}

	public static QuestCatalog compile(Collection<Source> sources) {
		Objects.requireNonNull(sources, "sources");
		if (sources.isEmpty()) {
			throw new QuestCompilationException("EMPTY_CANDIDATE_SET", "candidate import requires at least one definition");
		}
		List<Source> ordered = new ArrayList<>(sources);
		ordered.sort(Comparator.comparing(Source::resource));
		List<CompiledQuestDefinition> definitions = new ArrayList<>();
		for (Source source : ordered) {
			CompiledQuestDefinition definition;
			try (InputStream input = new java.io.ByteArrayInputStream(source.xml())) {
				definition = QuestDefinitionXmlCompiler.compile(input);
			} catch (QuestCompilationException e) {
				throw new QuestCompilationException("CANDIDATE_INVALID", source.resource() + ": " + e.code());
			} catch (IOException e) {
				throw new QuestCompilationException("CANDIDATE_READ_FAILED", source.resource());
			}
			if (definition.ownership() != QuestOwnership.CATALOG_ONLY
					&& definition.ownership() != QuestOwnership.COMPILED_CANDIDATE) {
				throw new QuestCompilationException("CANDIDATE_OWNERSHIP_FORBIDDEN",
					source.resource() + ": " + definition.ownership());
			}
			definitions.add(definition);
		}
		return new ImmutableQuestCatalog(definitions);
	}

	public static QuestCatalog compileClasspath(Collection<String> resources, ClassLoader loader) {
		Objects.requireNonNull(resources, "resources");
		Objects.requireNonNull(loader, "loader");
		List<Source> sources = new ArrayList<>();
		for (String resource : resources) {
			if (resource == null || resource.isBlank()) {
				throw new QuestCompilationException("INVALID_CANDIDATE_RESOURCE", "resource must not be blank");
			}
			try (InputStream input = loader.getResourceAsStream(resource)) {
				if (input == null) {
					throw new QuestCompilationException("CANDIDATE_RESOURCE_MISSING", resource);
				}
				sources.add(new Source(resource, input.readAllBytes()));
			} catch (IOException e) {
				throw new QuestCompilationException("CANDIDATE_READ_FAILED", resource);
			}
		}
		return compile(sources);
	}
}
