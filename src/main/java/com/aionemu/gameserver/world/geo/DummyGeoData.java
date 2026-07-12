package com.aionemu.gameserver.world.geo;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.geoEngine.models.GeoMap;

/**
 * 空实现地理数据，所有世界共用同一张哑地图。
 * Dummy geo-data implementation sharing a single no-op map for every world.
 *
 * @author ATracer
 */
public class DummyGeoData implements GeoData {

	/** 共享的空实现地理地图。 / Shared dummy geo map. */
	public static final DummyGeoMap DUMMY_MAP = new DummyGeoMap(StringUtils.EMPTY, 0);

	/**
	 * 空加载；哑数据无需初始化任何地图。
	 * No-op load; dummy data needs no map initialization.
	 */
	@Override
	public void loadGeoMaps() {
	}

	/**
	 * 返回共享哑地图，忽略世界 ID。
	 * Returns the shared dummy map, ignoring the world id.
	 *
	 * world id (ignored)
	 *
	 * @param worldId
	 * @return 空实现地理地图 / the dummy geo map
	 */
	@Override
	public GeoMap getMap(int worldId) {
		return DUMMY_MAP;
	}
}
