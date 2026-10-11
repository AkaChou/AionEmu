package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PortalCooldownItem;

import lombok.extern.slf4j.Slf4j;

/**
 * 传送门冷却 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PortalCooldownsDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
@Repository
public class PortalCooldownsDAO extends com.aionemu.gameserver.dao.PortalCooldownsDAO {

	/** 插入或更新传送门冷却 SQL / Insert or update portal cooldown SQL*/
	private static final String INSERT_QUERY = "INSERT INTO `portal_cooldowns` (`player_id`, `world_id`, `reuse_time`, `entry_count`) VALUES (?,?,?,?) " + "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`), `entry_count` = VALUES(`entry_count`)";
	/** 删除传送门冷却按玩家 SQL / Delete portal cooldowns by player SQL*/
	private static final String DELETE_QUERY = "DELETE FROM `portal_cooldowns` WHERE `player_id`=?";
	/** 查询传送门冷却 SQL / Select portal cooldowns SQL*/
	private static final String SELECT_QUERY = "SELECT `world_id`, `reuse_time`, `entry_count` FROM `portal_cooldowns` WHERE `player_id`=?";

	private final JdbcClient jdbcClient;

	public PortalCooldownsDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public PortalCooldownsDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 加载玩家传送门冷却。
	 * Loads portal cooldowns for a player.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadPortalCooldowns(final Player player) {
		Map<Integer, PortalCooldownItem> portalCoolDowns = new HashMap<>();

		try {
			long now = System.currentTimeMillis();
			jdbcClient.sql(SELECT_QUERY)
				.param(player.getObjectId())
				.query((rs, _) -> {
					int worldId = rs.getInt("world_id");
					long reuseTime = rs.getLong("reuse_time");
					int entryCount = rs.getInt("entry_count");
					if (reuseTime > now) {
						portalCoolDowns.put(worldId, new PortalCooldownItem(worldId, entryCount, reuseTime));
					}
					return null;
				})
				.list();

			player.getPortalCooldownList().setPortalCoolDowns(portalCoolDowns);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.bd26489b30c6", player.getObjectId()), e);
		}
	}

	/**
	 * 保存玩家传送门冷却。
	 * Stores portal cooldowns for a player.
	 * @param player 玩家 / player
	 */
	@Override
	public void storePortalCooldowns(final Player player) {
		deletePortalCooldowns(player);

		Map<Integer, PortalCooldownItem> portalCoolDowns = player.getPortalCooldownList().getPortalCoolDowns();
		if (portalCoolDowns == null || portalCoolDowns.isEmpty()) {
			return;
        }

		try {
			long now = System.currentTimeMillis();
			for (Map.Entry<Integer, PortalCooldownItem> entry : portalCoolDowns.entrySet()) {
				final int worldId = entry.getKey();
				final PortalCooldownItem item = entry.getValue();
				final long reuseTime = item.getCooldown();
				final int entryCount = item.getEntryCount();

				if (reuseTime < now) {
					continue;
				}

				jdbcClient.sql(INSERT_QUERY)
					.param(player.getObjectId())
					.param(worldId)
					.param(reuseTime)
					.param(entryCount)
					.update();
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.3f6c79bd16dd", player.getObjectId()), e);
		}
	}

	/**
	 * 删除玩家全部传送门冷却。
	 * Deletes all portal cooldowns for a player.
	 * @param player 玩家 / player
	 */
	private void deletePortalCooldowns(final Player player) {
		try {
			jdbcClient.sql(DELETE_QUERY)
				.param(player.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4b9e8239bcf5", player.getObjectId()), e);
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
