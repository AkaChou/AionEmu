package com.aionemu.gameserver.model.house;

import java.sql.Timestamp;


/**
 * 玩家房屋 Bid，用于房屋相关逻辑。
 * Player House Bid for house logic.
 */

public record PlayerHouseBid(int playerId, int houseId, long offer,
							 Timestamp time) implements Comparable<PlayerHouseBid> {
	/**
	 * 返回 bid offer / Returns the bid offer
	 */
	public long getBidOffer() {
		return offer;
	}

	/**
	 * 比较。 / Compares to another instance.
	 */
	@Override
	public int compareTo(PlayerHouseBid o) {
		return (int) (time.getTime() - o.time().getTime());
	}
}
