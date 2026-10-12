package com.aionemu.gameserver.persistence.dao.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.persistence.dao.SiegeLocationDao;
import com.aionemu.gameserver.persistence.entity.SiegeLocationPo;
import com.aionemu.gameserver.persistence.mapper.SiegeLocationMapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 攻城地点数据访问实现。
 * Data access implementation for siege locations.
 */
@Slf4j
@Service
@Lazy
public class SiegeLocationDaoImpl extends ServiceImpl<SiegeLocationMapper, SiegeLocationPo>
	implements SiegeLocationDao {

	/**
	 * 加载攻城据点数据。
	 * Loads siege locations.
	 *
	 * @param locations 地图位置 / location map
	 */
	@Override
	public void loadSiegeLocations(Map<Integer, SiegeLocation> locations) {

		try {
			Map<Integer, Boolean> loadedIds = new ConcurrentHashMap<>();

			for (SiegeLocationPo po : mapper.selectAll()) {
				SiegeLocation loc = locations.get(po.getId());

				if (loc != null) {
					loc.setRace(SiegeRace.valueOf(po.getRace()));
					loc.setLegionId(po.getLegionId());
					loadedIds.put(po.getId(), true);
				}
			}

			for (Map.Entry<Integer, SiegeLocation> entry : locations.entrySet()) {
				if (!loadedIds.containsKey(entry.getKey())) {
					insertSiegeLocation(entry.getValue());
				}
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f0edc322efd4"), e);
		}

	}

	/**
	 * 更新单个攻城据点。
	 * Updates a single siege location.
	 *
	 * @param siegeLocation 攻城战据点 / siege location
	 * @return 是否成功 / whether succeeded
	 */
	@Override
	public boolean updateSiegeLocation(SiegeLocation siegeLocation) {
		try {
			return UpdateChain.of(SiegeLocationPo.class)
				.set(SiegeLocationPo::getRace, siegeLocation.getRace().toString())
				.set(SiegeLocationPo::getLegionId, siegeLocation.getLegionId())
				.where(SiegeLocationPo::getId).eq(siegeLocation.getLocationId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.98dc2b49ab4b", siegeLocation.getLocationId()), e);
			return false;
		}
	}

	/**
	 * 插入攻城据点。
	 * Inserts a siege location.
	 *
	 * @param siegeLocation 攻城战据点 / siege location
	 */
	private void insertSiegeLocation(SiegeLocation siegeLocation) {
		try {
			mapper.insertSiegeLocation(siegeLocation.getLocationId(), siegeLocation.getRace().toString(),
				siegeLocation.getLegionId());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.2d6381d9bee1", siegeLocation.getLocationId()), e);
		}
	}
}
