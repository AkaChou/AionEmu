package com.aionemu.gameserver.persistence.dao.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.PlayerCooldownDao;
import com.aionemu.gameserver.persistence.entity.PlayerCooldownPo;
import com.aionemu.gameserver.persistence.mapper.PlayerCooldownMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家技能冷却数据访问实现。
 * Data access implementation for player skill cooldowns.
 * @author nrg
 */
@Slf4j
@Service
@Lazy
public class PlayerCooldownDaoImpl extends ServiceImpl<PlayerCooldownMapper, PlayerCooldownPo>
	implements PlayerCooldownDao {

	/**
	 * 冷却可持久化判定（剩余时间大于 28 秒） / Predicate for persistable cooldowns (remaining &gt; 28s)
	 */
	private static final Predicate<Long> COOLDOWN_PREDICATE =
		reuseDelay -> reuseDelay != null && reuseDelay - System.currentTimeMillis() > 28000;

	@Override
	public void loadPlayerCooldowns(Player player) {
		Map<Integer, Long> validCooldowns = new ConcurrentHashMap<>();

		try {
			long currentTime = System.currentTimeMillis();

			List<PlayerCooldownPo> rows = mapper.selectListByQuery(
				QueryWrapper.create().where(PlayerCooldownPo::getPlayerId).eq(player.getObjectId()));
			for (PlayerCooldownPo po : rows) {
				long reuseDelay = po.getReuseDelay();

				if (reuseDelay > currentTime && DataManager.SKILL_DATA.shouldPersistCooldown(po.getCooldownId())) {
					validCooldowns.put(po.getCooldownId(), reuseDelay);
				}
			}

			for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
				player.setSkillCoolDown(entry.getKey(), entry.getValue());
			}

			log.debug("Loaded {} cooldowns for player {}", validCooldowns.size(), player.getObjectId());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.11da0c8c55d9", player.getObjectId()), e);
		}
	}

	@Override
	public void storePlayerCooldowns(Player player) {
		Map<Integer, Long> cooldowns = player.getSkillCoolDowns();

		if (cooldowns == null || cooldowns.isEmpty()) {
			deletePlayerCooldowns(player);
			return;
		}

		Map<Integer, Long> validCooldowns = new ConcurrentHashMap<>();

		for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
			Long reuseDelay = entry.getValue();
			if (COOLDOWN_PREDICATE.test(reuseDelay) && DataManager.SKILL_DATA.shouldPersistCooldown(entry.getKey())) {
				validCooldowns.put(entry.getKey(), reuseDelay);
			}
		}

		if (validCooldowns.isEmpty()) {
			deletePlayerCooldowns(player);
			return;
		}

		try {
			mapper.deleteByQuery(QueryWrapper.create().where(PlayerCooldownPo::getPlayerId).eq(player.getObjectId()));

			for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
				mapper.upsert(player.getObjectId(), entry.getKey(), entry.getValue());
			}
			log.debug("Stored {} cooldowns for player {}", validCooldowns.size(), player.getObjectId());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5f4dd6d0855d", player.getObjectId()), e);
		}
	}

	/**
	 * 删除玩家的全部冷却记录。
	 * Deletes all cooldown records for the player.
	 * @param player 玩家 / player
	 */
	private void deletePlayerCooldowns(Player player) {
		try {
			int deleted = mapper.deleteByQuery(
				QueryWrapper.create().where(PlayerCooldownPo::getPlayerId).eq(player.getObjectId()));

			if (deleted > 0) {
				log.debug("Deleted {} cooldowns for player {}", deleted, player.getObjectId());
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4b0c2a651d40", player.getObjectId()), e);
		}
	}
}
