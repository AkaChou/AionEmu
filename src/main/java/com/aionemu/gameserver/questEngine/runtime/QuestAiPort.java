package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion;

/** Typed boundary for AI commands issued to a quest-spawned NPC after commit. */
public interface QuestAiPort {
	/**
	 * 让 slot 的权威 NPC 跟随玩家 (AIEventType.FOLLOW_ME, 护送)。
	 *
	 * @return true 表示已发出命令; false 表示 slot 无 handle / 玩家离线 / 失败 (best-effort)
	 */
	boolean startFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot);

	/** 让当前权威交互 NPC 跟随玩家到坐标目标。 */
	default boolean startFollowCurrentTargetToPoint(QuestSnapshot snapshot, QuestMutationPlan plan,
			float x, float y, float z) {
		return false;
	}

	/** 让当前权威交互 NPC 跟随玩家到当前世界中的指定 NPC。 */
	default boolean startFollowCurrentTargetToNpc(QuestSnapshot snapshot, QuestMutationPlan plan, int npcId) {
		return false;
	}

	/** 停止 slot 的权威 NPC 跟随玩家 (AIEventType.STOP_FOLLOW_ME)。 */
	boolean stopFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot);

	/** 让 slot 的权威 NPC 攻击事件快照中的 target (objectId 无法解析时跳过)。 */
	boolean attackTarget(QuestSnapshot snapshot, QuestMutationPlan plan, String slot);

	/** Makes a task-owned NPC attack the first matching template in the player's current world instance. */
	default boolean attackNpcTemplate(QuestSnapshot snapshot, QuestMutationPlan plan, String slot,
		int templateId) {
		return false;
	}

	/** 让 slot 的权威 NPC 开始巡逻行走 (WalkManager.startWalking)。 */
	boolean startWalking(QuestSnapshot snapshot, QuestMutationPlan plan, String slot);

	/** Broadcasts one closed-set emotion for the authoritative slot handle. */
	boolean broadcastEmotion(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, QuestNpcEmotion emotion);

	/** Broadcasts one closed-set emote from the authoritative NPC used by the current interaction. */
	default boolean broadcastInteractionEmotion(QuestSnapshot snapshot, QuestMutationPlan plan,
			QuestNpcEmotion emotion) {
		return false;
	}

	/** Starts the quest follow checker for the authoritative slot and destination zone. */
	boolean watchFollowZone(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, String zone);

	/** Starts the quest follow checker for the authoritative slot and destination coordinate. */
	default boolean watchFollowCoordinate(QuestSnapshot snapshot, QuestMutationPlan plan, String slot,
			float x, float y, float z) {
		return false;
	}
}
