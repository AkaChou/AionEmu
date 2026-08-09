package com.aionemu.gameserver.questEngine.definition;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable runtime catalog snapshot. Publishing the dispatcher that owns this registry atomically installs
 * metadata, executable owners, and all indexes built from the same compiled catalog.
 */
public final class QuestCatalogRegistry implements QuestCatalog {
	private final QuestCatalog catalog;

	public QuestCatalogRegistry(QuestCatalog catalog) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
	}

	public static QuestCatalogRegistry empty() {
		return new QuestCatalogRegistry(new ImmutableQuestCatalog(java.util.List.of()));
	}

	@Override
	public Optional<QuestCatalogEntry> findEntry(int questId) {
		return catalog.findEntry(questId);
	}

	@Override
	public Optional<QuestMetadata> findMetadata(int questId) {
		return catalog.findMetadata(questId);
	}

	@Override
	public Optional<CompiledQuestDefinition> findExecutable(int questId) {
		return catalog.findExecutable(questId);
	}

	@Override
	public Collection<QuestCatalogEntry> entries() {
		return catalog.entries();
	}

	@Override
	public Collection<CompiledQuestDefinition> executables() {
		return catalog.executables();
	}
}
