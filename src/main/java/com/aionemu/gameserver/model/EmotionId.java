package com.aionemu.gameserver.model;

/**
 * 表情 ID 枚举。
 * Emotion Id enumeration.
 */

public enum EmotionId {
	/** 无 / None. */
	NONE(0),
	/** 大笑 / Laugh */
	LAUGH(1),
	/** 生气 / Angry */
	ANGRY(2),
	/** 悲伤 / Sad */
	SAD(3),
	/** 指 / Point */
	POINT(5),
	/** 同意 / Yes */
	YES(6),
	/** 拒绝 / No */
	NO(7),
	/** 胜利 / Victory */
	VICTORY(8),
	/** 鼓掌 / Clap */
	CLAP(11),
	/** 叹气 / Sigh */
	SIGH(12),
	/** 惊讶 / Surprise */
	SURPRISE(13),
	/** 舒适 / Comfort. */
	COMFORT(14),
	/** 感谢 / Thank */
	THANK(15),
	/** 乞求 / Beg */
	BEG(16),
	/** 脸红 / Blush */
	BLUSH(17),
	/** 微笑 / Smile */
	SMILE(28),
	/** 敬礼 / Salute */
	SALUTE(29),
	/** 恐慌 / Panic */
	PANIC(30),
	/** 道歉 / Sorry */
	SORRY(31),
	/** 思考 / Think */
	THINK(33),
	/** 讨厌 / Dislike */
	DISLIKE(34),
	/** 站立 / Stand. */
	STAND(128),
	/** 现金表情：美好的一天（完整） / Cash: Good Day (full) */
	CASH_GOOD_DAY_FULL(133),
	/** 现金表情：你和我（完整） / Cash: You and Me (full) */
	CASH_U_AND_ME_FULL(134);

	private int id;

	private EmotionId(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int id() {
		return id;
	}
}
