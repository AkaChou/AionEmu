package com.aionemu.gameserver.questEngine.runtime;

/** Typed boundary for world-NPC lifecycle actions applied after a successful commit. */
public interface QuestNpcPort {
	/**
	 * Deletes the interaction NPC of this commit (the authoritative
	 * interactionObjectId) and optionally schedules its respawn.
	 *
	 * @return true 表示已删除; false 表示找不到交互 NPC (best-effort, 记录审计)
	 */
	boolean deleteInteractionNpc(QuestSnapshot snapshot, QuestMutationPlan plan, boolean scheduleRespawn);
}
