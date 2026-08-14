package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Place 位置枚举。
 * Place Location enumeration.
 *
 * @author Rolandas
 */
@XmlType(name = "PlaceLocation")
@XmlEnum
public enum PlaceLocation {

	/** 地板 / Floor. */
	FLOOR,
	/** 叠放 / Stack. */
	STACK,
	/** 墙面 / Wall. */
	WALL;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value. */
	public static PlaceLocation fromValue(String value) {
		return valueOf(value);
	}
}
