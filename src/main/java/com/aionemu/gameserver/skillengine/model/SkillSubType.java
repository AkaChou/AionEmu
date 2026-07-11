package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 技能子类型：攻击、治疗、增益、召唤等功能分类。
 * Skill sub type: attack, heal, buff, summon and other functional classes.
 *
 * @author ATracer
 */
@XmlType(name = "skillSubType")
@XmlEnum
public enum SkillSubType {

	/** 无 / None */
	NONE,
	/** 攻击 / Attack */
	ATTACK,
	/** 咏唱/战歌 / Chant */
	CHANT,
	/** 治疗 / Heal */
	HEAL,
	/** 增益 / Buff */
	BUFF,
	/** 减益 / Debuff */
	DEBUFF,
	/** 召唤 / Summon */
	SUMMON,
	/** 召唤追踪体 / Summon homing */
	SUMMONHOMING,
	/** 召唤陷阱 / Summon trap */
	SUMMONTRAP;
}
