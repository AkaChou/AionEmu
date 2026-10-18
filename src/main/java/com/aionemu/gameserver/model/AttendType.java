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

	/** 周年签到（真端枚举 0）/ Anniversary (retail enum 0) */
	ANNIVERSARY(0),
	/** 基础签到（真端枚举 1）/ Basic (retail enum 1) */
	BASIC(1),
	/** 网吧签到（真端枚举 2）/ PC-bang attendance (retail enum 2) */
	PC_BASIC(2);

	/** 返回 ID（真端 S_LOGIN_EVENT_LIST 下发的类型值）/ Returns the id sent by retail S_LOGIN_EVENT_LIST */
	private final int id;

	AttendType(int id) {
		this.id = id;
	}
}
