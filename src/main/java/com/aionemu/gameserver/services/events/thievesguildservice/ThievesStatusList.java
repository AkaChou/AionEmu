package com.aionemu.gameserver.services.events.thievesguildservice;

import java.sql.Timestamp;

/**
 * 盗贼状态条目，保存盗贼公会相关玩家状态数据。
 * Thieves status entry holding thieves-guild related player state data.
 *
 * @author Rinzler (Encom)
 */

public class ThievesStatusList {

	private int playerId;
	private int rankId;
	private int thievesCount;
	private Long lastThievesKinah;
	private int prisonCount;
	private String revengeName;
	private int revengeCount;
	private Timestamp revengeDate;

	public ThievesStatusList() {
	}

	public ThievesStatusList(int playerId, int rankId, int thievesCount, Long lastThievesKinah, int prisonCount,
			String revengeName, int revengeCount, Timestamp revengeDate) {
		this.playerId = playerId;
		this.rankId = rankId;
		this.thievesCount = thievesCount;
		this.lastThievesKinah = lastThievesKinah;
		this.prisonCount = prisonCount;
		this.revengeName = revengeName;
		this.revengeCount = revengeCount;
		this.revengeDate = revengeDate;
	}

	public int getPlayerId() {
		return playerId;
	}

	public void setPlayerId(int playerId) {
		this.playerId = playerId;
	}

	public int getRankId() {
		return rankId;
	}

	public void setRankId(int rankId) {
		this.rankId = rankId;
	}

	public int getThievesCount() {
		return thievesCount;
	}

	public void setThievesCount(int thievesCount) {
		this.thievesCount = thievesCount;
	}

	public Long getLastThievesKinah() {
		return lastThievesKinah;
	}

	public void setLastThievesKinah(Long lastThievesKinah) {
		this.lastThievesKinah = lastThievesKinah;
	}

	public int getPrisonCount() {
		return prisonCount;
	}

	public void setPrisonCount(int prisonCount) {
		this.prisonCount = prisonCount;
	}

	public String getRevengeName() {
		return revengeName;
	}

	public void setRevengeName(String revengeName) {
		this.revengeName = revengeName;
	}

	public int getRevengeCount() {
		return revengeCount;
	}

	public void setRevengeCount(int revengeCount) {
		this.revengeCount = revengeCount;
	}

	public Timestamp getRevengeDate() {
		return revengeDate;
	}

	public void setRevengeDate(Timestamp revengeDate) {
		this.revengeDate = revengeDate;
	}
}
