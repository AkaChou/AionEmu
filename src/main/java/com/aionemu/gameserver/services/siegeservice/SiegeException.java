package com.aionemu.gameserver.services.siegeservice;

/**
 * 攻城运行时异常，标识攻城流程错误。
 * Siege runtime exception identifying siege flow errors.
 */
public class SiegeException extends RuntimeException {

	private static final long serialVersionUID = 8834569185793190327L;

	public SiegeException() {
	}

	public SiegeException(String message) {
		super(message);
	}

	public SiegeException(String message, Throwable cause) {
		super(message, cause);
	}

	public SiegeException(Throwable cause) {
		super(cause);
	}
}