package com.aionemu.gameserver.model.templates.challenge;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 挑战类型枚举。
 * Challenge Type enumeration.
 */

@Getter
@XmlType(name = "ChallengeType")
@XmlEnum
public enum ChallengeType {
	/** 军团。 / Legion. */
	LEGION(1),
	/** 城镇。 / Town. */
	TOWN(2);

	/** 返回 ID / Returns the id */
	private final int id;

	ChallengeType(int id) {
		this.id = id;
	}

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static ChallengeType fromValue(String paramString) {
		return valueOf(paramString);
	}
}
