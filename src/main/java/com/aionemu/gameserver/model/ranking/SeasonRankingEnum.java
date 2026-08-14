package com.aionemu.gameserver.model.ranking;

/**
 * 赛季排行枚举：对应各排行的数据库表 ID。
 * Season ranking enumeration mapping each ranking type to its database table id.
 */
public enum SeasonRankingEnum {
	/** 黄金神庙训练所 / Hall Of Tenacity */
	HALL_OF_TENACITY(1),
	/** 黄金竞技场 / Arena Of Tenacity */
	ARENA_OF_TENACITY(541),
	/** 挑战之塔 / Tower Of Challenge */
	TOWER_OF_CHALLENGE(2),
	/** 竞技场 6v6 / Arena 6v6 */
	ARENA_6V6(3);

	private int tableId;

	private SeasonRankingEnum(int tableId) {
		this.tableId = tableId;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return tableId;
	}
}
