package com.aionemu.gameserver.questEngine.runtime;

import java.util.Objects;

/** 失败或降级的类型化任务拥有者调用的不可变结构化审计记录。 / Immutable structured audit record for a failed or degraded typed-owner invocation. */
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

	public String actionType() {
		for (Throwable current = failure; current != null && current.getCause() != current; current = current.getCause()) {
			if (current instanceof QuestAfterCommitException afterCommit) {
				return afterCommit.actionType();
			}
		}
		return "unknown";
	}

	public String rootFailureType() {
		return rootFailure().getClass().getName();
	}

	public String rootFailureMessage() {
		String message = rootFailure().getMessage();
		return message == null ? "" : message;
	}

	private Throwable rootFailure() {
		Throwable root = failure;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		return root;
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}
}
