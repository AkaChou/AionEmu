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
	/**
	 * 区域类别：虚拟 / 子区域 / 飞行 / 神器 / 要塞 / 限制 / 物品使用 / PvP / 决斗 / 房屋 / 天气。
	 * Zone classes: dummy / sub / fly / artifact / fort / limit / item use / pvp / duel / house / weather.
	 */
	DUMMY, SUB, FLY, ARTIFACT, FORT, LIMIT, ITEM_USE, PVP, DUEL, HOUSE, WEATHER;
}
