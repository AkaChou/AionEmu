package com.aionemu.gameserver.persistence.dao;

import java.util.Map;

import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.persistence.entity.SiegeLocationPo;
import com.mybatisflex.core.service.IService;

/**
 * 攻城地点数据访问层。
 * Data access layer for siege location persistence.
 * @author Sarynth
 */
public interface SiegeLocationDao extends IService<SiegeLocationPo> {

	/**
	 * 加载全部攻城地点到给定映射。
	 * Loads all siege locations into the given map.
	 * @param locations 攻城地点映射 / siege location map
	 */
	void loadSiegeLocations(Map<Integer, SiegeLocation> locations);

	/**
	 * 更新单个攻城地点。
	 * Updates a single siege location.
	 * @param paramSiegeLocation 攻城战地点 / siege location
	 * @return 是否更新成功 / true if updated
	 */
	boolean updateSiegeLocation(SiegeLocation paramSiegeLocation);

	/**
	 * 更新攻城地点（委托 {@link #updateSiegeLocation}）。
	 * Updates a siege location (delegates to {@link #updateSiegeLocation}).
	 * @param siegeLocation 攻城战地点 / siege location
	 */
	default void updateLocation(final SiegeLocation siegeLocation) {
		updateSiegeLocation(siegeLocation);
	}
}
