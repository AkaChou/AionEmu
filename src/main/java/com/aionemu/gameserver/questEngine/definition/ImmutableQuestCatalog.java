package com.aionemu.gameserver.questEngine.definition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Deterministic immutable catalog; duplicate owners fail during construction. */
public final class ImmutableQuestCatalog implements QuestCatalog {
	private final Map<Integer, QuestCatalogEntry> entries;
	private final Collection<CompiledQuestDefinition> executables;

	public ImmutableQuestCatalog(Collection<CompiledQuestDefinition> definitions) {
		this(definitions.stream().map(QuestCatalogEntry::executable).toList(), true);
	}

	public static ImmutableQuestCatalog fromEntries(Collection<QuestCatalogEntry> entries) {
		return new ImmutableQuestCatalog(entries, true);
	}

	private ImmutableQuestCatalog(Collection<QuestCatalogEntry> entries, boolean ignored) {
		Objects.requireNonNull(entries, "entries");
		Map<Integer, QuestCatalogEntry> index = new LinkedHashMap<>();
		for (QuestCatalogEntry entry : entries) {
			Objects.requireNonNull(entry, "entry");
			if (index.putIfAbsent(entry.id(), entry) != null) {
				throw new QuestCompilationException("DUPLICATE_CATALOG_ENTRY",
					"duplicate quest catalog entry: " + entry.id());
			}
		}
		this.entries = Collections.unmodifiableMap(index);
		this.executables = index.values().stream().flatMap(entry -> entry.executable().stream()).toList();
	}

	@Override
	public Optional<QuestCatalogEntry> findEntry(int questId) {
		return Optional.ofNullable(entries.get(questId));
	}

	@Override
	public Optional<QuestMetadata> findMetadata(int questId) {
		return findEntry(questId).map(QuestCatalogEntry::metadata);
	}

	@Override
	public Optional<CompiledQuestDefinition> findExecutable(int questId) {
		return findEntry(questId).flatMap(QuestCatalogEntry::executable);
	}

	@Override
	public Collection<QuestCatalogEntry> entries() {
		return entries.values();
	}

	@Override
	public Collection<CompiledQuestDefinition> executables() {
		return executables;
	}
}
