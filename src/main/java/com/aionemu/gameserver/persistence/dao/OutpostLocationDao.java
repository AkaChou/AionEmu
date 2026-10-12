package com.aionemu.gameserver.persistence.dao;

import java.util.Map;

import com.aionemu.gameserver.model.outpost.OutpostLocation;
import com.aionemu.gameserver.persistence.entity.OutpostLocationPo;
import com.mybatisflex.core.service.IService;

/**
 * 前哨据点数据访问对象，负责加载与更新前哨位置状态。
 * Outpost data access object responsible for loading and updating outpost location state.
 * Created by Wnkrz on 27/08/2017.
 */
public interface OutpostLocationDao extends IService<OutpostLocationPo> {

	/**
	 * 加载全部前哨据点位置到给定映射中。
	 * Loads all outpost locations into the given map.
	 * @param locations 目标映射 / destination map
	 */
	void loadOutposLocations(Map<Integer, OutpostLocation> locations);

	/**
	 * 更新单个前哨据点位置状态。
	 * Updates a single outpost location state.
	 * @param location 前哨位置 / outpost location
	 * @return 若成功则为 true / true if successful
	 */
	boolean updateOutpostLocation(OutpostLocation location);

	/**
	 * 更新位置的便捷方法，委托给 {@link #updateOutpostLocation}。
	 * Convenience method that delegates to {@link #updateOutpostLocation}.
	 * @param location 前哨位置 / outpost location
	 */
	default void updateLocation(final OutpostLocation location) {
		updateOutpostLocation(location);
	}
}
