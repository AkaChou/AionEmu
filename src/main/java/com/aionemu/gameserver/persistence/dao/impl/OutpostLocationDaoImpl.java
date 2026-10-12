package com.aionemu.gameserver.persistence.dao.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.outpost.OutpostLocation;
import com.aionemu.gameserver.persistence.dao.OutpostLocationDao;
import com.aionemu.gameserver.persistence.entity.OutpostLocationPo;
import com.aionemu.gameserver.persistence.mapper.OutpostLocationMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 前哨据点数据访问实现。
 * Data access implementation for outpost locations.
 * Created by Wnkrz on 27/08/2017.
 */
@Slf4j
@Service
@Lazy
public class OutpostLocationDaoImpl extends ServiceImpl<OutpostLocationMapper, OutpostLocationPo>
	implements OutpostLocationDao {

	/**
	 * 加载前哨据点；缺失记录会自动插入。
	 * Loads outpost locations; missing rows are inserted automatically.
	 *
	 * @param locations 前哨据点映射 / outpost location map
	 */
	@Override
	public void loadOutposLocations(Map<Integer, OutpostLocation> locations) {

		try {
			Map<Integer, Boolean> loadedIds = new ConcurrentHashMap<>();

			for (OutpostLocationPo po : mapper.selectAll()) {
				OutpostLocation loc = locations.get(po.getId());

				if (loc != null) {
					loc.setRace(Race.valueOf(po.getRace()));
					loadedIds.put(po.getId(), true);
				}
			}

			for (Map.Entry<Integer, OutpostLocation> entry : locations.entrySet()) {
				if (!loadedIds.containsKey(entry.getKey())) {
					insertOutpostLocation(entry.getValue());
				}
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.19142084445f"), e);
		}

	}

	/**
	 * 更新前哨据点。
	 * Updates an outpost location.
	 *
	 * @param location 据点位置 / outpost location
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean updateOutpostLocation(OutpostLocation location) {
		try {
			return mapper.updateOutpostRace(location.getRace().toString(), location.getId()) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.3e416c0dd49f", location.getId()), e);
			return false;
		}
	}

	/**
	 * 插入前哨据点（默认种族 NPC）。
	 * Inserts an outpost location (default race NPC).
	 *
	 * @param location 据点位置 / outpost location
	 */
	private void insertOutpostLocation(OutpostLocation location) {
		try {
			mapper.insertOutpostLocation(location.getId(), Race.NPC.toString());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.96d78ac95569", location.getId()), e);
		}
	}
}
