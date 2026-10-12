package com.aionemu.gameserver.persistence.dao;

import java.util.Map;

import com.aionemu.gameserver.model.base.BaseLocation;
import com.aionemu.gameserver.persistence.entity.BaseLocationPo;
import com.mybatisflex.core.service.IService;

/**
 * 据点位置数据访问对象。
 * Base location data access object.
 */
public interface BaseLocationDao extends IService<BaseLocationPo> {

	/**
	 * 加载所有据点位置。
	 * Loads all base locations.
	 * @param locations 目标映射 / target map
	 */
	void loadBaseLocations(Map<Integer, BaseLocation> locations);

	/**
	 * 更新据点位置。
	 * Updates a base location.
	 * @param location 基地位置 / base location
	 * @return 是否成功 / whether successful
	 */
	boolean updateBaseLocation(BaseLocation location);

	/**
	 * 更新据点位置（委托给 {@link #updateBaseLocation}）。
	 * Updates a base location (delegates to {@link #updateBaseLocation}).
	 * @param location 基地位置 / base location
	 */
	default void updateLocation(final BaseLocation location) {
		updateBaseLocation(location);
	}
}
