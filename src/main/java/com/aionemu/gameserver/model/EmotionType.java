package com.aionemu.gameserver.model;

/**
 * 表情类型枚举。
 * Emotion Type enumeration.
 */

public enum EmotionType {
	/** 未知 / Unk. */
	UNK(-1),
	/** 选择目标 / Select Target */
	SELECT_TARGET(0),
	/** 跳跃 / Jump */
	JUMP(1),
	/** 坐下 / Sit */
	SIT(2),
	/** 站立 / Stand */
	STAND(3),
	/** 椅子上坐下 / Chair Sit */
	CHAIR_SIT(4),
	/** 从椅子起身 / Chair Up */
	CHAIR_UP(5),
	/** 开始飞行传送 / Start Flyteleport */
	START_FLYTELEPORT(6),
	/** 着陆飞行传送 / Land Flyteleport */
	LAND_FLYTELEPORT(7),
	/** 风道 / Windstream */
	WINDSTREAM(8),
	/** 风道结束 / Windstream End */
	WINDSTREAM_END(9),
	/** 退出风道 / Windstream Exit */
	WINDSTREAM_EXIT(10),
	/** 风道开始加速 / Windstream Start Boost */
	WINDSTREAM_START_BOOST(11),
	/** 风道结束加速 / Windstream End Boost*/
	WINDSTREAM_END_BOOST(12),
	/** 飞行 / Fly */
	FLY(13),
	/** 着陆 / Land */
	LAND(14),
	/** 骑乘 / Ride */
	RIDE(15),
	/** 下骑 / Ride End */
	RIDE_END(16),
	/** 死亡 / Die */
	DIE(18),
	/** 复活 / Resurrect */
	RESURRECT(19),
	/** 黄金竞技场未知 / Arena Of Tenacity Unk */
	ARENA_OF_TENACITY_UNK(20), // 5.3
	/** 表情 / Emote. */
	EMOTE(21),
	/** 结束表情（决斗） / End of a emote (duel) */
	END_DUEL(22),
	/** 攻击模式 / Attackmode. */
	ATTACKMODE(24), // 游戏发起的攻击模式 / Attack mode, by game
	/** 中立模式 / Neutralmode. */
	NEUTRALMODE(25), // 游戏发起的中立模式 / Neutral mode, by game
	/** 行走 / Walk. */
	WALK(26),
	/** 奔跑 / Run */
	RUN(27),
	/** 开门 / Open Door */
	OPEN_DOOR(31),
	/** 关门 / Close Door */
	CLOSE_DOOR(32),
	/** 打开个人商店 / Open Private Shop */
	OPEN_PRIVATESHOP(33),
	/** 关闭个人商店 / Close Private Shop */
	CLOSE_PRIVATESHOP(34),
	/** NPC 攻击模式后触发的表情（非「表情」） / Not "emote"; triggered after Attack Mode of npcs */
	START_EMOTE2(35),
	/** 启动能量碎片 / Powershard On */
	POWERSHARD_ON(36),
	/** 关闭能量碎片 / Powershard Off */
	POWERSHARD_OFF(37),
	/** 玩家切换的攻击模式 / Attack toggled by player */
	ATTACKMODE2(38),
	/** 玩家切换的中立模式2 / Neutral toggled by player */
	NEUTRALMODE2(39),
	/** 开始拾取 / Start Loot*/
	START_LOOT(40),
	/** 结束拾取 / End Loot */
	END_LOOT(41),
	/** 开始任务拾取 / Start Quest Loot */
	START_QUESTLOOT(42),
	/** 结束任务拾取 / End Quest Loot */
	END_QUESTLOOT(43),
	/** 滑翔 / Gliding */
	GLIDING(46),
	/** 结束滑翔 / Gliding End */
	GLIDING_END(47),
	/** 开始喂食 / Start Feeding */
	START_FEEDING(50),
	/** 结束喂食 / End Feeding */
	END_FEEDING(51),
	/** 风道侧移 / Windstream Strafe*/
	WINDSTREAM_STRAFE(52),
	/** 开始疾跑 / Start Sprint */
	START_SPRINT(53),
	/** 结束疾跑 / End Sprint */
	END_SPRINT(54),
	/** 开始翱翔加速 / Start Soar Speed */
	START_SOAR_SPEED(55),
	/** 结束翱翔加速 / End Soar Speed */
	END_SOAR_SPEED(56),
	/** 宠物依偎 / Pet Snuggle */
	PET_SNUGGLE(114),
	/** 宠物表情2 / Pet Emotion 2 */
	PET_EMOTION_2(121),
	/** 宠物表情3 / Pet Emotion 3 */
	PET_EMOTION_3(122),
	/** 宠物表情4 / Pet Emotion 4 */
	PET_EMOTION_4(123);

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
