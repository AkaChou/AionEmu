package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestPlayerEmotion;

/** Typed boundary for player effects applied after a successful commit. */
public interface QuestEffectPort {
	/** Morphs the player into the ascension form for the given ascension id. */
	boolean morph(QuestSnapshot snapshot, QuestMutationPlan plan, int ascensionId);

	/** Starts a flight teleport for the given route id. */
	boolean flightTeleport(QuestSnapshot snapshot, QuestMutationPlan plan, int flightTeleportId);

	/**
	 * 触发玩家侧任务表情；旧测试端口可以不支持该能力。
	 * Emits a player-side quest emotion. Older test ports may leave this unsupported.
	 */
	default boolean playerEmotion(QuestSnapshot snapshot, QuestMutationPlan plan, QuestPlayerEmotion emotion) {
		throw new UnsupportedOperationException("player emotion is not composed");
	}
}
