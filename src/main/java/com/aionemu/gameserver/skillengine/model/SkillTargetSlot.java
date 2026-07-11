package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 技能目标槽位：效果占用的 BUFF/DEBUFF 等槽位类别。
 * Skill target slot: buff/debuff/etc. slot class occupied by an effect.
 *
 * @author ATracer
 */
@XmlType(name = "TargetSlot")
@XmlEnum
public enum SkillTargetSlot {

	/** 增益槽 / Buff slot */
	BUFF,
	/** 减益槽 / Debuff slot */
	DEBUFF,
	/** 咏唱/战歌槽 / Chant slot */
	CHANT,
	/** 特殊槽 / Special slot */
	SPEC,
	/** 特殊槽 2 / Special slot 2 */
	SPEC2,
	/** 强化槽 / Boost slot */
	BOOST,
	/** 不显示 / No show */
	NOSHOW,
	/** 无 / None */
	NONE;
}
