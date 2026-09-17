package com.aionemu.gameserver.model.templates.event;

import jakarta.xml.bind.annotation.XmlEnum;
import lombok.Getter;

/**
 * 账号类型枚举。
 * Account Type enumeration.
 *
 * @author Rinzler (Encom)
 */

@Getter
@XmlEnum
public enum AccountType {
	/** 新手 / Newbie. */
	NEWBIE(0), RETURN(1), CASH(2), DIAMOND_01(3);

	/** 返回 ID / Returns the id */
	private final int id;

	AccountType(int id) {
		this.id = id;
	}
}
