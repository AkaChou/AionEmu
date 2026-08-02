package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;

/** A statically declared reward; delivery is owned by the reward port. */
public record QuestReward(String kind, int id, long amount) {
	public QuestReward {
		kind = requireText(kind, "kind");
		if (id < 0) {
			throw new IllegalArgumentException("reward id must be non-negative");
		}
		if (amount <= 0) {
			throw new IllegalArgumentException("reward amount must be positive");
		}
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}
}
