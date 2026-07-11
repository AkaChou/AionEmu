package com.aionemu.gameserver;

/**
 * 游戏服务器严重错误的超类；用于不可恢复的启动或运行时故障。
 * Superclass of GameServer errors used for non-recoverable startup or runtime failures.
 *
 * @author Aquanox
 */
public class GameServerError extends Error {

	private static final long serialVersionUID = -7445873741878754767L;

	/**
	 * 构造无详情消息的错误；原因未初始化，之后可通过 {@link #initCause} 设置。
	 * Constructs a new error with {@code null} as its detail message. The cause is not initialized
	 * and may subsequently be initialized by a call to {@link #initCause}.
	 */
	public GameServerError() {
	}

	/**
	 * 以指定原因为构造参数；详情消息通常取 {@code cause.toString()}。
	 * Constructs a new error with the specified cause and a detail message of
	 * {@code (cause==null ? null : cause.toString())} (which typically contains the class and detail
	 * message of {@code cause}). Useful for errors that are little more than wrappers for other throwables.
	 *
	 * @param cause 原因，可为 null / the cause (may be {@code null})
	 */
	public GameServerError(Throwable cause) {
		super(cause);
	}

	/**
	 * 以指定详情消息构造错误；原因未初始化。
	 * Constructs a new error with the specified detail message. The cause is not initialized
	 * and may subsequently be initialized by a call to {@link #initCause}.
	 *
	 * 详情消息 / the detail message
	 */
	public GameServerError(String message) {
		super(message);
	}

	/**
	 * 以指定详情消息与原因构造错误；原因消息不会自动并入本错误消息。
	 * Constructs a new error with the specified detail message and cause.
	 * Note that the detail message associated with {@code cause} is <i>not</i>
	 * automatically incorporated in this error's detail message.
	 *
	 * 详情消息 / the detail message
	 * @param cause 原因，可为 null / the cause (may be {@code null})
	 */
	public GameServerError(String message, Throwable cause) {
		super(message, cause);
	}
}
