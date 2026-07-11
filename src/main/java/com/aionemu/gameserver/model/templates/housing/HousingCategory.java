package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 住房分类枚举。
 * Housing Category enumeration.
 *
 * @author Rolandas
 */
@XmlType(name = "HousingObjectType")
@XmlEnum
public enum HousingCategory {

	/** 床 / Bed. */
	BED, BOOK, CARPET, CHAIR, CURTAIN, DECORATION, LIGHT, NPC, OUTLIGHT, TABLE;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static HousingCategory fromValue(String value) {
		return valueOf(value);
	}
}
