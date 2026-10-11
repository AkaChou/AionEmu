package com.aionemu.gameserver.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemCooldown;

import lombok.extern.slf4j.Slf4j;

/**
 * 物品冷却 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of ItemCooldownsDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author ATracer
 */
@Slf4j
@Repository
public class ItemCooldownsDAO extends com.aionemu.gameserver.dao.ItemCooldownsDAO {

	/** 插入或更新物品冷却 SQL / Insert or update item cooldown SQL*/
	private static final String INSERT_QUERY = "INSERT INTO `item_cooldowns` (`player_id`, `delay_id`, `use_delay`, `reuse_time`) VALUES (?,?,?,?) " + "ON DUPLICATE KEY UPDATE `use_delay` = VALUES(`use_delay`), `reuse_time` = VALUES(`reuse_time`)";
	/** 删除物品冷却按玩家 SQL / Delete item cooldowns by player SQL*/
	private static final String DELETE_QUERY = "DELETE FROM `item_cooldowns` WHERE `player_id`=?";
	/** 查询物品冷却 SQL / Select item cooldowns SQL*/
	private static final String SELECT_QUERY = "SELECT `delay_id`, `use_delay`, `reuse_time` FROM `item_cooldowns` WHERE `player_id`=?";

	private final JdbcClient jdbcClient;
	private final JdbcTemplate jdbcTemplate;

	public ItemCooldownsDAO() {
		this(DatabaseFactory.getJdbcClient(), DatabaseFactory.getJdbcTemplate());
	}

	@Autowired
	public ItemCooldownsDAO(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
	}

	public ItemCooldownsDAO(JdbcClient jdbcClient) {
		this(jdbcClient, DatabaseFactory.getJdbcTemplate());
	}

	/**
	 * 加载玩家物品冷却。
	 * Loads item cooldowns for a player.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadItemCooldowns(final Player player) {
		try {
			long now = System.currentTimeMillis();
			jdbcClient.sql(SELECT_QUERY)
				.param(player.getObjectId())
				.query((rset, _) -> {
					int delayId = rset.getInt("delay_id");
					int useDelay = rset.getInt("use_delay");
					long reuseTime = rset.getLong("reuse_time");
					if (reuseTime > now) {
						player.addItemCoolDown(delayId, reuseTime, useDelay);
					}
					return null;
				})
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a442a442976d", player.getObjectId()), e);
		}
		player.getEffectController().broadCastEffects();
	}

	/**
	 * 保存玩家物品冷却。
	 * Stores item cooldowns for a player.
	 * @param player 玩家 / player
	 */
	@Override
	public void storeItemCooldowns(Player player) {
		deleteItemCooldowns(player);

		Map<Integer, ItemCooldown> itemCoolDowns = player.getItemCoolDowns();
		if (itemCoolDowns == null || itemCoolDowns.isEmpty()) {
			return;
		}

		List<Map.Entry<Integer, ItemCooldown>> validCooldowns = new ArrayList<>();
		long threshold = System.currentTimeMillis() + 30000;
		for (Map.Entry<Integer, ItemCooldown> entry : itemCoolDowns.entrySet()) {
			ItemCooldown cooldown = entry.getValue();
			if (cooldown != null && cooldown.getReuseTime() > threshold) {
				validCooldowns.add(entry);
			}
		}

		if (validCooldowns.isEmpty()) {
			return;
		}

		try {
			jdbcTemplate.batchUpdate(INSERT_QUERY, new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					Map.Entry<Integer, ItemCooldown> entry = validCooldowns.get(i);
					ItemCooldown cooldown = entry.getValue();
					ps.setInt(1, player.getObjectId());
					ps.setInt(2, entry.getKey());
					ps.setInt(3, cooldown.useDelay());
					ps.setLong(4, cooldown.getReuseTime());
				}

				@Override
				public int getBatchSize() {
					return validCooldowns.size();
				}
			});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.77f5a5e723bf", player.getObjectId()), e);
		}
	}

	/**
	 * 删除玩家全部物品冷却。
	 * Deletes all item cooldowns for a player.
	 * @param player 玩家 / player
	 */
	private void deleteItemCooldowns(final Player player) {
		try {
			jdbcClient.sql(DELETE_QUERY)
				.param(player.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8c3c2893f6d3", player.getObjectId()), e);
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 * @param database 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	@Override
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return DAOUtils.supports(database, majorVersion, minorVersion);
	}
}
