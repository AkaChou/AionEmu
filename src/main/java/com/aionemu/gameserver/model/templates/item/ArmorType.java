package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 防具类型：定义所需技能与掩码。
 * Armor type: defines required skills and mask.
 *
 * @author Rinzler (Encom)
 */

@XmlType(name = "armor_type")
@XmlEnum
public enum ArmorType {
	// 防具类型 4.8 / Armor Type 4.8
	/** 无防具 / No Armor */
	NO_ARMOR(new int[] {}),
	/** 锁甲 / Chain */
	CHAIN(new int[] { 42, 49 }),
	/** 布甲 / Clothes */
	CLOTHES(new int[] { 40 }),
	/** 皮甲 / Leather */
	LEATHER(new int[] { 41, 48 }),
	/** 板甲 / Plate */
	PLATE(new int[] { 54 }),
	/** 长袍 / Robe */
	ROBE(new int[] { 103, 106 }),
	/** 碎片 / Shard */
	SHARD(new int[] {}),
	/** 盾牌 / Shield */
	SHIELD(new int[] { 43, 50 }),
	/** 翅膀 / Wing */
	WING(new int[] {}),
	/** 羽毛 / Plume */
	PLUME(new int[] {}),
	/** 手镯 / Bracelet */
	BRACELET(new int[] {});

	private int[] requiredSkills;

	private ArmorType(int[] requiredSkills) {
		this.requiredSkills = requiredSkills;
	}

	/** 返回所需技能 / Returns the required skills */
	public int[] getRequiredSkills() {
		return requiredSkills;
	}

	/** 获取掩码。 / Returns the mask. */
	public int getMask() {
		return 1 << this.ordinal();
	}
}
