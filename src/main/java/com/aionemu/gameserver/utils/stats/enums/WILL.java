package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础意志值枚举。
 * Baseline will values by player class.
 */
public enum WILL {
	WARRIOR(90), GLADIATOR(90), TEMPLAR(105), SCOUT(90), ASSASSIN(90), RANGER(110), MAGE(115), SORCERER(110),
	SPIRIT_MASTER(115), PRIEST(110), CLERIC(110), CHANTER(110),
	// 新职业 4.3 / New Class 4.3
	TECHNIST(90), GUNSLINGER(100), MUSE(110), SONGWEAVER(110),
	// 新职业 4.5 / New Class 4.5
	AETHERTECH(90);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private WILL(int value) {
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
