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
	/** 物理 / Physical */
	PHYSICAL(false, SkillElement.NONE),
	/** 魔法地 / Magical Earth */
	MAGICAL_EARTH(true, SkillElement.EARTH),
	/** 魔法水 / Magical Water */
	MAGICAL_WATER(true, SkillElement.WATER),
	/** 魔法风 / Magical Wind */
	MAGICAL_WIND(true, SkillElement.WIND),
	/** 魔法火 / Magical Fire */
	MAGICAL_FIRE(true, SkillElement.FIRE);

	private boolean magic;
	private SkillElement elem;

	private ItemAttackType(boolean magic, SkillElement elem) {
		this.magic = magic;
		this.elem = elem;
	}

	/**
	 * 是否为魔法攻击。
	 * Whether this attack type is magical.
	 *
	 * @return 是否魔法 / Whether magical
	 */
	public boolean isMagical() {
		return magic;
	}

	/** 返回魔法元素 / Returns the magical element */
	public SkillElement getMagicalElement() {
		return elem;
	}
}
