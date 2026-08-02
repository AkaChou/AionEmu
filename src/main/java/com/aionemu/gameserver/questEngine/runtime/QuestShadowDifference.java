package com.aionemu.gameserver.questEngine.runtime;

import java.util.Objects;

/** Immutable typed shadow mismatch; quest id 0 denotes a dispatch-wide result mismatch. */
public record QuestShadowDifference(QuestShadowDifferenceKind kind, int questId) {
	public QuestShadowDifference {
		kind = Objects.requireNonNull(kind, "kind");
		if (questId < 0) {
			throw new IllegalArgumentException("questId must not be negative");
		}
	}
}
