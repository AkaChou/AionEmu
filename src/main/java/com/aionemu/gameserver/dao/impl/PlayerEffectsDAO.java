package com.aionemu.gameserver.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家技能效果（Buff）持久化 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerEffectsDAO using JdbcClient.
 *
 * @author ATracer, Updated for MySQL 8
 */
@Slf4j
@Repository
public class PlayerEffectsDAO extends com.aionemu.gameserver.dao.PlayerEffectsDAO {

	/**
	 * 插入或更新玩家效果 / Insert or update a player effect
	 */
	private static final String INSERT_QUERY = "INSERT INTO `player_effects` (`player_id`, `skill_id`, `skill_lvl`, `current_time`, `end_time`) " + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + "`skill_lvl` = VALUES(`skill_lvl`), `current_time` = VALUES(`current_time`), `end_time` = VALUES(`end_time`)";
	/**
	 * 删除玩家全部效果 / Delete all effects of a player
	 */
	private static final String DELETE_QUERY = "DELETE FROM `player_effects` WHERE `player_id` = ?";
	/**
	 * 查询玩家全部效果 / Select all effects for a player
	 */
	private static final String SELECT_QUERY = "SELECT `skill_id`, `skill_lvl`, `current_time`, `end_time` FROM `player_effects` WHERE `player_id` = ?";
	/**
	 * 删除已过期效果 / Delete expired effects
	 */
	private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `player_effects` WHERE `end_time` < ?";

	/**
	 * 可持久化效果过滤条件（剩余时间大于 28 秒） / Predicate for insertable effects (remaining time > 28s)
	 */
	private static final Predicate<Effect> INSERTABLE_EFFECTS_PREDICATE = effect -> effect != null
		&& !effect.getSkillTemplate().isNoSaveOnLogout() && effect.getRemainingTime() > 28000;

	private final JdbcClient jdbcClient;
	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;

	public PlayerEffectsDAO() {
		this(DatabaseFactory.getJdbcClient(), DatabaseFactory.getJdbcTemplate(), DatabaseFactory.getTransactionTemplate());
	}

	@Autowired
	public PlayerEffectsDAO(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate must not be null");
	}

	public PlayerEffectsDAO(JdbcClient jdbcClient) {
		this(jdbcClient, DatabaseFactory.getJdbcTemplate(), DatabaseFactory.getTransactionTemplate());
	}

	/**
	 * 加载玩家技能效果到效果控制器。
	 * Loads the player's skill effects into the effect controller.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void loadPlayerEffects(Player player) {
		try {
			List<SavedEffect> savedEffects = jdbcClient.sql(SELECT_QUERY)
				.param(player.getObjectId())
				.query((rset, _) -> {
					int skillId = rset.getInt("skill_id");
					int skillLvl = rset.getInt("skill_lvl");
					int remainingTime = rset.getInt("current_time");
					long endTime = rset.getLong("end_time");

					if (remainingTime > 0) {
						return new SavedEffect(skillId, skillLvl, remainingTime, endTime);
					}
					return null;
				})
				.list();

			for (SavedEffect effect : savedEffects) {
				if (effect != null) {
					player.getEffectController().addSavedEffect(
						effect.skillId,
						effect.skillLvl,
						effect.remainingTime,
						effect.endTime
					);
				}
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8532fdc00af7", player.getObjectId()), e);
		}

		player.getEffectController().broadCastEffects();
	}

	/**
	 * 持久化玩家当前可保存的技能效果。
	 * Persists the player's currently insertable skill effects.
	 *
	 * @param player 玩家 / player
	 */
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
			transactionTemplate.executeWithoutResult(_ -> {
				jdbcClient.sql(DELETE_QUERY)
					.param(player.getObjectId())
					.update();

				jdbcTemplate.batchUpdate(INSERT_QUERY, new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Effect effect = validEffects.get(i);
						ps.setInt(1, player.getObjectId());
						ps.setInt(2, effect.getSkillId());
						ps.setInt(3, effect.getSkillLevel());
						ps.setInt(4, effect.getRemainingTime());
						ps.setLong(5, persistedEndTime(effect));
					}

					@Override
					public int getBatchSize() {
						return validEffects.size();
					}
				});
			});
		} catch (RuntimeException e) {
			log.error(I18n.get("log.db9bf45e33ff", player.getObjectId()), e);
		}
	}

	private static long persistedEndTime(Effect effect) {
		var template = effect.getSkillTemplate();
		return template.isSpendTimeOnLogout() || CustomConfig.ABYSSXFORM_LOGOUT && template.isDeityAvatar()
			? effect.getEndTime() : Long.MAX_VALUE;
	}

	/**
	 * 删除玩家全部效果记录。
	 * Deletes all effect records of the player.
	 *
	 * @param player 玩家 / player
	 */
	private void deletePlayerEffects(Player player) {
		try {
			jdbcClient.sql(DELETE_QUERY)
				.param(player.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5703018fc477", player.getObjectId()), e);
		}
	}

	/**
	 * 判断当前数据库是否受本 DAO 支持。
	 * Checks whether the given database is supported by this DAO.
	 *
	 * @param databaseName 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}

	/**
	 * 从数据库加载的效果快照。
	 * Snapshot of an effect loaded from the database.
	 */
	private record SavedEffect(int skillId, int skillLvl, int remainingTime, long endTime) {
	}
}
