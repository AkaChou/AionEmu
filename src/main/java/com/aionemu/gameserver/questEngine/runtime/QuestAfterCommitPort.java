package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;

/** Best-effort protocol boundary invoked only after the database commit. */
public interface QuestAfterCommitPort {
	void execute(AfterCommitAction action, QuestSnapshot snapshot, QuestMutationPlan plan);
}
