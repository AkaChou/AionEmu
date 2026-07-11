package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PortalCooldownsDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PortalCooldownItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 传送门冷却 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PortalCooldownsDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
public class MySQL8PortalCooldownsDAO extends PortalCooldownsDAO {

	/** 插入或更新传送门冷却 SQL / Insert or update portal cooldown SQL*/
	private static final String INSERT_QUERY = "INSERT INTO `portal_cooldowns` (`player_id`, `world_id`, `reuse_time`, `entry_count`) VALUES (?,?,?,?) " + "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`), `entry_count` = VALUES(`entry_count`)";
	/** 删除传送门冷却按玩家 SQL / Delete portal cooldowns by player SQL*/
	private static final String DELETE_QUERY = "DELETE FROM `portal_cooldowns` WHERE `player_id`=?";
	/** 查询传送门冷却 SQL / Select portal cooldowns SQL*/
	private static final String SELECT_QUERY = "SELECT `world_id`, `reuse_time`, `entry_count` FROM `portal_cooldowns` WHERE `player_id`=?";

	/**
	 * 加载玩家传送门冷却。
	 * Loads portal cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void loadPortalCooldowns(final Player player) {
		Map<Integer, PortalCooldownItem> portalCoolDowns = new HashMap<>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

			stmt.setInt(1, player.getObjectId());

			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int worldId = rset.getInt("world_id");
					long reuseTime = rset.getLong("reuse_time");
					int entryCount = rset.getInt("entry_count");
					if (reuseTime > System.currentTimeMillis()) {
						portalCoolDowns.put(worldId, new PortalCooldownItem(worldId, entryCount, reuseTime));
					}
				}
			}

			player.getPortalCooldownList().setPortalCoolDowns(portalCoolDowns);
		} catch (SQLException e) {
			log.error(I18n.get("log.bd26489b30c6", player.getObjectId(), e));
		}
	}

	/**
	 * 保存玩家传送门冷却。
	 * Stores portal cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void storePortalCooldowns(final Player player) {
		deletePortalCooldowns(player);

		Map<Integer, PortalCooldownItem> portalCoolDowns = player.getPortalCooldownList().getPortalCoolDowns();
		if (portalCoolDowns == null || portalCoolDowns.isEmpty()) {
			return;
        }

		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);

			try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
				for (Map.Entry<Integer, PortalCooldownItem> entry : portalCoolDowns.entrySet()) {
					final int worldId = entry.getKey();
					final PortalCooldownItem item = entry.getValue();
					final long reuseTime = item.getCooldown();
					final int entryCount = item.getEntryCount();

					if (reuseTime < System.currentTimeMillis()) {
						continue;
					}

					stmt.setInt(1, player.getObjectId());
					stmt.setInt(2, worldId);
					stmt.setLong(3, reuseTime);
					stmt.setInt(4, entryCount);
					stmt.addBatch();
				}
				stmt.executeBatch();
			}

			con.commit();
		} catch (SQLException e) {
			log.error(I18n.get("log.3f6c79bd16dd", player.getObjectId(), e));
		}
	}

	/**
	 * 删除玩家全部传送门冷却。
	 * Deletes all portal cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	private void deletePortalCooldowns(final Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

			stmt.setInt(1, player.getObjectId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.4b9e8239bcf5", player.getObjectId(), e));
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * @param arg0 数据库名 / database name
	 * major version
	 * minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String arg0, int arg1, int arg2) {
		return MySQL8DAOUtils.supports(arg0, arg1, arg2);
	}
}
