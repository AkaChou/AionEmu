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
	MANAGER,
	/** 传送点 / Teleport */
	TELEPORT,
	/** 标示 / Sign */
	SIGN;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 按值返回 / From Value*/
	public static SpawnType fromValue(String v) {
		return valueOf(v);
	}
}
