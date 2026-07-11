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

	/**
	 * getPlayerId 方法。
	 * getPlayerId method.
	 * result
	 */
	public int getPlayerId() {
		return playerId;
	}

	/**
	 * setPlayerId 方法。
	 * setPlayerId method.
	 *
	 * playerId
	 */
	public void setPlayerId(int playerId) {
		this.playerId = playerId;
	}

	/**
	 * getRankId 方法。
	 * getRankId method.
	 * result
	 */
	public int getRankId() {
		return rankId;
	}

	/**
	 * setRankId 方法。
	 * setRankId method.
	 *
	 * rankId
	 */
	public void setRankId(int rankId) {
		this.rankId = rankId;
	}

	/**
	 * getThievesCount 方法。
	 * getThievesCount method.
	 * result
	 */
	public int getThievesCount() {
		return thievesCount;
	}

	/**
	 * setThievesCount 方法。
	 * setThievesCount method.
	 *
	 * thievesCount
	 */
	public void setThievesCount(int thievesCount) {
		this.thievesCount = thievesCount;
	}

	/**
	 * getLastThievesKinah 方法。
	 * getLastThievesKinah method.
	 * result
	 */
	public Long getLastThievesKinah() {
		return lastThievesKinah;
	}

	/**
	 * setLastThievesKinah 方法。
	 * setLastThievesKinah method.
	 *
	 * @param lastThievesKinah 上次盗贼基纳 / lastThievesKinah
	 */
	public void setLastThievesKinah(Long lastThievesKinah) {
		this.lastThievesKinah = lastThievesKinah;
	}

	/**
	 * getPrisonCount 方法。
	 * getPrisonCount method.
	 * result
	 */
	public int getPrisonCount() {
		return prisonCount;
	}

	/**
	 * setPrisonCount 方法。
	 * setPrisonCount method.
	 *
	 * prisonCount
	 */
	public void setPrisonCount(int prisonCount) {
		this.prisonCount = prisonCount;
	}

	/**
	 * getRevengeName 方法。
	 * getRevengeName method.
	 * result
	 */
	public String getRevengeName() {
		return revengeName;
	}

	/**
	 * setRevengeName 方法。
	 * setRevengeName method.
	 *
	 * @param revengeName 复仇目标名 / revengeName
	 */
	public void setRevengeName(String revengeName) {
		this.revengeName = revengeName;
	}

	/**
	 * getRevengeCount 方法。
	 * getRevengeCount method.
	 * result
	 */
	public int getRevengeCount() {
		return revengeCount;
	}

	/**
	 * setRevengeCount 方法。
	 * setRevengeCount method.
	 *
	 * revengeCount
	 */
	public void setRevengeCount(int revengeCount) {
		this.revengeCount = revengeCount;
	}

	/**
	 * getRevengeDate 方法。
	 * getRevengeDate method.
	 * result
	 */
	public Timestamp getRevengeDate() {
		return revengeDate;
	}

	/**
	 * setRevengeDate 方法。
	 * setRevengeDate method.
	 *
	 * revengeDate
	 */
	public void setRevengeDate(Timestamp revengeDate) {
		this.revengeDate = revengeDate;
	}
}