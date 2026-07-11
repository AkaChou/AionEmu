package com.aionemu.gameserver.world.exceptions;

/**
 * 未设置位置的对象被生成或回收时抛出，通常表示调用方遗漏了坐标赋值。
 * Thrown when an object without a set position is spawned or despawned;
 * indicates the caller forgot to assign coordinates.
 *
 * @author -Nemesiss-
 */
@SuppressWarnings("serial")
public class NotSetPositionException extends RuntimeException {

	/**
	 * 构造无详细消息的异常。
	 * Constructs a {@code NotSetPositionException} with no detail message.
	 */
	public NotSetPositionException() {
		super();
	}

	/**
	 * 使用指定详细消息构造异常。
	 * Constructs a {@code NotSetPositionException} with the specified detail message.
	 *
	 * @param s 详细消息 / the detail message
	 */
	public NotSetPositionException(String s) {
		super(s);
	}

	/**
	 * 使用指定消息与原因构造异常。
	 * Constructs an exception with the specified message and cause.
	 *
	 * exception description
	 * @param cause 异常原因 / reason of this exception
	 */
	public NotSetPositionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 使用指定原因构造异常。
	 * Constructs an exception with the specified cause.
	 *
	 * @param cause 异常原因 / reason of this exception
	 */
	public NotSetPositionException(Throwable cause) {
		super(cause);
	}
}
