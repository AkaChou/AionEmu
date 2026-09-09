package com.aionemu.gameserver.model.ranking;

import com.aionemu.gameserver.model.PlayerClass;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * Season 排行结果，用于排行相关逻辑。
 * Season Ranking Result for ranking logic.
 */
@AllArgsConstructor
public class SeasonRankingResult {

	@Getter
	private final String playerName;
	@Getter
	private final int oldRank;
	@Getter
	private final int rank;
	private final int pc;
	@Getter
	private final PlayerClass playerClass;
	@Getter
	private final int playerRace;
	@Getter
	private final int playerId;

	/** 获取分数 / Returns the points. */
	public int getPoints() {
		return pc;
	}
}
