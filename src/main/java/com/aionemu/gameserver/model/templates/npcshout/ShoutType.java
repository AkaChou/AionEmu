package com.aionemu.gameserver.model.templates.npcshout;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Shout 类型枚举。
 * Shout Type enumeration.
 *
 * @author Rolandas
 */

@XmlType(name = "ShoutType")
@XmlEnum
public enum ShoutType {

	/** 广播 / Broadcast. */
	BROADCAST, SAY, HEAR;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static ShoutType fromValue(String v) {
		return valueOf(v);
	}
}
