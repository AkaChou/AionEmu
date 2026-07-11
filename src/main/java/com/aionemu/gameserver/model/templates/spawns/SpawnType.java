package com.aionemu.gameserver.model.templates.spawns;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 刷新点类型枚举。
 * Spawn Type enumeration.
 */

@XmlType(name = "SpawnType")
@XmlEnum
public enum SpawnType {
	/** 管理器。 / Manager. */
	MANAGER, TELEPORT, SIGN;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static SpawnType fromValue(String v) {
		return valueOf(v);
	}
}
