package com.aionemu.gameserver.persistence.dao;

import java.util.Map;

import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.persistence.entity.SpecialLandingPo;
import com.mybatisflex.core.service.IService;

/**
 * 欧比斯特殊着陆点数据访问对象。
 * Abyss special landing location data access object.
 */
public interface SpecialLandingDao extends IService<SpecialLandingPo> {

	/**
	 * 加载所有特殊着陆点。
	 * Loads all special landing locations.
	 * @param locations 目标映射 / target map
	 */
	void loadLandingSpecialLocations(Map<Integer, LandingSpecialLocation> locations);

	/**
	 * 存储特殊着陆点。
	 * Stores a special landing location.
	 * @param location 特殊着陆点 / special landing location
	 */
	void store(LandingSpecialLocation location);

	/**
	 * 更新特殊着陆点。
	 * Updates a special landing location.
	 * @param location 特殊着陆点 / special landing location
	 * @return 是否成功 / whether successful
	 */
	boolean updateLandingSpecialLocation(LandingSpecialLocation location);

	/**
	 * 更新特殊着陆点（委托给 {@link #updateLandingSpecialLocation}）。
	 * Updates a special landing location (delegates to {@link #updateLandingSpecialLocation}).
	 * @param location 特殊着陆点 / special landing location
	 */
	default void updateLocation(final LandingSpecialLocation location) {
		updateLandingSpecialLocation(location);
	}
}
