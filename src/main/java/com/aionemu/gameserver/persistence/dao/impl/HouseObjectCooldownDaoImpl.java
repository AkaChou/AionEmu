package com.aionemu.gameserver.persistence.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.HouseObjectCooldownDao;
import com.aionemu.gameserver.persistence.entity.HouseObjectCooldownPo;
import com.aionemu.gameserver.persistence.mapper.HouseObjectCooldownMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 房屋物件冷却数据访问实现。
 * Data access implementation for house object cooldowns.
 * @author Rolandas
 */
@Slf4j
@Service
@Lazy
public class HouseObjectCooldownDaoImpl extends ServiceImpl<HouseObjectCooldownMapper, HouseObjectCooldownPo>
	implements HouseObjectCooldownDao {

	@Override
	public void loadHouseObjectCooldowns(Player paramPlayer) {
		try {
			Map<Integer, Long> cooldowns = new HashMap<>();
			long now = System.currentTimeMillis();

			List<HouseObjectCooldownPo> rows = mapper.selectListByQuery(
				QueryWrapper.create().where(HouseObjectCooldownPo::getPlayerId).eq(paramPlayer.getObjectId()));
			for (HouseObjectCooldownPo po : rows) {
				long reuseTime = po.getReuseTime();
				if (reuseTime > now) {
					cooldowns.put(po.getObjectId(), reuseTime);
				}
			}

			paramPlayer.getHouseObjectCooldownList().setHouseObjectCooldowns(cooldowns);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.fa08c06bca16", paramPlayer.getObjectId()), e);
		}
	}

	@Override
	public void storeHouseObjectCooldowns(Player paramPlayer) {
		Map<Integer, Long> cooldowns = paramPlayer.getHouseObjectCooldownList().getHouseObjectCooldowns();

		if (cooldowns == null || cooldowns.isEmpty()) {
			return;
		}

		try {
			mapper.deleteByQuery(
				QueryWrapper.create().where(HouseObjectCooldownPo::getPlayerId).eq(paramPlayer.getObjectId()));

			long currentTime = System.currentTimeMillis();
			for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
				if (entry.getValue() <= currentTime) {
					continue;
				}

				mapper.upsert(paramPlayer.getObjectId(), entry.getKey(), entry.getValue());
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4f3f74a85b17", paramPlayer.getObjectId()), e);
		}
	}
}
