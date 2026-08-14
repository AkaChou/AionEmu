package com.aionemu.gameserver.utils.idfactory;

/**
 * ID 工厂抛出的致命错误。
 * Fatal error thrown by the ID factory.
 *
 * @author SoulKeeper
 */
@SuppressWarnings("serial")
public class IDFactoryError extends Error {

	/**
	 * 无消息构造。
	 * No-arg constructor.
	 */
	public IDFactoryError() {

	}

	/**
	 * 带消息构造。
	 * Construct with a message.
	 *
	 * @param message 错误消息 / Error message
	 */
	public IDFactoryError(String message) {
		super(message);
	}

	/**
	 * 带消息与原因构造。
	 * Construct with a message and cause.
	 *
	 * @param message 错误消息 / Error message
	 * @param cause 原因 / Cause
	 */
	public IDFactoryError(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 带原因构造。
	 * Construct with a cause.
	 *
	 * @param cause 原因 / Cause
	 */
	public IDFactoryError(Throwable cause) {
		super(cause);
	}
}
