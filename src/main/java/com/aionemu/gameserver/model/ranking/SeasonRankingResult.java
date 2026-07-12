package com.aionemu.gameserver.model.ranking;

import com.aionemu.gameserver.model.PlayerClass;
import lombok.Getter;

/**
 * Season 排行结果，用于排行相关逻辑。
 * Season Ranking Result for ranking logic.
 */
public class SeasonRankingResult {

	@Getter
	private String playerName;
	@Getter
	private int oldRank;
	@Getter
	private int rank;
	private int pc;
	@Getter
	private PlayerClass playerClass;
	@Getter
	private int playerRace;
	@Getter
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

	/** 获取点。 / Returns the points. */
	public int getPoints() {
		return pc;
	}
}
