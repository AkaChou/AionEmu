package com.aionemu.gameserver.questEngine.definition;

import java.util.Collection;
import java.util.Optional;

/**
 * 规范任务元数据与可执行归属的唯一查找契约。
 * The only lookup contract for canonical quest metadata and executable owners.
 */
public interface QuestCatalog {
	Optional<QuestCatalogEntry> findEntry(int questId);

	Optional<QuestMetadata> findMetadata(int questId);

	Optional<CompiledQuestDefinition> findExecutable(int questId);

	Collection<QuestCatalogEntry> entries();

	Collection<CompiledQuestDefinition> executables();

	/**
	 * 调用方迁移到显式契约前的向后兼容可执行查找。
	 * Backward-compatible executable lookup while callers migrate to the explicit contract.
	 */
	default Optional<CompiledQuestDefinition> find(int questId) {
		return findExecutable(questId);
	}

	/**
	 * 调用方迁移到显式契约前的向后兼容可执行集合。
	 * Backward-compatible executable collection while callers migrate to the explicit contract.
	 */
	default Collection<CompiledQuestDefinition> all() {
		return executables();
	}
}
