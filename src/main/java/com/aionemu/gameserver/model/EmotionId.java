package com.aionemu.gameserver.model;

/**
 * 表情 ID 枚举。
 * Emotion Id enumeration.
 */

public enum EmotionId {
	/** 无 / None. */
	NONE(0), LAUGH(1), ANGRY(2), SAD(3), POINT(5), YES(6), NO(7), VICTORY(8), CLAP(11), SIGH(12), SURPRISE(13),
	/** 舒适 / Comfort. */
	COMFORT(14), THANK(15), BEG(16), BLUSH(17), SMILE(28), SALUTE(29), PANIC(30), SORRY(31), THINK(33), DISLIKE(34),
	/** 站立 / Stand. */
	STAND(128), CASH_GOOD_DAY_FULL(133), CASH_U_AND_ME_FULL(134);

	private int id;

	private EmotionId(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int id() {
		return id;
	}
}
