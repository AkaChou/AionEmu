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
	ROOF(1, 1),
	/** 外墙 / Outwall. */
	OUTWALL(2, 2),
	/** 框架 / Frame. */
	FRAME(3, 3),
	/** 门 / Door. */
	DOOR(4, 4),
	/** 花园 / Garden. */
	GARDEN(5, 5),
	/** 篱笆 / Fence. */
	FENCE(6, 6),
	/** 内墙（任意）/ Inwall (any). */
	INWALL_ANY(8, 13),
	/** 内地板（任意）/ Infloor (any). */
	INFLOOR_ANY(14, 19),
	/** 附加 / Addon. */
	ADDON(27, 27);

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
