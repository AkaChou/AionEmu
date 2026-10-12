package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
				try (Connection con = DatabaseAccess.getConnection()) {
					deleteAll(con, player);
					insertAll(con, player, validEffects);
				} catch (SQLException e) {
					throw new DataAccessException("Failed to store player effects", e) {
					};
				}
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
	 * 在事务连接上删除玩家全部效果。
	 * Deletes all of the player's effects on the transaction connection.
	 *
	 * @param con 事务连接 / transaction connection
	 * @param player 玩家 / player
	 * @throws SQLException 删除失败时 / on delete failure
	 */
	private void deleteAll(Connection con, Player player) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement(PlayerEffectMapper.DELETE_QUERY_JDBC)) {
			ps.setInt(1, player.getObjectId());
			ps.executeUpdate();
		}
	}

	/**
	 * 在事务连接上批量插入玩家效果。
	 * Batch-inserts the player's effects on the transaction connection.
	 *
	 * @param con 事务连接 / transaction connection
	 * @param player 玩家 / player
	 * @param validEffects 待插入效果 / insertable effects
	 * @throws SQLException 写入失败时 / on write failure
	 */
	private void insertAll(Connection con, Player player, List<Effect> validEffects) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement(PlayerEffectMapper.INSERT_QUERY_JDBC)) {
			for (Effect effect : validEffects) {
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, effect.getSkillId());
				ps.setInt(3, effect.getSkillLevel());
				ps.setInt(4, effect.getRemainingTime());
				ps.setLong(5, persistedEndTime(effect));
				ps.addBatch();
			}
			ps.executeBatch();
		}
	}

	private static long persistedEndTime(Effect effect) {
		var template = effect.getSkillTemplate();
		return template.isSpendTimeOnLogout() || CustomConfig.ABYSSXFORM_LOGOUT && template.isDeityAvatar()
			? effect.getEndTime() : Long.MAX_VALUE;
	}
}
