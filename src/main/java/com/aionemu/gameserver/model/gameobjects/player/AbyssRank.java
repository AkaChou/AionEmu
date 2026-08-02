package com.aionemu.gameserver.model.gameobjects.player;

import java.util.Calendar;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 欧比斯军阶游戏对象。
 * Abyss Rank game object.
 */

@Slf4j
public class AbyssRank {

	// AP
	private int dailyAP;
	private int weeklyAP;
	private int currentAp;
	// GP
	private int dailyGP;
	private int weeklyGP;
	private int currentGp;
	private AbyssRankEnum rank;
	private int topRanking;
	private PersistentState persistentState;
	private int dailyKill;
	private int weeklyKill;
	private int allKill;
	private int maxRank;
	private int lastKill;
	private int lastAP;
	private int lastGP;
	private long lastUpdate;

	/**
	 * @param dailyAP
	 * @param dailyGP
	 * @param weeklyAP
	 *
	 * @param weeklyGP
	 * @param ap
	 * @param gp
	 *
	 * @param rank
	 * @param topRanking
	 * @param dailyKill
	 * @param weeklyKill
	 * @param allKill
	 * @param maxRank
	 * @param lastKill
	 * @param lastAP
	 * @param lastGP
	 */
	public AbyssRank(int dailyAP, int dailyGP, int weeklyAP, int weeklyGP, int ap, int gp, int rank, int topRanking,
			int dailyKill, int weeklyKill, int allKill, int maxRank, int lastKill, int lastAP, int lastGP,
			long lastUpdate) {
		// AP
		this.dailyAP = dailyAP;
		this.weeklyAP = weeklyAP;
		this.currentAp = ap;
		// GP
		this.dailyGP = dailyGP;
		this.weeklyGP = weeklyGP;
		this.currentGp = gp;
		this.rank = AbyssRankEnum.getRankById(rank);
		this.topRanking = topRanking;
		this.dailyKill = dailyKill;
		this.weeklyKill = weeklyKill;
		this.allKill = allKill;
		this.maxRank = maxRank;
		this.lastKill = lastKill;
		this.lastAP = lastAP;
		this.lastGP = lastGP;
		this.lastUpdate = lastUpdate;
		doUpdate();
	}

	public enum AbyssRankUpdateType {
		PLAYER_ELYOS(1), PLAYER_ASMODIANS(2), LEGION_ELYOS(4), LEGION_ASMODIANS(8);

		private int id;

		AbyssRankUpdateType(int id) {
			this.id = id;
		}

		/** 值。 / Value. */
		public int value() {
			return id;
		}
	}

	/**
	 * 添加 AP 到玩家(当前玩家 AP + addedAP )。 / Add AP to a player (current player AP + added AP)
	 *
	 * @param additionalAp
	 */
	public void addAp(int additionalAp, Player player) {
		dailyAP += additionalAp;
		if (dailyAP < 0) {
			dailyAP = 0;
		}
		weeklyAP += additionalAp;
		if (weeklyAP < 0) {
			weeklyAP = 0;
		}
		currentAp += additionalAp;
		if (currentAp < 0) {
			currentAp = 0;
		}
		AbyssRankEnum newRank = AbyssRankEnum.getRankForAp(currentAp);
		if (player.getAbyssRank().getRank().getId() >= 1 && player.getAbyssRank().getRank().getId() <= 9) {
			if (newRank.getId() > 9) {
				newRank = AbyssRankEnum.GRADE1_SOLDIER;
			}
			setRank(newRank);
		}
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 添加 GP 到玩家(当前玩家 GP + addedGP )。 / Add GP to a player (current player GP + added GP)
	 *
	 * @param additionalGp
	 */
	public void addGp(int additionalGp) {
		dailyGP += additionalGp;
		if (dailyGP < 0) {
			dailyGP = 0;
		}
		weeklyGP += additionalGp;
		if (weeklyGP < 0) {
			weeklyGP = 0;
		}
		currentGp += additionalGp;
		if (currentGp < 0) {
			currentGp = 0;
		}
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * @return The daily <Abyss Point> count
	 */
	public int getDailyAP() {
		return dailyAP;
	}

	/**
	 * @return The weekly <Abyss Point> count
	 */
	public int getWeeklyAP() {
		return weeklyAP;
	}

	/**
	 * @return The all time <Abyss Point> count
	 */
	public int getAp() {
		return currentAp;
	}

	/**
	 * @return The daily <Glory Point> count
	 */
	public int getDailyGP() {
		return dailyGP;
	}

	/**
	 * @return The weekly <Glory Point> count
	 */
	public int getWeeklyGP() {
		return weeklyGP;
	}

	/**
	 * @return The all time <Glory Point> count
	 */
	public int getGp() {
		return currentGp;
	}

	/**
	 * @return the rank
	 */
	public AbyssRankEnum getRank() {
		return rank;
	}

	/**
	 * @return The top ranking of the current rank
	 */
	public int getTopRanking() {
		return topRanking;
	}

	/**
	 * @param topRanking
	 */
	public void setTopRanking(int topRanking) {
		this.topRanking = topRanking;
	}

	/**
	 * @return The daily count kill
	 */
	public int getDailyKill() {
		return dailyKill;
	}

	/**
	 * @return The weekly count kill
	 */
	public int getWeeklyKill() {
		return weeklyKill;
	}

	/**
	 * @return all Kill
	 */
	public int getAllKill() {
		return allKill;
	}

	/**
	 * 添加一个 kill 到玩家。 / Add one kill to a player
	 */
	public void updateKillCounts() {
		this.dailyKill += 1;
		this.weeklyKill += 1;
		this.allKill += 1;
	}

	/**
	 * @return max Rank
	 */
	public int getMaxRank() {
		return maxRank;
	}

	/**
	 * @return The last week count kill
	 */
	public int getLastKill() {
		return lastKill;
	}

	/**
	 * @return The last week <Abyss Point> count
	 */
	public int getLastAP() {
		return lastAP;
	}

	/**
	 * @return The last week <Glory Point> count
	 */
	public int getLastGP() {
		return lastGP;
	}

	/**
	 * @param rank the rank to set
	 */
	public void setRank(AbyssRankEnum rank) {
		if (rank.getId() > this.maxRank) {
			this.maxRank = rank.getId();
		}
		this.rank = rank;
		this.topRanking = rank.getQuota();
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 添加一个 kill 到玩家。 / Add one kill to a player
	 */
	public void setAllKill() {
		this.dailyKill += 1;
		this.weeklyKill += 1;
		this.allKill += 1;
	}

	/**
	 * @return the persistentState
	 */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/**
	 * @param persistentState the persistentState to set
	 */
	public void setPersistentState(PersistentState persistentState) {
		if (persistentState != PersistentState.UPDATE_REQUIRED || this.persistentState != PersistentState.NEW) {
			this.persistentState = persistentState;
		}
	}

	/** Captures all fields changed by quest AP/GP rewards. */
	public TransactionSnapshot transactionSnapshot() {
		return new TransactionSnapshot();
	}

	public final class TransactionSnapshot {
		private final int savedDailyAp = dailyAP;
		private final int savedWeeklyAp = weeklyAP;
		private final int savedCurrentAp = currentAp;
		private final int savedDailyGp = dailyGP;
		private final int savedWeeklyGp = weeklyGP;
		private final int savedCurrentGp = currentGp;
		private final AbyssRankEnum savedRank = rank;
		private final int savedTopRanking = topRanking;
		private final int savedMaxRank = maxRank;
		private final PersistentState savedPersistentState = persistentState;
		private boolean restored;

		private TransactionSnapshot() {
		}

		public void restore() {
			if (restored) {
				return;
			}
			restored = true;
			dailyAP = savedDailyAp;
			weeklyAP = savedWeeklyAp;
			currentAp = savedCurrentAp;
			dailyGP = savedDailyGp;
			weeklyGP = savedWeeklyGp;
			currentGp = savedCurrentGp;
			rank = savedRank;
			topRanking = savedTopRanking;
			maxRank = savedMaxRank;
			persistentState = savedPersistentState;
		}
	}

	/**
	 * @return The last update of the AbyssRank
	 */
	public long getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * 更新每日/每周/上次击杀与 AP 计数。 / Make an update for the daily/weekly/last kill & ap counts
	 */
	public void doUpdate() {
		boolean needUpdate = false;
		Calendar lastCal = Calendar.getInstance();
		lastCal.setTimeInMillis(lastUpdate);
		Calendar curCal = Calendar.getInstance();
		curCal.setTimeInMillis(System.currentTimeMillis());
		// 检查日——同时检查月年，防止玩家隔很久回来。 / Checking the day - month & year are checked to prevent if a player come back
		// 1 个月后同一天 / after 1 month, the same day
		if (lastCal.get(Calendar.DAY_OF_MONTH) != curCal.get(Calendar.DAY_OF_MONTH)
				|| lastCal.get(Calendar.MONTH) != curCal.get(Calendar.MONTH)
				|| lastCal.get(Calendar.YEAR) != curCal.get(Calendar.YEAR)) {
			this.dailyAP = 0;
			this.dailyGP = 0;
			this.dailyKill = 0;
			needUpdate = true;
		}
		// 检查周——同时检查年，防止玩家隔很久回来。 / Checking the week - year is checked to prevent if a player come back after 1
		// 年，同一周 / year, the same week
		if (lastCal.get(Calendar.WEEK_OF_YEAR) != curCal.get(Calendar.WEEK_OF_YEAR)
				|| lastCal.get(Calendar.YEAR) != curCal.get(Calendar.YEAR)) {
			this.lastKill = this.weeklyKill;
			this.lastAP = this.weeklyAP;
			this.lastGP = this.weeklyGP;
			this.weeklyKill = 0;
			this.weeklyAP = 0;
			this.weeklyGP = 0;
			needUpdate = true;
		}
		// 离线变更的军阶 / For offline changed ranks
		if (rank.getId() > maxRank) {
			maxRank = rank.getId();
			needUpdate = true;
		}
		// 最后，更新上次更新时间 / Finally, update the the last update
		this.lastUpdate = System.currentTimeMillis();
		if (needUpdate) {
			setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
	}
}
