package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础飞行速度值枚举。
 * Baseline fly speed values by player class.
 */
public enum FLY_SPEED {
	WARRIOR(9), GLADIATOR(9), TEMPLAR(9), SCOUT(9), ASSASSIN(9), RANGER(9), MAGE(9), SORCERER(9), SPIRIT_MASTER(9),
	PRIEST(9), CLERIC(9), CHANTER(9),
	// 新职业 4.3 / New Class 4.3
	TECHNIST(9), GUNSLINGER(9), MUSE(9), SONGWEAVER(9),
	// 新职业 4.5 / New Class 4.5
	AETHERTECH(9);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private FLY_SPEED(int value) {
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
