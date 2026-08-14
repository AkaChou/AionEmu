package com.aionemu.gameserver.dao;

import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;

/**
 * 欧比斯特殊着陆点数据访问对象。
 * Abyss special landing location data access object.
 */
public abstract class AbyssSpecialLandingDAO implements DAO {
	/**
	 * 加载所有特殊着陆点。
	 * Loads all special landing locations.
	 *
	 * @param locations 目标映射 / target map
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean loadLandingSpecialLocations(Map<Integer, LandingSpecialLocation> locations);

	/**
	 * 存储特殊着陆点。
	 * Stores a special landing location.
	 *
	 * @param location 特殊着陆点 / special landing location
	 */
	public abstract void store(LandingSpecialLocation location);

	/**
	 * 更新特殊着陆点。
	 * Updates a special landing location.
	 *
	 * @param location 特殊着陆点 / special landing location
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean updateLandingSpecialLocation(LandingSpecialLocation location);

	/**
	 * 更新特殊着陆点（委托给 {@link #updateLandingSpecialLocation}）。
	 * Updates a special landing location (delegates to {@link #updateLandingSpecialLocation}).
	 *
	 * @param location 特殊着陆点 / special landing location
	 */
	public void updateLocation(final LandingSpecialLocation location) {
		updateLandingSpecialLocation(location);
	}

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public String getClassName() {
		return AbyssSpecialLandingDAO.class.getName();
	}
}
