package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Armor 类型枚举。
 * Armor Type enumeration.
 *
 * @author Rinzler (Encom)
 */

@XmlType(name = "armor_type")
@XmlEnum
public enum ArmorType {
	// 防具类型 4.8 / Armor Type 4.8
	/** 无防具 / No Armor */
	NO_ARMOR(new int[] {}), CHAIN(new int[] { 42, 49 }), CLOTHES(new int[] { 40 }), LEATHER(new int[] { 41, 48 }),
	/** 板甲 / Plate. */
	PLATE(new int[] { 54 }), ROBE(new int[] { 103, 106 }), SHARD(new int[] {}), SHIELD(new int[] { 43, 50 }),
	/** 翅膀 / Wing. */
	WING(new int[] {}), PLUME(new int[] {}), BRACELET(new int[] {});

	private int[] requiredSkills;

	private ArmorType(int[] requiredSkills) {
		this.requiredSkills = requiredSkills;
	}

	/** 返回 required skills / Returns the required skills */
	public int[] getRequiredSkills() {
		return requiredSkills;
	}

	/** 获取掩码。 / Returns the mask. */
	public int getMask() {
		return 1 << this.ordinal();
	}
}
