package com.aionemu.gameserver.model.ranking;

import com.aionemu.gameserver.model.PlayerClass;

/**
 * Season 排行结果，用于排行相关逻辑。
 * Season Ranking Result for ranking logic.
 */
public record SeasonRankingResult(String playerName, int oldRank, int rank, int pc, PlayerClass playerClass,
								  int playerRace, int playerId) {

	/**
	 * 获取分数 / Returns the points.
	 */
	public int getPoints() {
		return pc;
	}
}
