package com.aionemu.gameserver.model.ranking;

/**
 * Season 排行 Enum 枚举。
 * Season Ranking Enum enumeration.
 */

public enum SeasonRankingEnum {
	/** Hall Of Tenacity / Hall Of Tenacity */
	HALL_OF_TENACITY(1), ARENA_OF_TENACITY(541), TOWER_OF_CHALLENGE(2), ARENA_6V6(3);

	private int tableId;

	private SeasonRankingEnum(int tableId) {
		this.tableId = tableId;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return tableId;
	}
}
