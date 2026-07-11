package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础敏捷值枚举。
 * Baseline agility values by player class.
 */
public enum AGILITY {
	WARRIOR(100), GLADIATOR(100), TEMPLAR(110), SCOUT(100), ASSASSIN(100), RANGER(100), MAGE(95), SORCERER(100),
	SPIRIT_MASTER(100), PRIEST(100), CLERIC(90), CHANTER(90),
	// 资讯类 4.3 / News Class 4.3
	TECHNIST(110), GUNSLINGER(105), MUSE(100), SONGWEAVER(100),
	// 资讯类 4.5 / News Class 4.5
	AETHERTECH(110);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private AGILITY(int value) {
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
