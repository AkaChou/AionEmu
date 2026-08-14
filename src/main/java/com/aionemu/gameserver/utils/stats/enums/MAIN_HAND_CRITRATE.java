package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础主手暴击值枚举。
 * Baseline main-hand crit rate values by player class.
 */
public enum MAIN_HAND_CRITRATE {
	WARRIOR(2), GLADIATOR(2), TEMPLAR(2), SCOUT(3), ASSASSIN(3), RANGER(3), MAGE(1), SORCERER(2), SPIRIT_MASTER(2),
	PRIEST(2), CLERIC(2), CHANTER(1),
	// 新职业 4.3 / New Class 4.3
	TECHNIST(2), GUNSLINGER(2), MUSE(1), SONGWEAVER(2),
	// 新职业 4.5 / New Class 4.5
	AETHERTECH(3);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private MAIN_HAND_CRITRATE(int value) {
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
