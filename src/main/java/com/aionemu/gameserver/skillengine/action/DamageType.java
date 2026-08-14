package com.aionemu.gameserver.skillengine.action;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 伤害类型枚举：物理或魔法。
 * Damage type enum: physical or magical.
 *
 * @author ATracer
 */
@XmlType(name = "DamageType")
@XmlEnum
public enum DamageType {

	PHYSICAL, MAGICAL;

	/**
	 * 返回枚举名字符串。
	 * Returns the enum name as a string.
	 *
	 * @return 枚举名字符串 / enum name
	 */
	public String value() {
		return name();
	}

	/**
	 * 由字符串解析伤害类型。
	 * Parses a damage type from a string value.
	 *
	 * @param v 名称字符串 / name string
	 * @return 匹配的枚举常量 / matching enum constant
	 */
	public static DamageType fromValue(String v) {
		return valueOf(v);
	}
}
