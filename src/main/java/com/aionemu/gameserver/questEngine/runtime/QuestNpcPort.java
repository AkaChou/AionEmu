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

	/**
	 * Deletes all NPCs in the player's current authoritative world-map instance.
	 *
	 * @return true when the player's current world instance was available and the cleanup was applied
	 */
	default boolean deleteWorldNpcs(QuestSnapshot snapshot, QuestMutationPlan plan) {
		throw new UnsupportedOperationException("deleteWorldNpcs is not composed");
	}

	/**
	 * 将任务玩家造成的伤害/仇恨添加到指定模板的附近 NPC。
	 * Adds damage/aggro from the quest player to nearby NPCs of a template.
	 */
	default boolean addNpcAggro(QuestSnapshot snapshot, QuestMutationPlan plan, int npcTemplateId, int damage) {
		throw new UnsupportedOperationException("NPC aggro is not composed");
	}
}
