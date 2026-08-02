package com.aionemu.gameserver.questEngine.definition;

import java.util.List;
import java.util.Objects;

/** A pure event/condition to mutation transition. */
public record QuestTransition(QuestEvent event, List<QuestCondition> conditions,
		List<QuestAction> actions, String targetNode, List<AfterCommitAction> afterCommit,
		Integer priority, String sourceNode) {
	public QuestTransition(QuestEvent event, List<QuestCondition> conditions, List<QuestAction> actions,
			String targetNode, List<AfterCommitAction> afterCommit, Integer priority) {
		this(event, conditions, actions, targetNode, afterCommit, priority, null);
	}

	public QuestTransition {
		event = Objects.requireNonNull(event, "event");
		conditions = List.copyOf(Objects.requireNonNull(conditions, "conditions"));
		actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
		if (targetNode == null || targetNode.isBlank()) {
			throw new IllegalArgumentException("targetNode must not be blank");
		}
		afterCommit = List.copyOf(Objects.requireNonNull(afterCommit, "afterCommit"));
		if (priority != null && priority < 0) {
			throw new IllegalArgumentException("priority must be non-negative");
		}
		if (sourceNode != null && sourceNode.isBlank()) {
			throw new IllegalArgumentException("sourceNode must not be blank");
		}
	}

	public boolean hasExplicitPriority() {
		return priority != null;
	}
}
