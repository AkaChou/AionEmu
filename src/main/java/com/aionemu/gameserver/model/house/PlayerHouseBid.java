package com.aionemu.gameserver.model.house;

import java.sql.Timestamp;

/**
 * 玩家房屋 Bid，用于房屋相关逻辑。
 * Player House Bid for house logic.
 */

public class PlayerHouseBid implements Comparable<PlayerHouseBid> {
	private int playerId;
	private int houseId;
	private long offer;
	private Timestamp time;

	public PlayerHouseBid(int playerId, int houseId, long offer, Timestamp time) {
		this.playerId = playerId;
		this.houseId = houseId;
		this.offer = offer;
		this.time = time;
	}

	/** 返回玩家 ID / Returns the player id */
	public int getPlayerId() {
		return playerId;
	}

	/** 返回 house id / Returns the house id */
	public int getHouseId() {
		return houseId;
	}

	/** 返回 bid offer / Returns the bid offer */
	public long getBidOffer() {
		return offer;
	}

	/** 返回时间 / Returns the time*/
	public Timestamp getTime() {
		return time;
	}

	/** 比较。 / Compares to another instance. */
	@Override
	public int compareTo(PlayerHouseBid o) {
		return (int) (time.getTime() - o.getTime().getTime());
	}
}
