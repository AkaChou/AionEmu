package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion;

/** 提交后向任务刷出的 NPC 发出 AI 命令的类型化边界。 / Typed boundary for AI commands issued to a quest-spawned NPC after commit. */
public interface QuestAiPort {
	/**
	 * 让 slot 的权威 NPC 跟随玩家（AIEventType.FOLLOW_ME，护送）。
	 * Makes the slot's authoritative NPC follow the player (AIEventType.FOLLOW_ME, escort).
	 *
	 * @return true 表示已发出命令；false 表示 slot 无 handle / 玩家离线 / 失败（best-effort） / true if issued; false if the slot has no handle, the player is offline, or it failed (best-effort)
	 */
	boolean startFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot);

	/** 让当前权威交互 NPC 跟随玩家到坐标目标。 / Makes the current authoritative interaction NPC follow the player to a coordinate target. */
	default boolean startFollowCurrentTargetToPoint(QuestSnapshot snapshot, QuestMutationPlan plan,
			float x, float y, float z) {
		return false;
	}

	/** 让当前权威交互 NPC 跟随玩家到当前世界中的指定 NPC。 / Makes the current authoritative interaction NPC follow the player to a given NPC in the current world. */
	default boolean startFollowCurrentTargetToNpc(QuestSnapshot snapshot, QuestMutationPlan plan, int npcId) {
		return false;
	}

	/** 停止 slot 的权威 NPC 跟随玩家（AIEventType.STOP_FOLLOW_ME）。 / Stops the slot's authoritative NPC from following the player (AIEventType.STOP_FOLLOW_ME). */
	boolean stopFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot);

	/** 让 slot 的权威 NPC 攻击事件快照中的 target（objectId 无法解析时跳过）。 / Makes the slot's authoritative NPC attack the target in the event snapshot (skipped when the objectId cannot be resolved). */
	boolean attackTarget(QuestSnapshot snapshot, QuestMutationPlan plan, String slot);

	/** 让任务持有的 NPC 攻击玩家当前世界实例中第一个匹配模板。 / Makes a task-owned NPC attack the first matching template in the player's current world instance. */
	default boolean attackNpcTemplate(QuestSnapshot snapshot, QuestMutationPlan plan, String slot,
		int templateId) {
		return false;
	}

	/** 让 slot 的权威 NPC 开始巡逻行走（WalkManager.startWalking）。 / Makes the slot's authoritative NPC start patrol walking (WalkManager.startWalking). */
	boolean startWalking(QuestSnapshot snapshot, QuestMutationPlan plan, String slot);

	/** 为权威 slot 句柄广播一个封闭集合内的表情。 / Broadcasts one closed-set emotion for the authoritative slot handle. */
	boolean broadcastEmotion(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, QuestNpcEmotion emotion);

	/** 从当前交互所用的权威 NPC 广播一个封闭集合内的表情。 / Broadcasts one closed-set emote from the authoritative NPC used by the current interaction. */
	default boolean broadcastInteractionEmotion(QuestSnapshot snapshot, QuestMutationPlan plan,
			QuestNpcEmotion emotion) {
		return false;
	}

	/** 为权威 slot 与目标区域启动任务跟随检查器。 / Starts the quest follow checker for the authoritative slot and destination zone. */
	boolean watchFollowZone(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, String zone);

	/** 为权威 slot 与目标坐标启动任务跟随检查器。 / Starts the quest follow checker for the authoritative slot and destination coordinate. */
	default boolean watchFollowCoordinate(QuestSnapshot snapshot, QuestMutationPlan plan, String slot,
			float x, float y, float z) {
		return false;
	}

	/** 监视本次攻击的常驻 NPC 依靠战斗仇恨被诱导到坐标，不切换为跟随 AI。 */
	default boolean watchLuredNpcCoordinate(QuestSnapshot snapshot, QuestMutationPlan plan,
			float x, float y, float z, float radius) {
		return false;
	}
}
