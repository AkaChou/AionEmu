package com.aionemu.gameserver.questEngine.runtime;

/** 成功提交后应用的世界 NPC 生命周期动作类型化边界。 / Typed boundary for world-NPC lifecycle actions applied after a successful commit. */
public interface QuestNpcPort {
	/**
	 * 删除本次提交的交互 NPC（权威 interactionObjectId），并可选安排其重生。
	 * Deletes the interaction NPC of this commit (the authoritative
	 * interactionObjectId) and optionally schedules its respawn.
	 *
	 * @return true 表示已删除；false 表示找不到交互 NPC（best-effort，记录审计） / true if deleted; false if the interaction NPC was not found (best-effort, audited)
	 */
	boolean deleteInteractionNpc(QuestSnapshot snapshot, QuestMutationPlan plan, boolean scheduleRespawn);

	/**
	 * 删除玩家当前权威世界地图实例中的所有 NPC。
	 * Deletes all NPCs in the player's current authoritative world-map instance.
	 *
	 * @return 玩家当前世界实例可用且清理已应用时为 true / true when the player's current world instance was available and the cleanup was applied
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
