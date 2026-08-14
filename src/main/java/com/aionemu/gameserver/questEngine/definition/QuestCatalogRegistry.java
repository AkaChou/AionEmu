package com.aionemu.gameserver.questEngine.definition;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * 不可变的运行时目录快照。发布持有该注册表的分发器时，会以同一编译目录原子性安装
 * 元数据、可执行归属及全部索引。
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
