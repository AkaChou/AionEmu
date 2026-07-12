package com.aionemu.gameserver.model.house;

import java.sql.Timestamp;
import lombok.Getter;

/**
 * 玩家房屋 Bid，用于房屋相关逻辑。
 * Player House Bid for house logic.
 */

public class PlayerHouseBid implements Comparable<PlayerHouseBid> {
	@Getter
	private int playerId;
	@Getter
	private int houseId;
	private long offer;
	@Getter
	private Timestamp time;

	public PlayerHouseBid(int playerId, int houseId, long offer, Timestamp time) {
		this.playerId = playerId;
		this.houseId = houseId;
		this.offer = offer;
		this.time = time;
	}

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
