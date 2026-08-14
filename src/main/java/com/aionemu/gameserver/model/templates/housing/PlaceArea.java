package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Place 区域枚举。
 * Place Area enumeration.
 *
 * @author Rolandas
 */
@XmlType(name = "PlaceArea")
@XmlEnum
public enum PlaceArea {

	/** 全部 / All. */
	ALL,
	/** 室内 / Interior. */
	INTERIOR,
	/** 室外 / Exterior. */
	EXTERIOR;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value. */
	public static PlaceArea fromValue(String value) {
		return valueOf(value);
	}
}
