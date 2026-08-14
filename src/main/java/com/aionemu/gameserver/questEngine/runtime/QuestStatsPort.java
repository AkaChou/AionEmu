package com.aionemu.gameserver.questEngine.runtime;

/** 提交后的 best-effort 玩家属性协议刷新。 / Best-effort post-commit player-stat protocol refresh. */
public interface QuestStatsPort {
	boolean refresh(QuestSnapshot snapshot, QuestMutationPlan plan);
}
