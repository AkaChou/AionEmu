package com.aionemu.gameserver.model.gameobjects.player;

import lombok.Getter;

/**
 * 玩家房屋 OwnerFlags 枚举。
 * Player House Owner Flags enumeration.
 */

@Getter
public enum PlayerHouseOwnerFlags {
	/** 所有者 / Is Owner*/
	IS_OWNER(1 << 0), HAS_OWNER(1 << 0), BUY_STUDIO_ALLOWED(1 << 1), SINGLE_HOUSE(1 << 1), BIDDING_ALLOWED(1 << 2),
	/** 房屋所有者 / House Owner */
	HOUSE_OWNER((IS_OWNER.getId() | BIDDING_ALLOWED.getId()) & ~BUY_STUDIO_ALLOWED.getId()),
	/** Selling 房屋 / Selling House */
	SELLING_HOUSE(IS_OWNER.getId() | BUY_STUDIO_ALLOWED.getId()),

	// 玩家状态 / Player Status
	/** Sold 房屋 / Sold House */
	SOLD_HOUSE(BIDDING_ALLOWED.getId() | BUY_STUDIO_ALLOWED.getId());

	/** 返回 ID / Returns the id */
	private final byte id;

	PlayerHouseOwnerFlags(int id) {
		this.id = (byte) (id & 0xFF);
	}
}
