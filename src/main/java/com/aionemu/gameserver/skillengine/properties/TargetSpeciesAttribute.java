package com.aionemu.gameserver.skillengine.properties;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 目标物种属性：全部/玩家/NPC 等物种筛选。
 * Target species attribute: all/PC/NPC species filters.
 */
@XmlType(name = "TargetSpeciesAttribute")
@XmlEnum
public enum TargetSpeciesAttribute {

	NONE, ALL, PC, NPC;

	/**
	 * 返回枚举名字符串。
	 * Returns the enum name as a string.
	 *
	 * enum name
	 */
	public String value() {
		return name();
	}

	/**
	 * 由字符串解析目标物种。
	 * Parses a target species from a string value.
	 *
	 * @param v 名称字符串 / name string
	 * matching enum constant
	 */
	public static TargetSpeciesAttribute fromValue(String v) {
		return valueOf(v);
	}
}
