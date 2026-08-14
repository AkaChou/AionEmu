package com.aionemu.gameserver.model.templates.item.bonuses;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 属性加成类型枚举。
 * Stat Bonus Type enumeration.
 *
 * @author Ranastic
 */
@XmlType(name = "StatBonusType")
@XmlEnum
public enum StatBonusType {

	/** 背包。 / Inventory. */
	INVENTORY, POLISH;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 按值解析枚举项 / From Value */
	public static StatBonusType fromValue(String v) {
		return valueOf(v);
	}
}
