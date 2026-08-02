package com.aionemu.gameserver.questEngine.definition;

import java.util.List;
import java.util.Objects;

/** Immutable source definition shared by XML and Java DSL compilers. */
public record QuestDefinition(int id, int version, QuestOwnership ownership,
		List<EvidenceRef> evidence, QuestMetadata metadata, ProgressLayout progressLayout,
		List<QuestNode> nodes, List<QuestTransition> transitions) {
	public QuestDefinition {
		if (id <= 0) {
			throw new IllegalArgumentException("quest id must be positive");
		}
		if (version <= 0) {
			throw new IllegalArgumentException("version must be positive");
		}
		ownership = Objects.requireNonNull(ownership, "ownership");
		evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
		metadata = Objects.requireNonNull(metadata, "metadata");
		progressLayout = Objects.requireNonNull(progressLayout, "progressLayout");
		nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
		transitions = List.copyOf(Objects.requireNonNull(transitions, "transitions"));
	}

	public static QuestDefinition catalogOnly(int id, int version, QuestMetadata metadata,
			List<EvidenceRef> evidence) {
		return new QuestDefinition(id, version, QuestOwnership.CATALOG_ONLY, evidence, metadata,
			ProgressLayout.empty(), List.of(), List.of());
	}
}
