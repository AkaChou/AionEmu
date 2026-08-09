package com.aionemu.gameserver.questEngine.definition;

import java.util.Collection;
import java.util.Optional;

/** The only lookup contract for canonical quest metadata and executable owners. */
public interface QuestCatalog {
	Optional<QuestCatalogEntry> findEntry(int questId);

	Optional<QuestMetadata> findMetadata(int questId);

	Optional<CompiledQuestDefinition> findExecutable(int questId);

	Collection<QuestCatalogEntry> entries();

	Collection<CompiledQuestDefinition> executables();

	/** Backward-compatible executable lookup while callers migrate to the explicit contract. */
	default Optional<CompiledQuestDefinition> find(int questId) {
		return findExecutable(questId);
	}

	/** Backward-compatible executable collection while callers migrate to the explicit contract. */
	default Collection<CompiledQuestDefinition> all() {
		return executables();
	}
}
