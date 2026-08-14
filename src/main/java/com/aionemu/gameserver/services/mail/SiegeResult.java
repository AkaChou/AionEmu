package com.aionemu.gameserver.services.mail;

/**
 * 攻城结果枚举，标识攻城邮件结算结果。
 * Siege result enum identifying siege mail settlement outcomes.
 */
public enum SiegeResult {
	/** 防守成功 / Defence. */
	DEFENCE(0),
	/** 占领成功 / Occupy. */
	OCCUPY(1),
	/** 保护成功 / Protect. */
	PROTECT(2),
	/** 防守者 / Defender. */
	DEFENDER(3),
	/** 空 / Empty. */
	EMPTY(4),
	/** 失败 / Fail. */
	FAIL(5);

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