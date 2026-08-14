package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;

/** 仅在数据库提交后调用的 best-effort 协议边界。 / Best-effort protocol boundary invoked only after the database commit. */
public interface QuestAfterCommitPort {
	void execute(AfterCommitAction action, QuestSnapshot snapshot, QuestMutationPlan plan);
}
