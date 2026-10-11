package com.aionemu.gameserver.network;

/**
 * 当 {@link Crypt} 的密钥被重复设置时抛出的运行时异常。
 * Runtime exception thrown when {@link Crypt} key is set more than once.
 * @author -Nemesiss-
 */
public class KeyAlreadySetException extends RuntimeException {

	/**
	 * 无详情消息的构造。
	 * Constructs with no detail message.
	 */
	public KeyAlreadySetException() {
		super();
	}
}
