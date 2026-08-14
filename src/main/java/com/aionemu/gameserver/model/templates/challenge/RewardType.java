package com.aionemu.gameserver.model.templates.challenge;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 奖励类型枚举。
 * Reward Type enumeration.
 */

@XmlType(name = "RewardType")
@XmlEnum
public enum RewardType {
	/** 无 / None. */
	NONE,
	/** 积分 / Point. */
	POINT,
	/** 生成 / Spawn. */
	SPAWN;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static RewardType fromValue(String paramString) {
		return valueOf(paramString);
	}
}
