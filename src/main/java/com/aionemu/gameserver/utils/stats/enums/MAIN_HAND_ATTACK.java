package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础主手攻击值枚举。
 * Baseline main-hand attack values by player class.
 */
public enum MAIN_HAND_ATTACK {
	WARRIOR(20), GLADIATOR(20), TEMPLAR(20), SCOUT(20), ASSASSIN(20), RANGER(18), MAGE(14), SORCERER(14),
	SPIRIT_MASTER(14), PRIEST(18), CLERIC(18), CHANTER(20),
	// 资讯类 4.3 / News Class 4.3
	TECHNIST(18), GUNSLINGER(18), MUSE(14), SONGWEAVER(14),
	// 资讯类 4.5 / News Class 4.5
	AETHERTECH(20);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private MAIN_HAND_ATTACK(int value) {
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
