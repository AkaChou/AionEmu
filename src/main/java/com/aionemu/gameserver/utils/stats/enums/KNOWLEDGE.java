package com.aionemu.gameserver.utils.stats.enums;

/**
 * 各职业基础知识值枚举。
 * Baseline knowledge values by player class.
 */
public enum KNOWLEDGE {
	WARRIOR(90), GLADIATOR(90), TEMPLAR(90), SCOUT(90), ASSASSIN(90), RANGER(120), MAGE(115), SORCERER(120),
	SPIRIT_MASTER(115), PRIEST(100), CLERIC(105), CHANTER(105),
	// 资讯类 4.3 / News Class 4.3
	TECHNIST(100), GUNSLINGER(100), MUSE(115), SONGWEAVER(110),
	// 资讯类 4.5 / News Class 4.5
	AETHERTECH(90);

	/**
	 * 该职业的基础属性值。
	 * Baseline attribute value for this class.
	 */
	private int value;

	private KNOWLEDGE(int value) {
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
