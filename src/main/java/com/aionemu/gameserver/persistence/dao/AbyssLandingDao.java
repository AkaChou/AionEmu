package com.aionemu.gameserver.persistence.dao;

import java.util.Map;

import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.persistence.entity.AbyssLandingPo;
import com.mybatisflex.core.service.IService;

/**
 * 欧比斯着陆点数据访问对象。
 * Abyss landing location data access object.
 */
public interface AbyssLandingDao extends IService<AbyssLandingPo> {

	/**
	 * 加载所有着陆点。
	 * Loads all landing locations.
	 * @param locations 目标映射 / target map
	 */
	void loadLandingLocations(Map<Integer, LandingLocation> locations);

	/**
	 * 存储着陆点。
	 * Stores a landing location.
	 * @param location 着陆点 / landing location
	 */
	void store(LandingLocation location);

	/**
	 * 更新着陆点。
	 * Updates a landing location.
	 * @param location 着陆点 / landing location
	 * @return 是否成功 / whether successful
	 */
	boolean updateLandingLocation(LandingLocation location);

	/**
	 * 更新着陆点（委托给 {@link #updateLandingLocation}）。
	 * Updates a landing location (delegates to {@link #updateLandingLocation}).
	 * @param location 着陆点 / landing location
	 */
	default void updateLocation(final LandingLocation location) {
		updateLandingLocation(location);
	}
}
