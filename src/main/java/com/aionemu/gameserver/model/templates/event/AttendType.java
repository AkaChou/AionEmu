package com.aionemu.gameserver.model.templates.event;

import jakarta.xml.bind.annotation.XmlEnum;
import lombok.Getter;

/**
 * 签到类型枚举。
 * Attend Type enumeration.
 *
 * @author Ranastic
 */

@XmlEnum
public enum AttendType {
	/** PC 基础 / PC Basic */
	PC_BASIC(0), BASIC(1), ANNIVERSARY(2);

	/** 返回 ID / Returns the id */
	@Getter
	private final int id;

	AttendType(int id) {
		this.id = id;
	}
}
