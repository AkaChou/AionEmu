package com.aionemu.gameserver.model.templates.mail;

/**
 * 邮件 Formatter 接口。
 * Mail Formatter interface.
 */

public abstract interface IMailFormatter {

	/** 获取类型。 / Returns the type. */
	public abstract MailPartType getType();

	/** 返回 formatted string / Returns the formatted string */
	public abstract String getFormattedString(MailPartType paramMailPartType);

	/** 返回参数值 / Returns the param value*/
	public abstract String getParamValue(String paramString);
}
