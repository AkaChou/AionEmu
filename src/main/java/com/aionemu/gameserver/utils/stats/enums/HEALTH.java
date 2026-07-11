package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础体质值枚举。
 * Baseline health values by player class.
 */
public enum HEALTH {
	WARRIOR(110), GLADIATOR(115), TEMPLAR(100), SCOUT(100), ASSASSIN(100), RANGER(90), MAGE(90), SORCERER(90),
	SPIRIT_MASTER(90), PRIEST(95), CLERIC(110), CHANTER(105),

	// 资讯类 4.3 / News Class 4.3
	TECHNIST(100), GUNSLINGER(105), MUSE(95), SONGWEAVER(100),
	// 资讯类 4.5 / News Class 4.5
	AETHERTECH(100);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private HEALTH(int value) {
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
