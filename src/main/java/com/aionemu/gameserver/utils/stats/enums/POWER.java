package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础力量值枚举。
 * Baseline power values by player class.
 */
public enum POWER {
	WARRIOR(110), GLADIATOR(110), TEMPLAR(110), SCOUT(100), ASSASSIN(110), RANGER(90), MAGE(90), SORCERER(90),
	SPIRIT_MASTER(90), PRIEST(95), CLERIC(105), CHANTER(110),

	// 资讯类 4.3 / News Class 4.3
	TECHNIST(100), // GOOD
	GUNSLINGER(100), // GOOD
	MUSE(95), // GOOD
	SONGWEAVER(90), // 95?
	AETHERTECH(110); // 115?

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private POWER(int value) {
		this.value = value;
	}

	/**
	 * 获取该职业的基础属性值。
	 * Returns the baseline attribute value for this class.
	 *
	 * Attribute value
	 */
	public int getValue() {
		return value;
	}
}
