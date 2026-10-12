package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.town.Town;
import com.aionemu.gameserver.persistence.dao.TownDao;
import com.aionemu.gameserver.persistence.entity.TownPo;
import com.aionemu.gameserver.persistence.mapper.TownMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 城镇数据访问实现。
 * Data access implementation for towns.
 */
@Slf4j
@Service
@Lazy
public class TownDaoImpl extends ServiceImpl<TownMapper, TownPo> implements TownDao {

	@Override
	public Map<Integer, Town> load(Race race) {
		Map<Integer, Town> towns = new ConcurrentHashMap<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(TownPo::getRace).eq(race.toString())
				.orderBy(TownPo::getId, true)).forEach(po -> towns.put(po.getId(), toTown(po)));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.da2be87857a1", race), e);
		}
		return towns;
	}

	@Override
	public void store(Town town) {
		if (town == null) {
			return;
		}

		switch (town.getPersistentState()) {
			case NEW:
				insertTown(town);
				break;
			case UPDATE_REQUIRED:
				updateTown(town);
				break;
			default:
				break;
		}
	}

	/**
	 * 将持久化对象转换为城镇模型。
	 * Converts a persistence object into the town model.
	 * @param po 城镇持久化对象 / town persistence object
	 * @return 村庄 / town
	 */
	private static Town toTown(TownPo po) {
		Town town = new Town(po.getId(), po.getLevel(), po.getPoints(), Race.valueOf(po.getRace()),
			po.getLevelUpDate());
		town.setPersistentState(PersistentState.UPDATED);
		return town;
	}

	/**
	 * 插入新城镇记录。
	 * Inserts a new town record.
	 * @param town 村庄 / town
	 */
	private void insertTown(Town town) {
		try {
			mapper.insert(toPo(town));
			town.setPersistentState(PersistentState.UPDATED);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.83ac20817a76", town.getId()), e);
		}
	}

	/**
	 * 更新城镇等级与积分；无行受影响时回退为插入。
	 * Updates town level and points; falls back to insert if no row is affected.
	 * @param town 村庄 / town
	 */
	private void updateTown(Town town) {
		try {
			boolean updated = UpdateChain.of(TownPo.class)
				.set(TownPo::getLevel, town.getLevel())
				.set(TownPo::getPoints, town.getPoints())
				.set(TownPo::getLevelUpDate, resolveLevelUpDate(town.getLevelUpDate()))
				.where(TownPo::getId).eq(town.getId())
				.update();

			if (updated) {
				town.setPersistentState(PersistentState.UPDATED);
			} else {
				insertTown(town);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.86ef9ddbc0ca", town.getId()), e);
		}
	}

	/**
	 * 将城镇转换为持久化对象，时间列沿用原判定。
	 * Converts a town into a persistence object, keeping the legacy date rule.
	 * @param town 村庄 / town
	 * @return 城镇持久化对象 / town persistence object
	 */
	private static TownPo toPo(Town town) {
		TownPo po = new TownPo();
		po.setId(town.getId());
		po.setLevel(town.getLevel());
		po.setPoints(town.getPoints());
		po.setRace(town.getRace().toString());
		po.setLevelUpDate(resolveLevelUpDate(town.getLevelUpDate()));
		return po;
	}

	/**
	 * 解析升级时间：为空或早于 1970 年时取当前时间。
	 * Resolves the level-up date: falls back to the current time when null or before 1970.
	 * @param levelUpDate 升级时间 / level-up date
	 * @return 可写入的时间 / writable date
	 */
	private static Timestamp resolveLevelUpDate(Timestamp levelUpDate) {
		if (levelUpDate == null || levelUpDate.getTime() < 1000000) {
			return new Timestamp(System.currentTimeMillis());
		}
		return levelUpDate;
	}
}
