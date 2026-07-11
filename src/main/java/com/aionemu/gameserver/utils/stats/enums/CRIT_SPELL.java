package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础法术暴击值枚举。
 * Baseline spell critical values by player class.
 */
public enum CRIT_SPELL {
	WARRIOR(0), GLADIATOR(0), TEMPLAR(0), SCOUT(0), ASSASSIN(0), RANGER(0), MAGE(0), SORCERER(0), SPIRIT_MASTER(0),
	PRIEST(0), CLERIC(0), CHANTER(0),
	// 资讯类 4.3 / News Class 4.3
	TECHNIST(0), GUNSLINGER(0), MUSE(0), SONGWEAVER(0),
	// 资讯类 4.5 / News Class 4.5
	AETHERTECH(0);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private CRIT_SPELL(int value) {
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
