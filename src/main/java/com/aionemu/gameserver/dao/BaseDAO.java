package com.aionemu.gameserver.dao;

import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.base.BaseLocation;

/**
 * 据点位置数据访问对象。
 * Base location data access object.
 */
public abstract class BaseDAO implements DAO {
	/**
	 * 加载所有据点位置。
	 * Loads all base locations.
	 *
	 * @param locations 目标映射 / target map
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean loadBaseLocations(Map<Integer, BaseLocation> locations);

	/**
	 * 更新据点位置。
	 * Updates a base location.
	 *
	 * @param location 基地位置 / base location
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean updateBaseLocation(BaseLocation location);

	/**
	 * 更新据点位置（委托给 {@link #updateBaseLocation}）。
	 * Updates a base location (delegates to {@link #updateBaseLocation}).
	 *
	 * @param location 基地位置 / base location
	 */
	public void updateLocation(final BaseLocation location) {
		updateBaseLocation(location);
	}

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public String getClassName() {
		return BaseDAO.class.getName();
	}
}
