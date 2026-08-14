package com.aionemu.gameserver.model.templates.challenge;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 挑战类型枚举。
 * Challenge Type enumeration.
 */

@XmlType(name = "ChallengeType")
@XmlEnum
public enum ChallengeType {
	/** 军团。 / Legion. */
	LEGION(1),
	/** 城镇。 / Town. */
	TOWN(2);

	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	private ChallengeType(int id) {
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
