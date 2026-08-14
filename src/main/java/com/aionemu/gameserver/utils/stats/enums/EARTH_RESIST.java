package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础地抗值枚举。
 * Baseline earth resist values by player class.
 */
public enum EARTH_RESIST {
	WARRIOR(0), GLADIATOR(0), TEMPLAR(0), SCOUT(0), ASSASSIN(0), RANGER(0), MAGE(0), SORCERER(0), SPIRIT_MASTER(0),
	PRIEST(0), CLERIC(0), CHANTER(0),
	// 新职业 4.3 / New Class 4.3
	TECHNIST(0), GUNSLINGER(0), MUSE(0), SONGWEAVER(0),
	// 新职业 4.5 / New Class 4.5
	AETHERTECH(0);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private EARTH_RESIST(int value) {
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
