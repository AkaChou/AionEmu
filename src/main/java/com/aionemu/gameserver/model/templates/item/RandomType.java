package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Random 类型枚举。
 * Random Type enumeration.
 */

@XmlEnum
public enum RandomType {
	/** 强化 / Enchantment */
	ENCHANTMENT, MANASTONE, MANASTONE_COMMON_GRADE_10(10), MANASTONE_COMMON_GRADE_20(20), MANASTONE_COMMON_GRADE_30(30),
	/** 魔石普通 40 级 / Manastone Common Grade 40 */
	MANASTONE_COMMON_GRADE_40(40), MANASTONE_COMMON_GRADE_50(50), MANASTONE_COMMON_GRADE_60(60),
	/** 魔石普通 65 级 / Manastone Common Grade 65 */
	MANASTONE_COMMON_GRADE_65(65), MANASTONE_RARE_GRADE_10(10), MANASTONE_RARE_GRADE_20(20),
	/** 魔石稀有 30 级 / Manastone Rare Grade 30 */
	MANASTONE_RARE_GRADE_30(30), MANASTONE_RARE_GRADE_40(40), MANASTONE_RARE_GRADE_50(50), MANASTONE_RARE_GRADE_60(60),
	/** 魔石稀有 65 级 / Manastone Rare Grade 65 */
	MANASTONE_RARE_GRADE_65(65), MANASTONE_LEGEND_GRADE_10(10), MANASTONE_LEGEND_GRADE_20(20),
	/** 魔石传颂 30 级 / Manastone Legend Grade 30 */
	MANASTONE_LEGEND_GRADE_30(30), MANASTONE_LEGEND_GRADE_40(40), MANASTONE_LEGEND_GRADE_50(50),
	/** 魔石传颂 60 级 / Manastone Legend Grade 60 */
	MANASTONE_LEGEND_GRADE_60(60), MANASTONE_LEGEND_GRADE_65(65), ANCIENT_ITEMS, CHUNK_EARTH, CHUNK_ROCK, CHUNK_SAND,
	/** 碎石宝石 / Chunk Gemstone */
	CHUNK_GEMSTONE, SCROLLS, POTION;

	private int level;

	private RandomType() {
	}

	private RandomType(int level) {
		this.level = level;
	}

	/** 获取等级。 / Returns the level. */
	public int getLevel() {
		return level;
	}
}
