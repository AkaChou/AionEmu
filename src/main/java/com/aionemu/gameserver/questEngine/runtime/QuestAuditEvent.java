package com.aionemu.gameserver.questEngine.runtime;

import java.util.Objects;

/** Immutable audit record for an owner invocation failure during routing. */
public record QuestAuditEvent(int questId, String eventType, QuestDispatchContract contract,
	QuestRouteResult result, String failureType) {
	public QuestAuditEvent {
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		eventType = requireText(eventType, "eventType");
		contract = Objects.requireNonNull(contract, "contract");
		result = Objects.requireNonNull(result, "result");
		failureType = requireText(failureType, "failureType");
		if (result != QuestRouteResult.FAILED) {
			throw new IllegalArgumentException("audit events must describe a failed owner invocation");
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
