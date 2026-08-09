package com.aionemu.gameserver.questEngine.runtime;

import java.util.Objects;

/** Immutable structured audit record for a failed or degraded typed-owner invocation. */
public record QuestAuditEvent(int questId, String eventType, QuestDispatchContract contract,
	QuestRouteResult result, String sourceNode, String targetNode, int npcId, int dialogId,
	QuestFailureStage failureStage, boolean committed, Throwable failure) {
	public QuestAuditEvent {
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		eventType = requireText(eventType, "eventType");
		contract = Objects.requireNonNull(contract, "contract");
		result = Objects.requireNonNull(result, "result");
		sourceNode = sourceNode == null ? "*" : sourceNode;
		targetNode = requireText(targetNode, "targetNode");
		if (npcId < 0) {
			throw new IllegalArgumentException("npcId must be non-negative");
		}
		failureStage = Objects.requireNonNull(failureStage, "failureStage");
		failure = Objects.requireNonNull(failure, "failure");
	}

	public String failureType() {
		return failure.getClass().getName();
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}
}
