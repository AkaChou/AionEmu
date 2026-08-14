package com.aionemu.gameserver.model.gameobjects.player.ranking;

import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * 挑战之塔军阶游戏对象。
 * Tower Of Challenge Rank game object.
 */

public class TowerOfChallengeRank {
	private int rank;
	private int bestRank;
	private int lowRank;
	private int currentTime;
	private int lastTime;
	private int bestTime;
	private PersistentState persistentState;

	public TowerOfChallengeRank(int rank, int bestRank, int low_rank, int current_time, int last_time, int best_time) {
		this.rank = rank;
		this.bestRank = bestRank;
		this.lowRank = low_rank;
		this.currentTime = current_time;
		this.lastTime = last_time;
		this.bestTime = best_time;
	}

	/** 获取军阶。 / Returns the rank. */
	public int getRank() {
		return rank;
	}

	/** 返回 best rank / Returns the best rank */
	public int getBestRank() {
		return bestRank;
	}

	/** 返回 low rank / Returns the low rank */
	public int getLowRank() {
		return lowRank;
	}

	/** 返回当前时间 / Returns the current time */
	public int getCurrentTime() {
		return currentTime;
	}

	/** 返回上次时间 / Returns the last time*/
	public int getLastTime() {
		return lastTime;
	}

	/** 返回 best time / Returns the best time */
	public int getBestTime() {
		return bestTime;
	}

	/** 设置军阶。 / Sets the rank. */
	public void setRank(int r) {
		this.rank = r;
	}

	/** 设置 best rank / Sets the best rank */
	public void setBestRank(int r) {
		this.bestRank = r;
	}

	/** 设置 low rank / Sets the low rank */
	public void setLowRank(int r) {
		this.lowRank = r;
	}

	/** 设置 current time / Sets the current time */
	public void setCurrentTime(int r) {
		this.currentTime = r;
	}

	/** 设置 last time / Sets the last time */
	public void setLastTime(int r) {
		this.lastTime = r;
	}

	/** 设置 best time / Sets the best time */
	public void setBestTime(int r) {
		this.bestTime = r;
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState persistentState) {
		if (persistentState != PersistentState.UPDATE_REQUIRED || this.persistentState != PersistentState.NEW) {
			this.persistentState = persistentState;
		}
	}
}
