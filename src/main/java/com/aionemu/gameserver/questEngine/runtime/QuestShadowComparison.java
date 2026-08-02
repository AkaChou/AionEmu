package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.List;
import java.util.Objects;

/**
 * One physical-event legacy invocation compared with one candidate inspection.
 * The selector and frozen owner inputs make a persisted mismatch reproducible
 * instead of collapsing every event of the same type into one opaque entry.
 */
public record QuestShadowComparison(String eventType, String eventSelector,
		List<OwnerInput> inputs, List<QuestShadowDifference> differences) {
	public QuestShadowComparison {
		eventType = requireText(eventType, "eventType");
		eventSelector = requireText(eventSelector, "eventSelector");
		inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
		differences = List.copyOf(Objects.requireNonNull(differences, "differences"));
	}

	public boolean clean() {
		return differences.isEmpty();
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}

	/** Minimal pre-event owner state required to reproduce one comparison. */
	public record OwnerInput(int questId, QuestStatus status, int packedVariables) {
		public OwnerInput {
			if (questId <= 0) {
				throw new IllegalArgumentException("questId must be positive");
			}
			status = Objects.requireNonNull(status, "status");
			if (packedVariables < 0) {
				throw new IllegalArgumentException("packedVariables must not be negative");
			}
		}
	}
}
