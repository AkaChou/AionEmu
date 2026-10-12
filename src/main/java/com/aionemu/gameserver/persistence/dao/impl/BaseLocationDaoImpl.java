package com.aionemu.gameserver.persistence.dao.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.base.BaseLocation;
import com.aionemu.gameserver.persistence.dao.BaseLocationDao;
import com.aionemu.gameserver.persistence.entity.BaseLocationPo;
import com.aionemu.gameserver.persistence.mapper.BaseLocationMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 基地位置（Base Location）数据访问实现。
 * Data access implementation for base locations.
 */
@Slf4j
@Service
@Lazy
public class BaseLocationDaoImpl extends ServiceImpl<BaseLocationMapper, BaseLocationPo> implements BaseLocationDao {

	/**
	 * 加载基地位置数据到给定映射；缺失记录会自动插入默认 NPC 阵营。
	 * Loads base location data into the given map; missing records are auto-inserted with default NPC race.
	 *
	 * @param locations 基地位置映射 / base location map
	 */
	@Override
	public void loadBaseLocations(Map<Integer, BaseLocation> locations) {

		try {
			Set<Integer> loadedIds = new HashSet<>();

			for (BaseLocationPo po : mapper.selectListByQuery(
					QueryWrapper.create().orderBy(BaseLocationPo::getId, true))) {
				BaseLocation loc = locations.get(po.getId());
				if (loc != null) {
					loc.setRace(Race.valueOf(po.getRace()));
					loadedIds.add(po.getId());
				}
			}

			for (Map.Entry<Integer, BaseLocation> entry : locations.entrySet()) {
				if (!loadedIds.contains(entry.getKey())) {
					insertBaseLocation(entry.getValue());
				}
			}

		} catch (DataAccessException e) {
			log.error(I18n.get("log.e6377177b72f"), e);
		}

	}

	/**
	 * 更新单个基地位置的所属阵营。
	 * Updates the race ownership of a single base location.
	 *
	 * @param location 基地位置 / base location
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean updateBaseLocation(BaseLocation location) {
		try {
			return UpdateChain.of(BaseLocationPo.class)
				.set(BaseLocationPo::getRace, location.getRace().toString())
				.where(BaseLocationPo::getId).eq(location.getId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.00e03bf592f1", location.getId()), e);
			return false;
		}
	}

	/**
	 * 插入基地位置（默认 NPC 阵营）。
	 * Inserts a base location with default NPC race.
	 *
	 * @param location 基地位置 / base location
	 */
	private void insertBaseLocation(BaseLocation location) {
		try {
			mapper.upsert(location.getId(), Race.NPC.toString());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.31d1da1209bd", location.getId()), e);
		}
	}
}
