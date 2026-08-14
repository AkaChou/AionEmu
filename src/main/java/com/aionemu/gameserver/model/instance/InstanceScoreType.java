package com.aionemu.gameserver.model.instance;

/**
 * 副本 Score 类型枚举。
 * Instance Score Type enumeration.
 */

public enum InstanceScoreType {
	/** 准备中 / Preparing. */
	PREPARING(1 * 1024 * 1024),
	/** 开始进行 / Start progress. */
	START_PROGRESS(2 * 1024 * 1024),
	/** 结束进行 / End progress. */
	END_PROGRESS(3 * 1024 * 1024);

	private int id;

	private InstanceScoreType(int id) {
		this.id = id;
	}

	/** 返回 ID。 / Returns the id. */
	public int getId() {
		return id;
	}

	/**
	 * @return 是否准备中 / Whether preparing
	 */
	public boolean isPreparing() {
		return id == 1048576;
	}

	/**
	 * @return 是否开始进行 / Whether start progress
	 */
	public boolean isStartProgress() {
		return id == 2097152;
	}

	/**
	 * @return 是否结束进行 / Whether end progress
	 */
	public boolean isEndProgress() {
		return id == 3145728;
	}
}
