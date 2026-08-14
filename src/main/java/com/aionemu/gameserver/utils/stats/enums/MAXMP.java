package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础最大魔法值枚举。
 * Baseline max MP values by player class.
 */
public enum MAXMP {
	WARRIOR(100), GLADIATOR(100), TEMPLAR(100), SCOUT(100), ASSASSIN(100), RANGER(100), MAGE(100), SORCERER(100),
	SPIRIT_MASTER(100), PRIEST(100), CLERIC(100), CHANTER(100),
	// 新职业 4.3 / New Class 4.3
	TECHNIST(100), GUNSLINGER(100), MUSE(100), SONGWEAVER(100),
	// 新职业 4.5 / New Class 4.5
	AETHERTECH(100);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private MAXMP(int value) {
		this.value = value;
	}

	/**
	 * 获取该职业的基础属性值。
	 * Returns the baseline attribute value for this class.
	 *
	 * @return 基础属性值 / baseline attribute value
	 */
	public int getValue() {
		return value;
	}
}
