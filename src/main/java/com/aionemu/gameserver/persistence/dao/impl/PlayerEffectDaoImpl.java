package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.PlayerEffectDao;
import com.aionemu.gameserver.persistence.entity.PlayerEffectPo;
import com.aionemu.gameserver.persistence.mapper.PlayerEffectMapper;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家技能效果（Buff）持久化数据访问实现。
 * Data access implementation for player skill effect persistence.
 *
 * @author ATracer
 */
@Slf4j
@Service
@Lazy
public class PlayerEffectDaoImpl extends ServiceImpl<PlayerEffectMapper, PlayerEffectPo> implements PlayerEffectDao {

	/**
	 * 可持久化效果过滤条件（剩余时间大于 28 秒） / Predicate for insertable effects (remaining time > 28s)
	 */
	private static final Predicate<Effect> INSERTABLE_EFFECTS_PREDICATE = effect -> effect != null
		&& !effect.getSkillTemplate().isNoSaveOnLogout() && effect.getRemainingTime() > 28000;

	@Override
	public void loadPlayerEffects(Player player) {
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerEffectPo::getPlayerId).eq(player.getObjectId()))
				.forEach(po -> {
					if (po.getCurrentTime() != null && po.getCurrentTime() > 0) {
						player.getEffectController().addSavedEffect(po.getSkillId(), po.getSkillLvl(),
							po.getCurrentTime(), po.getEndTime());
					}
				});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8532fdc00af7", player.getObjectId()), e);
		}

		player.getEffectController().broadCastEffects();
	}

	@Override
	public void storePlayerEffects(Player player) {
		List<Effect> validEffects = new ArrayList<>();

		for (Effect effect : player.getEffectController().getAbnormalEffectsToShow()) {
			if (INSERTABLE_EFFECTS_PREDICATE.test(effect)) {
				validEffects.add(effect);
			}
		}

		if (validEffects.isEmpty()) {
			deletePlayerEffects(player);
			return;
		}

		try {
			DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> {
				deleteAll(player);
				insertAll(player, validEffects);
			});
		} catch (RuntimeException e) {
			log.error(I18n.get("log.db9bf45e33ff", player.getObjectId()), e);
		}
	}

	/**
	 * 删除玩家全部效果记录（单语句，无需事务）。
	 * Deletes all effect records of the player (single statement, no transaction needed).
	 *
	 * @param player 玩家 / player
	 */
	private void deletePlayerEffects(Player player) {
		try {
			mapper.deleteByQuery(QueryWrapper.create().where(PlayerEffectPo::getPlayerId).eq(player.getObjectId()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5703018fc477", player.getObjectId()), e);
		}
	}

	/**
	 * 在事务内删除玩家全部效果。
	 * Deletes all of the player's effects inside the transaction.
	 *
	 * @param player 玩家 / player
	 */
	private void deleteAll(Player player) {
		mapper.deleteByQuery(QueryWrapper.create().where(PlayerEffectPo::getPlayerId).eq(player.getObjectId()));
	}

	/**
	 * 在事务内逐行 upsert 玩家效果。
	 * Upserts the player's effects one row at a time inside the transaction.
	 *
	 * @param player 玩家 / player
	 * @param validEffects 待插入效果 / insertable effects
	 */
	private void insertAll(Player player, List<Effect> validEffects) {
		for (Effect effect : validEffects) {
			mapper.upsertEffect(player.getObjectId(), effect.getSkillId(), effect.getSkillLevel(),
				effect.getRemainingTime(), persistedEndTime(effect));
		}
	}

	private static long persistedEndTime(Effect effect) {
		var template = effect.getSkillTemplate();
		return template.isSpendTimeOnLogout() || CustomConfig.ABYSSXFORM_LOGOUT && template.isDeityAvatar()
			? effect.getEndTime() : Long.MAX_VALUE;
	}
}
