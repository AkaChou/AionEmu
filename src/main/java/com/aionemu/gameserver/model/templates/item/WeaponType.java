package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 武器类型：所需技能与占用槽位。
 * Weapon type: required skills and slots.
 *
 * @author Rinzler (Encom)
 */
@XmlType(name = "weapon_type")
@XmlEnum
public enum WeaponType

{
	// 武器类型 4.8 / Weapon Type 4.8
	/** 匕首 1H / Dagger 1H */
	DAGGER_1H(new int[] { 66, 45 }, 1),
	/** 锤 1H / Mace 1H */
	MACE_1H(new int[] { 39, 46 }, 1),
	/** 剑 1H / Sword 1H */
	SWORD_1H(new int[] { 37, 44 }, 1),
	/** 锄头 1H / Toolhoe 1H */
	TOOLHOE_1H(new int[] {}, 1),
	/** 魔法书 2H / Book 2H */
	BOOK_2H(new int[] { 100 }, 2),
	/** 宝珠 2H / Orb 2H */
	ORB_2H(new int[] { 111 }, 2),
	/** 长枪 2H / Polearm 2H */
	POLEARM_2H(new int[] { 52 }, 2),
	/** 法杖 2H / Staff 2H */
	STAFF_2H(new int[] { 89 }, 2),
	/** 双手剑 2H / Sword 2H */
	SWORD_2H(new int[] { 51 }, 2),
	/** 镐 2H / Toolpick 2H */
	TOOLPICK_2H(new int[] {}, 2),
	/** 钓竿 2H / Toolrod 2H */
	TOOLROD_2H(new int[] {}, 2),
	/** 弓 2H / Bow */
	BOW(new int[] { 53 }, 2),
	/** 枪 1H / Gun 1H */
	GUN_1H(new int[] { 117, 112 }, 1),
	/** 炮 2H / Cannon 2H */
	CANNON_2H(new int[] { 113 }, 2),
	/** 竖琴 2H / Harp 2H */
	HARP_2H(new int[] { 124, 114 }, 2),
	/** 钥刃 2H / Keyblade 2H */
	KEYBLADE_2H(new int[] { 115 }, 2),
	/** 钥锤 2H / Keyhammer 2H */
	KEYHAMMER_2H(new int[] {}, 2);

	private int slots;
	private int[] requiredSkill;

	private WeaponType(int[] requiredSkills, int slots) {
		this.requiredSkill = requiredSkills;
		this.slots = slots;
	}

	/** 返回所需技能 / Returns the required skills */
	public int[] getRequiredSkills() {
		return requiredSkill;
	}

	/** 返回所需槽位 / Returns the required slots */
	public int getRequiredSlots() {
		return slots;
	}

	/** 获取掩码。 / Returns the mask. */
	public int getMask() {
		return 1 << this.ordinal();
	}
}
