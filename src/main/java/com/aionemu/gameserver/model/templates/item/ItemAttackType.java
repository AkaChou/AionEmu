package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;

import com.aionemu.gameserver.model.SkillElement;

/**
 * 物品 Attack 类型枚举。
 * Item Attack Type enumeration.
 *
 * @author ATracer
 */
@XmlEnum
public enum ItemAttackType {
	/** 物理 / Physical. */
	PHYSICAL(false, SkillElement.NONE), MAGICAL_EARTH(true, SkillElement.EARTH),
	/** 魔法水 / Magical Water*/
	MAGICAL_WATER(true, SkillElement.WATER), MAGICAL_WIND(true, SkillElement.WIND),
	/** 魔法火 / Magical Fire*/
	MAGICAL_FIRE(true, SkillElement.FIRE);

	private boolean magic;
	private SkillElement elem;

	private ItemAttackType(boolean magic, SkillElement elem) {
		this.magic = magic;
		this.elem = elem;
	}

	/**
	 * @return Whether magical / Whether magical
	 */
	public boolean isMagical() {
		return magic;
	}

	/** 返回 magical element / Returns the magical element */
	public SkillElement getMagicalElement() {
		return elem;
	}
}
