package com.aionemu.gameserver.model.templates.mail;

/**
 * 邮件 Formatter 接口。
 * Mail Formatter interface.
 */

public interface IMailFormatter {

	/** 获取类型。 / Returns the type. */
	MailPartType getType();

	/** 返回 formatted string / Returns the formatted string */
	String getFormattedString(MailPartType paramMailPartType);

	/** 返回参数值 / Returns the param value*/
	String getParamValue(String paramString);
}
