package com.aionemu.gameserver.world.exceptions;

/**
 * 引用了当前不存在的副本实例时抛出。
 * Thrown when an object references an instance that does not currently exist.
 *
 * @author -Nemesiss-
 */
@SuppressWarnings("serial")
public class InstanceNotExistException extends RuntimeException {

	/**
	 * 构造无详细消息的异常。
	 * Constructs an {@code InstanceNotExistException} with no detail message.
	 */
	public InstanceNotExistException() {
		super();
	}

	/**
	 * 使用指定详细消息构造异常。
	 * Constructs an {@code InstanceNotExistException} with the specified detail message.
	 *
	 * @param s 详细消息 / the detail message
	 */
	public InstanceNotExistException(String s) {
		super(s);
	}
}
