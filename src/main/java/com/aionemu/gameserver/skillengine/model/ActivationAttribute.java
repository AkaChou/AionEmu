package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 技能激活属性：定义技能如何被触发与维持。
 * Skill activation attribute: how a skill is triggered and maintained.
 */
@XmlType(name = "activationAttribute")
@XmlEnum
public enum ActivationAttribute {

	/** 无 / None */
	NONE,
	/** 主动施放 / Active cast */
	ACTIVE,
	/** 被激怒/反击触发 / Provoked (counter) */
	PROVOKED,
	/** 维持型 / Maintain */
	MAINTAIN,
	/** 切换型（开关） / Toggle */
	TOGGLE,
	/** 被动 / Passive */
	PASSIVE,
	/** 充能 / Charge */
	CHARGE;
}
