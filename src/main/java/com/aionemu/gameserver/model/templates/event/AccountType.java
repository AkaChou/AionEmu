package com.aionemu.gameserver.model.templates.event;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * 账号类型枚举。
 * Account Type enumeration.
 *
 * @author Rinzler (Encom)
 */

@XmlEnum
public enum AccountType {
	/** 新手 / Newbie. */
	NEWBIE(0), RETURN(1), CASH(2), DIAMOND_01(3);

	private int id;

	private AccountType(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
