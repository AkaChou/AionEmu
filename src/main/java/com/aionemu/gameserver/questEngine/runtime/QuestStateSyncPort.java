package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;

/** 已提交任务投影的 best-effort 提交后协议/领域刷新。 / Best-effort post-commit protocol/domain refresh for the committed quest projection. */
public interface QuestStateSyncPort {
	boolean sync(QuestSnapshot snapshot, QuestMutationPlan plan, QuestStateSyncMode mode);
}
