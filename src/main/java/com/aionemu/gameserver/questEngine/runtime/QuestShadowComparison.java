package com.aionemu.gameserver.questEngine.runtime;

import java.util.List;
import java.util.Objects;

/**
 * One physical-event legacy invocation compared with one candidate inspection.
 * Event-level: differences aggregate every owner observed in the event.
 */
public record QuestShadowComparison(String eventType, List<QuestShadowDifference> differences) {
	public QuestShadowComparison {
		eventType = requireText(eventType, "eventType");
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
}
