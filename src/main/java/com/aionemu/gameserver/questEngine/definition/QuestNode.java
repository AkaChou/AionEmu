package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;

/** A compile-time label; it is never persisted as a graph node. */
public record QuestNode(String label, NodeProjection projection) {
	public QuestNode {
		if (label == null || label.isBlank()) {
			throw new IllegalArgumentException("node label must not be blank");
		}
		projection = Objects.requireNonNull(projection, "projection");
	}
}
