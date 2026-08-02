package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;

/** Best-effort post-commit protocol/domain refresh for the committed quest projection. */
public interface QuestStateSyncPort {
	boolean sync(QuestSnapshot snapshot, QuestMutationPlan plan, QuestStateSyncMode mode);
}
