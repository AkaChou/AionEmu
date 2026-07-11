package com.aionemu.gameserver.world.exceptions;

/**
 * 对象在未 despawn 的情况下被重复生成时抛出。
 * Thrown when an object is spawned more than once without being despawned.
 *
 * @author -Nemesiss-
 */
@SuppressWarnings("serial")
public class AlreadySpawnedException extends RuntimeException {

	/**
	 * 构造无详细消息的异常。
	 * Constructs an {@code AlreadySpawnedException} with no detail message.
	 */
	public AlreadySpawnedException() {
		super();
	}

	/**
	 * 使用指定详细消息构造异常。
	 * Constructs an {@code AlreadySpawnedException} with the specified detail message.
	 *
	 * @param s 详细消息 / the detail message
	 */
	public AlreadySpawnedException(String s) {
		super(s);
	}

	/**
	 * 使用指定消息与原因构造异常。
	 * Constructs an exception with the specified message and cause.
	 *
	 * exception description
	 * @param cause 异常原因 / reason of this exception
	 */
	public AlreadySpawnedException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 使用指定原因构造异常。
	 * Constructs an exception with the specified cause.
	 *
	 * @param cause 异常原因 / reason of this exception
	 */
	public AlreadySpawnedException(Throwable cause) {
		super(cause);
	}
}
