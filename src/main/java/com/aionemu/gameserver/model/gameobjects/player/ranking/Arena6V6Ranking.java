package com.aionemu.gameserver.model.gameobjects.player.ranking;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;
import lombok.Setter;

/**
 * Arena6V6排行游戏对象。
 * Arena 6 V 6 Ranking game object.
 */
@Getter
@Setter
public class Arena6V6Ranking {

	// rank
	/** 获取军阶。 / Returns the rank. */
	private int rank;
	/** 返回 best rank / Returns the best rank */
	private int bestRank;

	// 竞赛点数 / Competition points
	/** 获取点。 / Returns the points. */
	private int points;
	/** 返回 last points / Returns the last points */
	private int lastPoints;
	/** 返回 high points / Returns the high points */
	private int highPoints;
	/** 返回 low points / Returns the low points */
	private int lowPoints;

	/** 返回 possition match / Returns the possition match */
	private int possitionMatch;

	private PersistentState persistentState;

	public Arena6V6Ranking(int rank, int bestRank, int points, int lastPoints, int highPoints, int lowPoints,
			int possitionMatch) {
		this.rank = rank;
		this.bestRank = bestRank;
		this.points = points;
		this.lastPoints = lastPoints;
		this.highPoints = highPoints;
		this.lowPoints = lowPoints;
		this.possitionMatch = possitionMatch;
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
