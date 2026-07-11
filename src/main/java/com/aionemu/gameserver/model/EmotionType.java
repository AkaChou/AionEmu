package com.aionemu.gameserver.model;

/**
 * 表情类型枚举。
 * Emotion Type enumeration.
 */

public enum EmotionType {
	/** 未知 / Unk. */
	UNK(-1), SELECT_TARGET(0), JUMP(1), SIT(2), STAND(3), CHAIR_SIT(4), CHAIR_UP(5), START_FLYTELEPORT(6),
	/** 着陆飞行传送 / Land Flyteleport */
	LAND_FLYTELEPORT(7), WINDSTREAM(8), WINDSTREAM_END(9), WINDSTREAM_EXIT(10), WINDSTREAM_START_BOOST(11),
	/** 风道结束加速 / Windstream End Boost*/
	WINDSTREAM_END_BOOST(12), FLY(13), LAND(14), RIDE(15), RIDE_END(16), DIE(18), RESURRECT(19),
	/** 黄金竞技场未知 / Arena Of Tenacity Unk */
	ARENA_OF_TENACITY_UNK(20), // 5.3
	/** 表情 / Emote. */
	EMOTE(21), END_DUEL(22), // What? Duel? It's the end of a emote
	/** 攻击模式 / Attackmode. */
	ATTACKMODE(24), // Attack mode, by game
	/** 中立模式 / Neutralmode. */
	NEUTRALMODE(25), // Attack mode, by game
	/** 行走 / Walk. */
	WALK(26), RUN(27), OPEN_DOOR(31), CLOSE_DOOR(32), OPEN_PRIVATESHOP(33), CLOSE_PRIVATESHOP(34), START_EMOTE2(35), // It's
																														// 否 / not
																														// “表情”。 / "emote".
																														// 已触发 / Triggered
																														// 之后 / after
																														// 攻击 / Attack
																														// 模式 / Mode
																														// of
																														// NPC / npcs
	/** 启动能量碎片 / Powershard On */
	POWERSHARD_ON(36), POWERSHARD_OFF(37), ATTACKMODE2(38), // It's the Attack toggled by player
	/** 中立模式2 / Neutralmode2 */
	NEUTRALMODE2(39), // It's Neutral toggled by player
	/** 开始拾取 / Start Loot*/
	START_LOOT(40), END_LOOT(41), START_QUESTLOOT(42), END_QUESTLOOT(43), GLIDING(46), GLIDING_END(47),
	/** 开始喂食 / Start Feeding */
	START_FEEDING(50), END_FEEDING(51),
	/** 风道侧移 / Windstream Strafe*/
	WINDSTREAM_STRAFE(52), START_SPRINT(53), END_SPRINT(54), START_SOAR_SPEED(55), END_SOAR_SPEED(56), PET_SNUGGLE(114),
	/** 宠物表情2 / Pet Emotion 2 */
	PET_EMOTION_2(121), PET_EMOTION_3(122), PET_EMOTION_4(123);

	private int id;

	private EmotionType(int id) {
		this.id = id;
	}

	/** 返回类型 ID / Returns the type id */
	public int getTypeId() {
		return id;
	}

	/** 按 ID 返回表情类型 / Returns the emotion type by id */
	public static EmotionType getEmotionTypeById(int id) {
		for (EmotionType emotionType : values()) {
			if (emotionType.getTypeId() == id) {
				return emotionType;
			}
		}
		return UNK;
	}
}
