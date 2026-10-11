package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.extern.slf4j.Slf4j;

/**
 * 制作冷却 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of CraftCooldownsDAO using JdbcClient.
 * Updated for MySQL 8.
 *
 * @author synchro2
 */
@Slf4j
@Repository
public class CraftCooldownsDAO extends com.aionemu.gameserver.dao.CraftCooldownsDAO {

	/**
	 * 插入更新制作冷却 SQL / Insert or update craft cooldown SQL
	 */
	private static final String INSERT_QUERY = "INSERT INTO `craft_cooldowns` (`player_id`, `delay_id`, `reuse_time`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`)";
	/**
	 * 按玩家 SQL 的删除制作冷却 / Delete craft cooldowns by player SQL
	 */
	private static final String DELETE_QUERY = "DELETE FROM `craft_cooldowns` WHERE `player_id` = ?";
	/**
	 * 查询制作冷却 SQL / Select craft cooldowns SQL
	 */
	private static final String SELECT_QUERY = "SELECT `delay_id`, `reuse_time` FROM `craft_cooldowns` WHERE `player_id` = ?";
	/**
	 * 删除过期制作冷却 SQL / Delete expired craft cooldowns SQL
	 */
	private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `craft_cooldowns` WHERE `reuse_time` < ?";

	private final JdbcClient jdbcClient;

	public CraftCooldownsDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public CraftCooldownsDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 加载玩家制作冷却。
	 * Loads craft cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void loadCraftCooldowns(Player player) {
		Map<Integer, Long> cooldowns = new HashMap<>();

		try {
			long currentTime = System.currentTimeMillis();
			jdbcClient.sql(SELECT_QUERY)
				.param(player.getObjectId())
				.query((rs, _) -> {
					int delayId = rs.getInt("delay_id");
					long reuseTime = rs.getLong("reuse_time");
					if (reuseTime > currentTime) {
						cooldowns.put(delayId, reuseTime);
					}
					return null;
				})
				.list();

			player.getCraftCooldownList().setCraftCoolDowns(cooldowns);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a61919a3fe85", player.getObjectId()), e);
		}
	}

	/**
	 * 保存玩家制作冷却。
	 * Stores craft cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
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
			jdbcClient.sql(DELETE_QUERY)
				.param(player.getObjectId())
				.update();

			for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
				jdbcClient.sql(INSERT_QUERY)
					.param(player.getObjectId())
					.param(entry.getKey())
					.param(entry.getValue())
					.update();
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0b287ae80175", player.getObjectId()), e);
		}
	}

	/**
	 * 删除玩家全部制作冷却。
	 * Deletes all craft cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	private void deleteCraftCoolDowns(Player player) {
		try {
			jdbcClient.sql(DELETE_QUERY)
				.param(player.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.01f57aee7b11", player.getObjectId()), e);
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
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
}
