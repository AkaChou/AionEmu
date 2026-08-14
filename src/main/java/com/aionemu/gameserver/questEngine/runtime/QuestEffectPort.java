package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.definition.QuestPlayerEmotion;

/** 成功提交后应用到玩家效果的类型化边界。 / Typed boundary for player effects applied after a successful commit. */
public interface QuestEffectPort {
	/** 将玩家变为指定升天 ID 的升天形态。 / Morphs the player into the ascension form for the given ascension id. */
	boolean morph(QuestSnapshot snapshot, QuestMutationPlan plan, int ascensionId);

	/** 为指定路线 ID 启动飞行传送。 / Starts a flight teleport for the given route id. */
	boolean flightTeleport(QuestSnapshot snapshot, QuestMutationPlan plan, int flightTeleportId);

	/** 任务提交成功后更改玩家的具体进阶职业。 / Changes the player's concrete advanced class after a successful quest commit. */
	default boolean setPlayerClass(QuestSnapshot snapshot, QuestMutationPlan plan, PlayerClass playerClass) {
		throw new UnsupportedOperationException("player class change is not composed");
	}

	/** 任务状态提交后启动 NPC 阵营任务生命周期。 / Starts the NPC-faction quest lifecycle after the quest state commits. */
	default boolean startNpcFactionQuest(QuestSnapshot snapshot, QuestMutationPlan plan, int npcFactionId) {
		throw new UnsupportedOperationException("NPC faction quest start is not composed");
	}

	/** 任务状态提交后完成 NPC 阵营任务生命周期。 / Completes the NPC-faction quest lifecycle after the quest state commits. */
	default boolean completeNpcFactionQuest(QuestSnapshot snapshot, QuestMutationPlan plan, int npcFactionId) {
		throw new UnsupportedOperationException("NPC faction quest completion is not composed");
	}

	/** 在显式放弃转换后中止 NPC 阵营任务生命周期。 / Aborts the NPC-faction quest lifecycle after an explicit abandon transition. */
	default boolean abortNpcFactionQuest(QuestSnapshot snapshot, QuestMutationPlan plan, int npcFactionId) {
		throw new UnsupportedOperationException("NPC faction quest abort is not composed");
	}

	/** 向任务拥有者应用技能效果。 / Applies a skill effect to the quest owner. */
	default boolean applyEffect(QuestSnapshot snapshot, QuestMutationPlan plan, int skillId, int durationMillis) {
		throw new UnsupportedOperationException("apply effect is not composed");
	}

	/** 按效果 ID 从任务拥有者移除效果。 / Removes an effect from the quest owner by effect id. */
	default boolean removeEffect(QuestSnapshot snapshot, QuestMutationPlan plan, int effectId) {
		throw new UnsupportedOperationException("remove effect is not composed");
	}

	/**
	 * 触发玩家侧任务表情；旧测试端口可以不支持该能力。
	 * Emits a player-side quest emotion. Older test ports may leave this unsupported.
	 */
	default boolean playerEmotion(QuestSnapshot snapshot, QuestMutationPlan plan, QuestPlayerEmotion emotion) {
		throw new UnsupportedOperationException("player emotion is not composed");
	}
}
