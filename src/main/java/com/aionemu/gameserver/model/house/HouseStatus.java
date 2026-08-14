package com.aionemu.gameserver.model.house;

/**
 * 房屋状态枚举。
 * House Status enumeration.
 */

public enum HouseStatus {
	/** 激活 / Active */
	ACTIVE,
	/** 未激活 / Inactive */
	INACTIVE,
	/** 等待出售 / Waiting for sale */
	SELL_WAIT,
	/** 不销售 / Not for sale */
	NOSALE;
}
