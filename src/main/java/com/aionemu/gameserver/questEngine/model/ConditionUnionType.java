package com.aionemu.gameserver.questEngine.model;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * 任务条件组合类型，决定多个子条件如何合并求值。
 * Quest condition union type that decides how multiple sub-conditions are combined.
 *
 * @author Mr. Poke
 */
@XmlEnum
public enum ConditionUnionType {

	/** 全部子条件为真时成立。 True when all sub-conditions hold. */
	AND,
	/** 任一子条件为真时成立。 True when any sub-condition holds. */
	OR;

	/**
	 * 返回枚举名称字符串（JAXB 序列化用）。
	 * Returns the enum name string (for JAXB serialization).
	 *
	 * @return 组合类型名称 / Union type name
	 */
	public String value() {
		return name();
	}

	/**
	 * 根据字符串解析条件组合类型。
	 * Parses a condition union type from its string name.
	 *
	 * @param v 组合类型名称 / Union type name
	 * @return 对应的枚举常量 / Matching enum constant
	 */
	public static ConditionUnionType fromValue(String v) {
		return valueOf(v);
	}
}
