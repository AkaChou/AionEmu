package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.persistence.dao.AbyssLandingDao;
import com.aionemu.gameserver.persistence.entity.AbyssLandingPo;
import com.aionemu.gameserver.persistence.mapper.AbyssLandingMapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 欧比斯登陆点数据访问实现。
 * Data access implementation for abyss landing locations.
 */
@Slf4j
@Service
@Lazy
public class AbyssLandingDaoImpl extends ServiceImpl<AbyssLandingMapper, AbyssLandingPo> implements AbyssLandingDao {

	/**
	 * 持久化登陆点（委托更新）。
	 * Persists a landing location (delegates to update).
	 * @param location 登陆点位置 / landing location
	 */
	@Override
	public void store(LandingLocation location) {
		updateLandingLocation(location);
	}

	/**
	 * 加载全部登陆点；缺失记录会自动插入。
	 * Loads all landing locations; missing rows are inserted automatically.
	 * @param locations 登陆点映射 / landing location map
	 */
	@Override
	public void loadLandingLocations(final Map<Integer, LandingLocation> locations) {
		List<Integer> loaded = new ArrayList<>();

		try {
			for (AbyssLandingPo po : mapper.selectAll()) {
				LandingLocation loc = locations.get(po.getId());
				if (loc != null) {
					loc.setLevel(po.getLevel());
					loc.setPoints(po.getPoints());
					loc.setArtifactPoints(po.getArtefact());
					loc.setBasePoints(po.getBase());
					loc.setCommanderPoints(po.getCommander());
					loc.setQuestPoints(po.getQuest());
					loc.setFacilityPoints(po.getFacility());
					loc.setSiegePoints(po.getSiege());
					loc.setMonumentsPoints(po.getMonuments());
					loaded.add(po.getId());
				}
			}
		} catch (Exception e) {
			log.warn(I18n.get("log.c4af5ae55182"), e);
		}

		for (Map.Entry<Integer, LandingLocation> entry : locations.entrySet()) {
			LandingLocation sLoc = entry.getValue();
			if (!loaded.contains(sLoc.getId())) {
				insertLandingLocation(sLoc);
			}
		}
	}

	/**
	 * 更新登陆点。
	 * Updates a landing location.
	 * @param locations 登陆点位置 / landing location
	 */
	@Override
	public boolean updateLandingLocation(final LandingLocation locations) {
		try {
			return UpdateChain.of(AbyssLandingPo.class)
				.set(AbyssLandingPo::getLevel, locations.getLevel())
				.set(AbyssLandingPo::getSiege, locations.getSiegePoints())
				.set(AbyssLandingPo::getCommander, locations.getCommanderPoints())
				.set(AbyssLandingPo::getArtefact, locations.getArtifactPoints())
				.set(AbyssLandingPo::getBase, locations.getBasePoints())
				.set(AbyssLandingPo::getMonuments, locations.getMonumentsPoints())
				.set(AbyssLandingPo::getQuest, locations.getQuestPoints())
				.set(AbyssLandingPo::getFacility, locations.getFacilityPoints())
				.set(AbyssLandingPo::getPoints, locations.getPoints())
				.where(AbyssLandingPo::getId).eq(locations.getId())
				.update();
		} catch (Exception e) {
			log.error(I18n.get("log.c2e97c849263", locations.getId()), e);
			return false;
		}
	}

	/**
	 * 插入登陆点。
	 * Inserts a landing location.
	 * @param locations 登陆点位置 / landing location
	 */
	private void insertLandingLocation(final LandingLocation locations) {
		try {
			AbyssLandingPo po = new AbyssLandingPo();
			po.setId(locations.getId());
			po.setLevel(locations.getLevel());
			po.setSiege(locations.getSiegePoints());
			po.setCommander(locations.getCommanderPoints());
			po.setArtefact(locations.getArtifactPoints());
			po.setBase(locations.getBasePoints());
			po.setMonuments(locations.getMonumentsPoints());
			po.setQuest(locations.getQuestPoints());
			po.setFacility(locations.getFacilityPoints());
			po.setLevelUpDate(new Timestamp(System.currentTimeMillis()));
			po.setRace(locations.getTemplate().getRace().toString());
			po.setPoints(locations.getPoints());
			mapper.insert(po);
		} catch (Exception e) {
			log.error(I18n.get("log.34ceaeea74ab", locations.getId()), e);
		}
	}
}
