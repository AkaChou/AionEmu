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
	BED,
	/** 书 / Book. */
	BOOK,
	/** 地毯 / Carpet. */
	CARPET,
	/** 椅子 / Chair. */
	CHAIR,
	/** 窗帘 / Curtain. */
	CURTAIN,
	/** 装饰 / Decoration. */
	DECORATION,
	/** 灯 / Light. */
	LIGHT,
	/** NPC / NPC. */
	NPC,
	/** 室外灯 / Outlight. */
	OUTLIGHT,
	/** 桌子 / Table. */
	TABLE;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value. */
	public static HousingCategory fromValue(String value) {
		return valueOf(value);
	}
}
