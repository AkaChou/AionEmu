package com.aionemu.gameserver.model.templates.spawns;

/**
 * 刷新点 Search 结果模板（静态数据/XML）。
 * XML template.
 *
 * @param spot    返回刷新点 / Returns the spot
 * @param worldId 返回世界 ID / Returns the world id
 */
public record SpawnSearchResult(SpawnSpotTemplate spot, int worldId) {
	public SpawnSearchResult(int worldId, SpawnSpotTemplate spot) {
		this(spot, worldId);
	}
}
