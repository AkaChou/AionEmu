package com.aionemu.gameserver.world.zone;

import java.util.List;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 区域属性位标志（绑定点、召回、滑翔、飞行、坐骑、PvP、决斗等）。
 * Zone attribute bit-flags (bind, recall, glide, fly, ride, PvP, duel, etc.).
 *
 * @author Rolandas
 */
@XmlType(name = "ZoneAttributes")
@XmlEnum(String.class)
public enum ZoneAttributes {
	/** 允许放置绑定点 / allow placing a bind point (kisk) */
	BIND(1 << 0),
	/** 允许召回 / allow recall */
	RECALL(1 << 1),
	/** 允许滑翔 / allow glide */
	GLIDE(1 << 2),
	/** 允许飞行 / allow fly */
	FLY(1 << 3),
	/** 允许坐骑 / allow ride */
	RIDE(1 << 4),
	/** 允许飞行坐骑 / allow fly-ride */
	FLY_RIDE(1 << 5),

	/** 启用 PvP（仅 PvP 类型区域）/ enable PvP (PvP-type zones only) */
	@XmlEnumValue("PVP")
	PVP_ENABLED(1 << 6), // Only for PvP type zones
	/** 允许同族决斗（仅决斗类型区域）/ allow same-race duels (duel-type zones only) */
	@XmlEnumValue("DUEL_SAME_RACE")
	DUEL_SAME_RACE_ENABLED(1 << 7), // Only for Duel type zones
	/** 允许异族决斗（仅决斗类型区域）/ allow other-race duels (duel-type zones only) */
	@XmlEnumValue("DUEL_OTHER_RACE")
	DUEL_OTHER_RACE_ENABLED(1 << 8); // Only for Duel type zones

	/** 位标志值 / bit-flag value */
	private int id;

	/**
	 * @param id 位标志值 / bit-flag value
	 */
	private ZoneAttributes(int id) {
		this.id = id;
	}

	/**
	 * 返回该属性的位标志值。
	 * Return the bit-flag value of this attribute.
	 *
	 * bit-flag value
	 */
	public int getId() {
		return id;
	}

	/**
	 * 将属性列表合并为整数位掩码。
	 * Merge a list of attributes into an integer bit-mask.
	 *
	 * attribute list
	 *
	 * @param flagValues @return 合并后的位掩码 / combined bit-mask
	 */
	public static Integer fromList(List<ZoneAttributes> flagValues) {
		Integer result = 0;
		for (ZoneAttributes attribute : ZoneAttributes.values()) {
			if (flagValues.contains(attribute)) {
				result |= attribute.getId();
			}
		}
		return result;
	}
}
