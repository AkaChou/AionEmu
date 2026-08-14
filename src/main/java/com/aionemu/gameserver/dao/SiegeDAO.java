package com.aionemu.gameserver.dao;

import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.siege.SiegeLocation;

/**
 * 攻城地点数据访问抽象层。
 * DAO for siege location persistence.
 *
 * @author Sarynth
 */
public abstract class SiegeDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * @return 完整类名 / fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return SiegeDAO.class.getName();
	}

	/**
	 * 加载全部攻城地点到给定映射。
	 * Loads all siege locations into the given map.
	 *
	 * @param locations 攻城地点映射 / siege location map
	 * @return 是否加载成功 / true if loaded
	 */
	public abstract boolean loadSiegeLocations(Map<Integer, SiegeLocation> locations);

	/**
	 * 更新单个攻城地点。
	 * Updates a single siege location.
	 *
	 * @param paramSiegeLocation 攻城战地点 / siege location
	 * @return 是否更新成功 / true if updated
	 */
	public abstract boolean updateSiegeLocation(SiegeLocation paramSiegeLocation);

	/**
	 * 更新攻城地点（委托 {@link #updateSiegeLocation}）。
	 * Updates a siege location (delegates to {@link #updateSiegeLocation}).
	 *
	 * @param siegeLocation 攻城战地点 / siege location
	 */
	public void updateLocation(final SiegeLocation siegeLocation) {
		updateSiegeLocation(siegeLocation);
	}
}
