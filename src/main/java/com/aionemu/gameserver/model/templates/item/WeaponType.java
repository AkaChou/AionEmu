package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Weapon 类型枚举。
 * Weapon Type enumeration.
 *
 * @author Rinzler (Encom)
 */
@XmlType(name = "weapon_type")
@XmlEnum
public enum WeaponType

{
	// 武器类型 4.8 / Weapon Type 4.8
	/** Dagger 1H / Dagger 1H */
	DAGGER_1H(new int[] { 66, 45 }, 1), MACE_1H(new int[] { 39, 46 }, 1), SWORD_1H(new int[] { 37, 44 }, 1),
	/** Toolhoe 1H / Toolhoe 1H */
	TOOLHOE_1H(new int[] {}, 1), BOOK_2H(new int[] { 100 }, 2), ORB_2H(new int[] { 111 }, 2),
	/** Polearm 2H / Polearm 2H */
	POLEARM_2H(new int[] { 52 }, 2), STAFF_2H(new int[] { 89 }, 2), SWORD_2H(new int[] { 51 }, 2),
	/** Toolpick 2H / Toolpick 2H */
	TOOLPICK_2H(new int[] {}, 2), TOOLROD_2H(new int[] {}, 2), BOW(new int[] { 53 }, 2),
	/** Gun 1H / Gun 1H */
	GUN_1H(new int[] { 117, 112 }, 1), CANNON_2H(new int[] { 113 }, 2), HARP_2H(new int[] { 124, 114 }, 2),
	/** Keyblade 2H / Keyblade 2H */
	KEYBLADE_2H(new int[] { 115 }, 2), KEYHAMMER_2H(new int[] {}, 2);

	private int slots;
	private int[] requiredSkill;

	private WeaponType(int[] requiredSkills, int slots) {
		this.requiredSkill = requiredSkills;
		this.slots = slots;
	}

	/** 返回 required skills / Returns the required skills */
	public int[] getRequiredSkills() {
		return requiredSkill;
	}

	/** 返回 required slots / Returns the required slots */
	public int getRequiredSlots() {
		return slots;
	}

	/** 获取掩码。 / Returns the mask. */
	public int getMask() {
		return 1 << this.ordinal();
	}
}
