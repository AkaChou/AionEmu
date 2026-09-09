package com.aionemu.gameserver.model.templates.spawns;

import lombok.Getter;

/**
 * 刷新点 Search 结果模板（静态数据/XML）。
 * XML template.
 */

public final class SpawnSearchResult {
	/** 返回刷新点 / Returns the spot */
	@Getter
	private final SpawnSpotTemplate spot;
	/** 返回世界 ID / Returns the world id */
	@Getter
	private final int worldId;

	public SpawnSearchResult(int worldId, SpawnSpotTemplate spot) {
		this.worldId = worldId;
		this.spot = spot;
	}
}
