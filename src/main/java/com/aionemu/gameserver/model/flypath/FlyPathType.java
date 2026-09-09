package com.aionemu.gameserver.model.flypath;

import lombok.Getter;

/**
 * 飞行路径类型枚举。
 * Fly Path Type enumeration.
 */

public enum FlyPathType {
	/** 喷泉。 / Geyser. */
	GEYSER(0), ONE_WAY(1), TWO_WAY(2);

	/** 返回 ID。 / Returns the id. */
	@Getter
	private final int id;

	FlyPathType(int id) {
		this.id = id;
	}
}
