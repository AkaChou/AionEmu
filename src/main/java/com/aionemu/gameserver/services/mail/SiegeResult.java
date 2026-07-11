package com.aionemu.gameserver.services.mail;

/**
 * 攻城结果枚举，标识攻城邮件结算结果。
 * Siege result enum identifying siege mail settlement outcomes.
 */
public enum SiegeResult {
	DEFENCE(0), OCCUPY(1), PROTECT(2), DEFENDER(3), EMPTY(4), FAIL(5);

	private int value;

	private SiegeResult(int value) {
		this.value = value;
	}

	/**
	 * getId 方法。
	 * getId method.
	 * result
	 */
	public int getId() {
		return this.value;
	}
}