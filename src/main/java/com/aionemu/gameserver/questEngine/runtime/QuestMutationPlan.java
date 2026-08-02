package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.List;

/** Frozen required mutations and post-commit protocol actions. */
public record QuestMutationPlan(int questId, QuestStatus nextStatus, int nextPackedVariables,
		List<QuestAction> requiredActions, List<AfterCommitAction> afterCommit) {
	public QuestMutationPlan {
		requiredActions = List.copyOf(requiredActions);
		afterCommit = List.copyOf(afterCommit);
	}
}
