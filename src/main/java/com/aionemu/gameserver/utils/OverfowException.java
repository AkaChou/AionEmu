package com.aionemu.gameserver.utils;

/**
 * 安全数学运算溢出时抛出的错误（类名保留历史拼写 Overfow）。
 * Error thrown when a safe math operation overflows (historical misspelling Overfow is preserved).
 *
 * @author MrPoke
 */
public class OverfowException extends Error {

	private static final long serialVersionUID = 488570750616236378L;

	/**
	 * 使用指定消息创建溢出错误。
	 * Creates an overflow error with the given message.
	 *
	 * Error message
	 */
	public OverfowException(String message) {
		super(message);
	}
}
