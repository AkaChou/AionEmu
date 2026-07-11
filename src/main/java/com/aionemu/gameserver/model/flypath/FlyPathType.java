package com.aionemu.gameserver.model.flypath;

/**
 * 飞行路径类型枚举。
 * Fly Path Type enumeration.
 */

public enum FlyPathType {
	/** 喷泉 / Geyser. */
	GEYSER(0), ONE_WAY(1), TWO_WAY(2);

	private int id;

	private FlyPathType(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
