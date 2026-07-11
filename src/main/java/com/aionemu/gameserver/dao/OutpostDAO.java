package com.aionemu.gameserver.dao;

import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.outpost.OutpostLocation;

/**
 * 前哨据点数据访问对象，负责加载与更新前哨位置状态。
 * Outpost data access object responsible for loading and updating outpost location state.
 *
 * Created by Wnkrz on 27/08/2017.
 */
public abstract class OutpostDAO implements DAO {

	/**
	 * 加载全部前哨据点位置到给定映射中。
	 * Loads all outpost locations into the given map.
	 *
	 * destination map
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean loadOutposLocations(Map<Integer, OutpostLocation> locations);

	/**
	 * 更新单个前哨据点位置状态。
	 * Updates a single outpost location state.
	 *
	 * outpost location
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean updateOutpostLocation(OutpostLocation location);

	/**
	 * 更新位置的便捷方法，委托给 {@link #updateOutpostLocation}。
	 * Convenience method that delegates to {@link #updateOutpostLocation}.
	 *
	 * outpost location
	 */
	public void updateLocation(final OutpostLocation location) {
		updateOutpostLocation(location);
	}

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * DAO class name
	 */
	@Override
	public String getClassName() {
		return OutpostDAO.class.getName();
	}
}
