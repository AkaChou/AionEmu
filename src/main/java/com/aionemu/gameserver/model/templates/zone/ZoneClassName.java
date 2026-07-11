package com.aionemu.gameserver.model.templates.zone;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 区域职业名称枚举。
 * Zone Class Name enumeration.
 */

@XmlType(name = "ZoneClassName")
@XmlEnum
public enum ZoneClassName {
	/** 虚拟 / Dummy. */
	DUMMY, SUB, FLY, ARTIFACT, FORT, LIMIT, ITEM_USE, PVP, DUEL, HOUSE, WEATHER;
}
