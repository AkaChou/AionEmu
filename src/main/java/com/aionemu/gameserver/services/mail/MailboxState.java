package com.aionemu.gameserver.services.mail;

/**
 * 邮箱状态常量，定义邮箱打开/关闭等状态位。
 * Mailbox state constants defining open/closed mailbox flags.
 */
public class MailboxState {
	/** 关闭 / Closed */
	public static final byte CLOSED = 0;
	/** 普通邮箱 / Regular mailbox */
	public static final byte REGULAR = 1;
	/** 急件邮箱 / Express mailbox */
	public static final byte EXPRESS = 2;
}