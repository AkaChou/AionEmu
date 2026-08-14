package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;
import java.util.Optional;

/**
 * 一条不可变的规范目录条目；仅元数据条目从不暴露可执行归属。
 * One immutable canonical catalog entry. Metadata-only entries never expose an executable owner.
 */
public record QuestCatalogEntry(int id, int version, QuestCatalogEntryMode mode,
		QuestMetadata metadata, Optional<CompiledQuestDefinition> executable) {
	public QuestCatalogEntry {
		if (id <= 0) {
			throw new IllegalArgumentException("quest id must be positive");
		}
		if (version <= 0) {
			throw new IllegalArgumentException("quest version must be positive");
		}
		mode = Objects.requireNonNull(mode, "mode");
		metadata = Objects.requireNonNull(metadata, "metadata");
		executable = Objects.requireNonNull(executable, "executable");
		if ((mode == QuestCatalogEntryMode.EXECUTABLE) != executable.isPresent()) {
			throw new IllegalArgumentException("catalog mode and executable owner must agree");
		}
		if (executable.isPresent()) {
			CompiledQuestDefinition definition = executable.orElseThrow();
			if (definition.id() != id || definition.version() != version
					|| definition.definition().metadata() != metadata) {
				throw new IllegalArgumentException("executable definition does not match catalog entry");
			}
		}
	}

	public static QuestCatalogEntry executable(CompiledQuestDefinition definition) {
		Objects.requireNonNull(definition, "definition");
		return new QuestCatalogEntry(definition.id(), definition.version(), QuestCatalogEntryMode.EXECUTABLE,
			definition.definition().metadata(), Optional.of(definition));
	}

	public static QuestCatalogEntry metadataOnly(QuestDefinition definition) {
		Objects.requireNonNull(definition, "definition");
		if (!definition.nodes().isEmpty() || !definition.transitions().isEmpty()) {
			throw new QuestCompilationException("METADATA_ONLY_EXECUTION_DECLARED",
				"metadata-only quest " + definition.id() + " must not declare nodes or transitions");
		}
		return new QuestCatalogEntry(definition.id(), definition.version(), QuestCatalogEntryMode.METADATA_ONLY,
			definition.metadata(), Optional.empty());
	}
}
