package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Equip 类型枚举。
 * Equip Type enumeration.
 *
 * @author ATracer
 */
@XmlType(name = "equipType")
@XmlEnum
public enum EquipType {

	/** 防具 / Armor. */
	ARMOR, WEAPON, STIGMA, ESTIMA, ACCESSORY, NONE;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static EquipType fromValue(String v) {
		return valueOf(v);
	}
}
