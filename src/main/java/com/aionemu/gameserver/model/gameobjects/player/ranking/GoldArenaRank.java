package com.aionemu.gameserver.model.gameobjects.player.ranking;

import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * GoldArena 军阶游戏对象。
 * Gold Arena Rank game object.
 */
public class GoldArenaRank {

	// rank
	private int rank;
	private int bestRank;

	// 竞赛点数 / competiton Points
	private int points;
	private int lastPoints;
	private int highPoints;
	private int lowPoints;

	private int possitionMatch;

	private PersistentState persistentState;

	public GoldArenaRank(int rank, int bestRank, int points, int lastPoints, int highPoints, int lowPoints,
			int possitionMatch) {
		this.rank = rank;
		this.bestRank = bestRank;
		this.points = points;
		this.lastPoints = lastPoints;
		this.highPoints = highPoints;
		this.lowPoints = lowPoints;
		this.possitionMatch = possitionMatch;
	}

	/** 获取军阶。 / Returns the rank. */
	public int getRank() {
		return rank;
	}

	/** 返回 best rank / Returns the best rank */
	public int getBestRank() {
		return bestRank;
	}

	/** 获取点。 / Returns the points. */
	public int getPoints() {
		return points;
	}

	/** 返回 last points / Returns the last points */
	public int getLastPoints() {
		return lastPoints;
	}

	/** 返回 high points / Returns the high points */
	public int getHighPoints() {
		return highPoints;
	}

	/** 返回 low points / Returns the low points */
	public int getLowPoints() {
		return lowPoints;
	}

	/** 返回 possition match / Returns the possition match */
	public int getPossitionMatch() {
		return possitionMatch;
	}

	/** 设置军阶。 / Sets the rank. */
	public void setRank(int rank) {
		this.rank = rank;
	}

	/** 设置 best rank / Sets the best rank */
	public void setBestRank(int rank) {
		this.bestRank = rank;
	}

	/** 设置点。 / Sets the points. */
	public void setPoints(int pts) {
		this.points = pts;
	}

	/** 设置 last points / Sets the last points */
	public void setLastPoints(int pts) {
		this.lastPoints = pts;
	}

	/** 设置 high points / Sets the high points */
	public void setHighPoints(int pts) {
		this.highPoints = pts;
	}

	/** 设置 low points / Sets the low points */
	public void setLowPoints(int pts) {
		this.lowPoints = pts;
	}

	/** 设置 possition match / Sets the possition match */
	public void setPossitionMatch(int pos) {
		this.possitionMatch = pos;
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
}
