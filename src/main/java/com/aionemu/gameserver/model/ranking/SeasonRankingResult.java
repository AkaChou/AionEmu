package com.aionemu.gameserver.model.ranking;

import com.aionemu.gameserver.model.PlayerClass;

/**
 * Season 排行结果，用于排行相关逻辑。
 * Season Ranking Result for ranking logic.
 */
public class SeasonRankingResult {

	private String playerName;
	private int oldRank;
	private int rank;
	private int pc;
	private PlayerClass playerClass;
	private int playerRace;
	private int playerId;

	public SeasonRankingResult(String playerName, int oldRank, int rank, int pc, PlayerClass playerClass,
			int playerRace, int playerId) {
		this.playerName = playerName;
		this.oldRank = oldRank;
		this.rank = rank;
		this.pc = pc;
		this.playerClass = playerClass;
		this.playerRace = playerRace;
		this.playerId = playerId;
	}

	/** 获取玩家名称。 / Returns the player name. */
	public String getPlayerName() {
		return playerName;
	}

	/** 返回玩家 ID / Returns the player id */
	public int getPlayerId() {
		return playerId;
	}

	/** 获取军阶。 / Returns the rank. */
	public int getRank() {
		return rank;
	}

	/** 返回旧军阶 / Returns the old rank*/
	public int getOldRank() {
		return oldRank;
	}

	/** 获取玩家种族。 / Returns the player race. */
	public int getPlayerRace() {
		return playerRace;
	}

	/** 获取玩家职业。 / Returns the player class. */
	public PlayerClass getPlayerClass() {
		return playerClass;
	}

	/** 获取点。 / Returns the points. */
	public int getPoints() {
		return pc;
	}
}
