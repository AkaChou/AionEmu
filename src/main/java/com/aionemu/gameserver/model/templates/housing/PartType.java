package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Part 类型枚举。
 * Part Type enumeration.
 */

@XmlType(name = "PartType")
@XmlEnum
public enum PartType {
	/** 屋顶 / Roof. */
	ROOF(1, 1), OUTWALL(2, 2), FRAME(3, 3), DOOR(4, 4), GARDEN(5, 5), FENCE(6, 6), INWALL_ANY(8, 13),
	/** Infloor Any / Infloor Any */
	INFLOOR_ANY(14, 19), ADDON(27, 27);

	private int lineNrStart;
	private int lineNrEnd;

	private PartType(int packetLineStart, int packetLineEnd) {
		this.lineNrStart = packetLineStart;
		this.lineNrEnd = packetLineEnd;
	}

	/** 返回 start line nr / Returns the start line nr */
	public int getStartLineNr() {
		return lineNrStart;
	}

	/** 返回 end line nr / Returns the end line nr */
	public int getEndLineNr() {
		return lineNrEnd;
	}

	/** 返回 for line nr / Returns the for line nr */
	public static PartType getForLineNr(int lineNr) {
		for (PartType type : PartType.values()) {
			if (type.getStartLineNr() <= lineNr && type.getEndLineNr() >= lineNr) {
				return type;
			}
		}
		return null;
	}
}
