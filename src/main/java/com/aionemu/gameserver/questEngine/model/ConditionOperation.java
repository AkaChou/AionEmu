package com.aionemu.gameserver.questEngine.model;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * 任务条件比较操作符枚举，用于 XML 条件表达式中的数值/集合比较。
 * Quest condition comparison operators used in XML condition expressions for value/set comparisons.
 *
 * @author Mr. Poke
 */
@XmlEnum
public enum ConditionOperation {

	/** 等于。 Equal to. */
	EQUAL,
	/** 大于。 Greater than. */
	GREATER,
	/** 大于等于。 Greater than or equal to. */
	GREATER_EQUAL,
	/** 小于。 Less than. */
	LESSER,
	/** 小于等于。 Less than or equal to. */
	LESSER_EQUAL,
	/** 不等于。 Not equal to. */
	NOT_EQUAL,
	/** 属于集合。 In set. */
	IN,
	/** 不属于集合。 Not in set. */
	NOT_IN;

	/**
	 * 返回枚举名称字符串（JAXB 序列化用）。
	 * Returns the enum name string (for JAXB serialization).
	 *
	 * @return 操作符名称 / Operator name
	 */
	public String value() {
		return name();
	}

	/**
	 * 根据字符串解析条件操作符。
	 * Parses a condition operator from its string name.
	 *
	 * @param v 操作符名称 / Operator name
	 * @return 对应的枚举常量 / Matching enum constant
	 */
	public static ConditionOperation fromValue(String v) {
		return valueOf(v);
	}
}
