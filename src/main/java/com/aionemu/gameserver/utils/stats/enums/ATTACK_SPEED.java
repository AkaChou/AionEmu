package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础攻击速度值枚举。
 * Baseline attack speed values by player class.
 */
public enum ATTACK_SPEED {
	WARRIOR(1500), GLADIATOR(1500), TEMPLAR(1500), SCOUT(1500), ASSASSIN(1500), RANGER(1500), MAGE(1500),
	SORCERER(1500), SPIRIT_MASTER(1500), PRIEST(1500), CLERIC(1500), CHANTER(1500),
	// 新职业 4.3 / New Class 4.3
	TECHNIST(1500), GUNSLINGER(1500), MUSE(1500), SONGWEAVER(1500),
	// 新职业 4.5 / New Class 4.5
	AETHERTECH(1500);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private ATTACK_SPEED(int value) {
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
