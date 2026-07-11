package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 攻击类型：用于条件/效果匹配命中来源。
 * Attack type: used by conditions/effects to match hit source.
 *
 * @author Sippolo
 */
@XmlType(name = "attackType")
@XmlEnum
public enum AttackType {

	/** 每次命中 / Every hit */
	EVERYHIT,
	/** 物理技能 / Physical skill */
	PHYSICAL_SKILL,
	/** 魔法技能 / Magical skill */
	MAGICAL_SKILL,
	/** 全部技能 / All skills */
	ALL_SKILL;
}
