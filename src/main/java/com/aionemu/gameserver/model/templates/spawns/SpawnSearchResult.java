package com.aionemu.gameserver.model.templates.spawns;

/**
 * 刷新点 Search 结果模板（静态数据/XML）。
 * XML template. / XML template.
 */

public final class SpawnSearchResult {
	private SpawnSpotTemplate spot;
	private int worldId;

	public SpawnSearchResult(int worldId, SpawnSpotTemplate spot) {
		this.worldId = worldId;
		this.spot = spot;
	}

	/** 返回 spot / Returns the spot */
	public SpawnSpotTemplate getSpot() {
		return spot;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return worldId;
	}
}
