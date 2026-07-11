package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 技能类型：物理、魔法或通用。
 * Skill type: physical, magical or all.
 */
@XmlType(name = "skillType")
@XmlEnum
public enum SkillType {

	/** 无 / None */
	NONE,
	/** 物理 / Physical */
	PHYSICAL,
	/** 魔法 / Magical */
	MAGICAL,
	/** 全部 / All */
	ALL;
}
