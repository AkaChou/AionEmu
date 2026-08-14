package com.aionemu.gameserver.world.exceptions;

/**
 * 同一 AionObject 被重复存储时抛出，通常表示严重错误。
 * Thrown when an AionObject is stored more than once; indicates a serious error.
 *
 * @author -Nemesiss-
 */
@SuppressWarnings("serial")
public class DuplicateAionObjectException extends RuntimeException {

	/**
	 * 构造无详细消息的异常。
	 * Constructs a {@code DuplicateAionObjectException} with no detail message.
	 */
	public DuplicateAionObjectException() {
		super();
	}

	/**
	 * 使用指定详细消息构造异常。
	 * Constructs a {@code DuplicateAionObjectException} with the specified detail message.
	 *
	 * @param s 详细消息 / the detail message
	 */
	public DuplicateAionObjectException(String s) {
		super(s);
	}

	/**
	 * 使用指定消息与原因构造异常。
	 * Constructs an exception with the specified message and cause.
	 *
	 * @param message 异常描述 / exception description
	 * @param cause 异常原因 / reason of this exception
	 */
	public DuplicateAionObjectException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 使用指定原因构造异常。
	 * Constructs an exception with the specified cause.
	 *
	 * @param cause 异常原因 / reason of this exception
	 */
	public DuplicateAionObjectException(Throwable cause) {
		super(cause);
	}
}
