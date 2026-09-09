package com.aionemu.gameserver.model.house;

import java.sql.Timestamp;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 玩家房屋 Bid，用于房屋相关逻辑。
 * Player House Bid for house logic.
 */

@AllArgsConstructor
public class PlayerHouseBid implements Comparable<PlayerHouseBid> {
	@Getter
	private final int playerId;
	@Getter
	private final int houseId;
	private final long offer;
	@Getter
	private final Timestamp time;

	/** 返回 bid offer / Returns the bid offer */
	public long getBidOffer() {
		return offer;
	}

	/** 比较。 / Compares to another instance. */
	@Override
	public int compareTo(PlayerHouseBid o) {
		return (int) (time.getTime() - o.getTime().getTime());
	}
}
