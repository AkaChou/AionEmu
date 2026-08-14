package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 物品类型枚举。
 * Item Type enumeration.
 *
 * @author Wakizashi
 */
@XmlType(name = "item_type")
@XmlEnum
public enum ItemType {
	/** 普通 / Normal */
	NORMAL,
	/** 欧比斯 / Abyss */
	ABYSS,
	/** 龙族 / Draconic */
	DRACONIC,
	/** 圣灵守护者 / Devanion */
	DEVANION,
	/** 传颂 / Legend */
	LEGEND;
}
