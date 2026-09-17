package com.aionemu.gameserver.model.ranking;

import com.aionemu.gameserver.model.PlayerClass;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * Season 排行结果，用于排行相关逻辑。
 * Season Ranking Result for ranking logic.
 */
@Getter
@AllArgsConstructor
public class SeasonRankingResult {

	private final String playerName;
	private final int oldRank;
	private final int rank;
	private final int pc;
	private final PlayerClass playerClass;
	private final int playerRace;
	private final int playerId;

	/** 获取分数 / Returns the points. */
	public int getPoints() {
		return pc;
	}
}
