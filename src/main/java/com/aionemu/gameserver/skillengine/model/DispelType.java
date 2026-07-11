package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 驱散方式：按效果 ID、类型或槽位匹配驱散目标。
 * Dispel type: match dispel targets by effect id, type or slot.
 *
 * @author ATracer
 */
@XmlType(name = "DispelType")
@XmlEnum
public enum DispelType {

	/** 按效果 ID / By effect id */
	EFFECTID,
	/** 按效果 ID 区间 / By effect id range */
	EFFECTIDRANGE,
	/** 按效果类型 / By effect type */
	EFFECTTYPE,
	/** 按目标槽位 / By target slot type */
	SLOTTYPE;

	/**
	 * 返回枚举名（JAXB 值）。
	 * Returns enum name (JAXB value).
	 *
	 * name
	 */
	public String value() {
		return name();
	}

	/**
	 * 由字符串解析驱散方式。
	 * Parses dispel type from string.
	 *
	 * @param v 名称 / name
	 * dispel type
	 */
	public static DispelType fromValue(String v) {
		return valueOf(v);
	}
}
