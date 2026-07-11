package com.aionemu.gameserver.geoEngine.collision;

/**
 * 不支持的碰撞类型异常，当两个 {@link Collidable} 无法做碰撞检测时抛出。
 * Thrown when a pair of {@link Collidable}s cannot be tested for collision.
 *
 * @author Kirill
 */
@SuppressWarnings("serial")
public class UnsupportedCollisionException extends UnsupportedOperationException {

	/**
	 * 以原因构造。
	 * Constructs with a cause.
	 *
	 * cause
	 */
	public UnsupportedCollisionException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * 以消息与原因构造。
	 * Constructs with a message and cause.
	 *
	 * message
	 * cause
	 */
	public UnsupportedCollisionException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * 以消息构造。
	 * Constructs with a message.
	 *
	 * message
	 */
	public UnsupportedCollisionException(String arg0) {
		super(arg0);
	}

	/**
	 * 无参构造。
	 * Default constructor.
	 */
	public UnsupportedCollisionException() {
		super();
	}
}
