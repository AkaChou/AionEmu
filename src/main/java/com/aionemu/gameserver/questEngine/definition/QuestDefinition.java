package com.aionemu.gameserver.questEngine.definition;

import java.util.List;
import java.util.Objects;

/**
 * XML 与 Java DSL compiler 共用的不可变任务定义。
 * Immutable quest definition shared by XML and Java DSL compilers.
 */
public record QuestDefinition(int id, int version,
		QuestMetadata metadata, ProgressLayout progressLayout,
		List<QuestNode> nodes, List<QuestTransition> transitions) {
	public QuestDefinition {
		if (id <= 0) {
			throw new IllegalArgumentException("quest id must be positive");
		}
		if (version <= 0) {
			throw new IllegalArgumentException("version must be positive");
		}
		metadata = Objects.requireNonNull(metadata, "metadata");
		progressLayout = Objects.requireNonNull(progressLayout, "progressLayout");
		nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
		transitions = List.copyOf(Objects.requireNonNull(transitions, "transitions"));
	}
}
