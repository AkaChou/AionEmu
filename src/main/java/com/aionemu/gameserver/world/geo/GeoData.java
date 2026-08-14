package com.aionemu.gameserver.world.geo;

import com.aionemu.gameserver.geoEngine.models.GeoMap;

/**
 * 地理数据访问接口：加载地图并按世界 ID 查询。
 * Geo-data access interface: load maps and look them up by world id.
 *
 * @author ATracer
 */
public interface GeoData {

	/**
	 * 加载全部地理地图。
	 * Loads all geo maps.
	 */
	void loadGeoMaps();

	/**
	 * 按世界 ID 获取地理地图。
	 * Returns the geo map for the given world id.
	 *
	 * @param worldId 世界 ID / world id
	 * @return 对应的地理地图 / the matching geo map
	 */
	GeoMap getMap(int worldId);
}
