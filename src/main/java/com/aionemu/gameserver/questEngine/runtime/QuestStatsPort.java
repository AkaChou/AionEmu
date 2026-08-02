package com.aionemu.gameserver.questEngine.runtime;

/** Best-effort post-commit player-stat protocol refresh. */
public interface QuestStatsPort {
	boolean refresh(QuestSnapshot snapshot, QuestMutationPlan plan);
}
