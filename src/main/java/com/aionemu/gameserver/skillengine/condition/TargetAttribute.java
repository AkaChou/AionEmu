package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 目标属性枚举：描述技能条件所要求的目标类型。
 * Target attribute enum: describes the required target type for a skill condition.
 *
 * @author ATracer
 */
@XmlType(name = "TargetAttribute")
@XmlEnum
public enum TargetAttribute {

	/** 仅 NPC / NPC only */
	NPC,
	/** 仅玩家角色 / Player character only */
	PC,
	/** 任意目标 / Any target */
	ALL,
	/** 自身 / Self */
	SELF,
	/** 无目标要求 / No target requirement */
	NONE;

	/**
	 * 返回枚举常量名称。
	 * Returns the enum constant name.
	 *
	 * @return 名称字符串 / name string
	 */
	public String value() {
		return name();
	}

	/**
	 * 由名称字符串解析目标属性。
	 * Parses a target attribute from its name string.
	 *
	 * @param v 名称 / name
	 * @return 目标属性 / target attribute
	 */
	public static TargetAttribute fromValue(String v) {
		return valueOf(v);
	}
}
