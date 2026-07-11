package com.aionemu.gameserver.network;

/**
 * 当 {@link Crypt} 的密钥被重复设置时抛出的运行时异常。
 * Runtime exception thrown when {@link Crypt} key is set more than once.
 *
 * @author -Nemesiss-
 */
@SuppressWarnings("serial")
public class KeyAlreadySetException extends RuntimeException {

	/**
	 * 无详情消息的构造。
	 * Constructs with no detail message.
	 */
	public KeyAlreadySetException() {
		super();
	}

	/**
	 * 带详情消息的构造。
	 * Constructs with the specified detail message.
	 *
	 * @param s 详情消息 / detail message
	 */
	public KeyAlreadySetException(String s) {
		super(s);
	}

	/**
	 * 带消息与原因的构造。
	 * Constructs with message and cause.
	 *
	 * exception description
	 * cause
	 */
	public KeyAlreadySetException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 带原因的构造。
	 * Constructs with cause.
	 *
	 * cause
	 */
	public KeyAlreadySetException(Throwable cause) {
		super(cause);
	}
}
