package com.aionemu.gameserver.model.autogroup;

/**
 * AGQuestion 枚举。
 * AG Question enumeration.
 */

public enum AGQuestion {
	/** 失败 / Failed. */
	FAILED, READY, ADDED;

	/** 是否失败 / Whether failed*/
	public boolean isFailed() {
		return this.equals(AGQuestion.FAILED);
	}

	/** 是否就绪。 / Whether Ready. */
	public boolean isReady() {
		return this.equals(AGQuestion.READY);
	}

	/**
	 * @return 是否已添加 / Whether added
	 */
	public boolean isAdded() {
		return this.equals(AGQuestion.ADDED);
	}
}
