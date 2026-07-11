package com.aionemu.gameserver.dao;

import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.landing.LandingLocation;

/**
 * 欧比斯着陆点数据访问对象。
 * Abyss landing location data access object.
 */
public abstract class AbyssLandingDAO implements DAO {
	/**
	 * 加载所有着陆点。
	 * Loads all landing locations.
	 *
	 * target map
	 * whether successful
	 */
	public abstract boolean loadLandingLocations(Map<Integer, LandingLocation> locations);

	/**
	 * 存储着陆点。
	 * Stores a landing location.
	 *
	 * landing location
	 */
	public abstract void store(LandingLocation location);

	/**
	 * 更新着陆点。
	 * Updates a landing location.
	 *
	 * landing location
	 * whether successful
	 */
	public abstract boolean updateLandingLocation(LandingLocation location);

	/**
	 * 更新着陆点（委托给 {@link #updateLandingLocation}）。
	 * Updates a landing location (delegates to {@link #updateLandingLocation}).
	 *
	 * landing location
	 */
	public void updateLocation(final LandingLocation location) {
		updateLandingLocation(location);
	}

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	@Override
	public String getClassName() {
		return AbyssLandingDAO.class.getName();
	}
}
