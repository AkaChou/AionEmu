package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础移动速度值枚举。
 * Baseline speed values by player class.
 */
public enum SPEED {
	WARRIOR(6), GLADIATOR(6), TEMPLAR(6), SCOUT(6), ASSASSIN(6), RANGER(6), MAGE(6), SORCERER(6), SPIRIT_MASTER(6),
	PRIEST(6), CLERIC(6), CHANTER(6),
	// 资讯类 4.3 / News Class 4.3
	TECHNIST(6), GUNSLINGER(6), MUSE(6), SONGWEAVER(6),
	// 资讯类 4.5 / News Class 4.5
	AETHERTECH(6);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private SPEED(int value) {
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
