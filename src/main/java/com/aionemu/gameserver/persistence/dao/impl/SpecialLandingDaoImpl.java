package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import com.aionemu.gameserver.persistence.dao.SpecialLandingDao;
import com.aionemu.gameserver.persistence.entity.SpecialLandingPo;
import com.aionemu.gameserver.persistence.mapper.SpecialLandingMapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 欧比斯特殊登陆点数据访问实现。
 * Data access implementation for abyss special landing locations.
 */
@Slf4j
@Service
@Lazy
public class SpecialLandingDaoImpl extends ServiceImpl<SpecialLandingMapper, SpecialLandingPo>
	implements SpecialLandingDao {

	/**
	 * 加载全部特殊登陆点状态，缺失的记录会自动插入。
	 * Loads all special landing location states; missing rows are inserted automatically.
	 *
	 * @param locations 模板位置映射 / template location map
	 */
	@Override
	public void loadLandingSpecialLocations(final Map<Integer, LandingSpecialLocation> locations) {
		List<Integer> loaded = new ArrayList<>();

		try {
			for (SpecialLandingPo po : mapper.selectAll()) {
				LandingSpecialLocation loc = locations.get(po.getId());
				if (loc != null) {
					loc.setType(LandingSpecialStateType.valueOf(po.getType()));
					loaded.add(loc.getId());
				}
			}
		} catch (DataAccessException e) {
			log.warn(I18n.get("log.5b9d0f9920d3"), e);
		}

		for (Map.Entry<Integer, LandingSpecialLocation> entry : locations.entrySet()) {
			LandingSpecialLocation sLoc = entry.getValue();
			if (!loaded.contains(sLoc.getId())) {
				insertLandingSpecialLocation(sLoc);
			}
		}
	}

	/**
	 * 持久化特殊登陆点。
	 * Persists a special landing location.
	 *
	 * @param location 特殊登陆点 / special landing location
	 */
	@Override
	public void store(LandingSpecialLocation location) {
		updateLandingSpecialLocation(location);
	}

	/**
	 * 更新特殊登陆点类型。
	 * Updates the type of a special landing location.
	 *
	 * @param locations 特殊登陆点 / special landing location
	 * @return 是否更新成功 / whether update succeeded
	 */
	@Override
	public boolean updateLandingSpecialLocation(final LandingSpecialLocation locations) {
		try {
			return UpdateChain.of(SpecialLandingPo.class)
				.set(SpecialLandingPo::getType, locations.getType().toString())
				.where(SpecialLandingPo::getId).eq(locations.getId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.2e2028503d2e", locations.getId()), e);
			return false;
		}
	}

	/**
	 * 插入新的特殊登陆点（默认 NO_ACTIVE）。
	 * Inserts a new special landing location (default NO_ACTIVE).
	 *
	 * @param locations 特殊登陆点 / special landing location
	 */
	private void insertLandingSpecialLocation(final LandingSpecialLocation locations) {
		locations.setType(LandingSpecialStateType.NO_ACTIVE);
		try {
			SpecialLandingPo po = new SpecialLandingPo();
			po.setId(locations.getId());
			po.setType(LandingSpecialStateType.NO_ACTIVE.toString());
			mapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7029001b4c33", locations.getId()), e);
		}
	}
}
