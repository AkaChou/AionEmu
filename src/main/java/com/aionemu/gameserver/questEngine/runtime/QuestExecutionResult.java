package com.aionemu.gameserver.questEngine.runtime;

import java.util.List;

/**
 * 一次串行化玩家事件执行的结果。
 * Result of one serialized player event execution.
 */
public record QuestExecutionResult(QuestExecutionStatus status, QuestMutationPlan plan,
		List<RuntimeException> afterCommitFailures) {
	public QuestExecutionResult {
		afterCommitFailures = List.copyOf(afterCommitFailures);
	}
}
