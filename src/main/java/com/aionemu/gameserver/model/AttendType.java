package com.aionemu.gameserver.model;

import jakarta.xml.bind.annotation.XmlEnum;
import lombok.Getter;

/**
 * 签到类型枚举。
 * Attend Type enumeration.
 * @author Alcapwnd
 */
@Getter
@XmlEnum
public enum AttendType {

	/** 无 / None. */
	NONE(0),
	/** 基础签到 / Basic */
	BASIC(1),
	/** 周年签到 / Anniversary */
	ANNIVERSARY(2);

	/** 返回 ID / Returns the id */
	private final int id;

	AttendType(int id) {
		this.id = id;
	}

	/** 按 ID 返回登录类型 / Returns the login type by id */
	public static AttendType getLoginTypeById(int id) {
		for (AttendType attendType : values()) {
			if (attendType.getId() == id) {
				return attendType;
			}
		}
		return AttendType.NONE;
	}
}
