package com.aionemu.gameserver.questEngine.runtime;

import java.util.List;

/** Result of one serialized player event execution. */
public record QuestExecutionResult(QuestExecutionStatus status, QuestMutationPlan plan,
		List<RuntimeException> afterCommitFailures) {
	public QuestExecutionResult {
		afterCommitFailures = List.copyOf(afterCommitFailures);
	}
}
