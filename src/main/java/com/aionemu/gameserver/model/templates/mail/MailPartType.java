package com.aionemu.gameserver.model.templates.mail;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 邮件 Part 类型枚举。
 * Mail Part Type enumeration.
 */

@XmlType(name = "MailPartType")
@XmlEnum
public enum MailPartType {
	/** 自定义。 / Custom. */
	CUSTOM, SENDER, TITLE, HEADER, BODY, TAIL;

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static MailPartType fromValue(String v) {
		return valueOf(v);
	}
}
