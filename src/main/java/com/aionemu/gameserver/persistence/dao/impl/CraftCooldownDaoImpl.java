package com.aionemu.gameserver.persistence.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.CraftCooldownDao;
import com.aionemu.gameserver.persistence.entity.CraftCooldownPo;
import com.aionemu.gameserver.persistence.mapper.CraftCooldownMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 制作冷却数据访问实现。
 * Data access implementation for craft cooldowns.
 * @author synchro2
 */
@Slf4j
@Service
@Lazy
public class CraftCooldownDaoImpl extends ServiceImpl<CraftCooldownMapper, CraftCooldownPo> implements CraftCooldownDao {

	@Override
	public void loadCraftCooldowns(Player player) {
		Map<Integer, Long> cooldowns = new HashMap<>();

		try {
			long currentTime = System.currentTimeMillis();
			List<CraftCooldownPo> rows = mapper.selectListByQuery(
				QueryWrapper.create().where(CraftCooldownPo::getPlayerId).eq(player.getObjectId()));
			for (CraftCooldownPo po : rows) {
				long reuseTime = po.getReuseTime();
				if (reuseTime > currentTime) {
					cooldowns.put(po.getDelayId(), reuseTime);
				}
			}

			player.getCraftCooldownList().setCraftCoolDowns(cooldowns);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a61919a3fe85", player.getObjectId()), e);
		}
	}

	@Override
	public void storeCraftCooldowns(Player player) {
		Map<Integer, Long> cooldowns = player.getCraftCooldownList().getCraftCoolDowns();

		if (cooldowns == null || cooldowns.isEmpty()) {
			return;
		}

		Map<Integer, Long> validCooldowns = new ConcurrentHashMap<>();
		long currentTime = System.currentTimeMillis();

		for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
			if (entry.getValue() > currentTime) {
				validCooldowns.put(entry.getKey(), entry.getValue());
			}
		}

		if (validCooldowns.isEmpty()) {
			deleteCraftCoolDowns(player);
			return;
		}

		try {
			mapper.deleteByQuery(QueryWrapper.create().where(CraftCooldownPo::getPlayerId).eq(player.getObjectId()));

			for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
				mapper.upsert(player.getObjectId(), entry.getKey(), entry.getValue());
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0b287ae80175", player.getObjectId()), e);
		}
	}

	/**
	 * 删除玩家全部制作冷却。
	 * Deletes all craft cooldowns for a player.
	 * @param player 玩家 / player
	 */
	private void deleteCraftCoolDowns(Player player) {
		try {
			mapper.deleteByQuery(QueryWrapper.create().where(CraftCooldownPo::getPlayerId).eq(player.getObjectId()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.01f57aee7b11", player.getObjectId()), e);
		}
	}
}
