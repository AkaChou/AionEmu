package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemCooldown;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

/**
 * 物品冷却 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of ItemCooldownsDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author ATracer
 */
@Slf4j
public class ItemCooldownsDAO extends com.aionemu.gameserver.dao.ItemCooldownsDAO {

	/** 插入或更新物品冷却 SQL / Insert or update item cooldown SQL*/
	private static final String INSERT_QUERY = "INSERT INTO `item_cooldowns` (`player_id`, `delay_id`, `use_delay`, `reuse_time`) VALUES (?,?,?,?) " + "ON DUPLICATE KEY UPDATE `use_delay` = VALUES(`use_delay`), `reuse_time` = VALUES(`reuse_time`)";
	/** 删除物品冷却按玩家 SQL / Delete item cooldowns by player SQL*/
	private static final String DELETE_QUERY = "DELETE FROM `item_cooldowns` WHERE `player_id`=?";
	/** 查询物品冷却 SQL / Select item cooldowns SQL*/
	private static final String SELECT_QUERY = "SELECT `delay_id`, `use_delay`, `reuse_time` FROM `item_cooldowns` WHERE `player_id`=?";

	/**
	 * 加载玩家物品冷却。
	 * Loads item cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void loadItemCooldowns(final Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

			stmt.setInt(1, player.getObjectId());

			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int delayId = rset.getInt("delay_id");
					int useDelay = rset.getInt("use_delay");
					long reuseTime = rset.getLong("reuse_time");
					if (reuseTime > System.currentTimeMillis()) {
						player.addItemCoolDown(delayId, reuseTime, useDelay);
					}
				}
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.a442a442976d", player.getObjectId(), e), e);
		}
		player.getEffectController().broadCastEffects();
	}

	/**
	 * 保存玩家物品冷却。
	 * Stores item cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void storeItemCooldowns(Player player) {
		deleteItemCooldowns(player);

		Map<Integer, ItemCooldown> itemCoolDowns = player.getItemCoolDowns();
		if (itemCoolDowns == null || itemCoolDowns.isEmpty()) {
			return;
        }

		final Iterator<Map.Entry<Integer, ItemCooldown>> iterator = itemCoolDowns.entrySet().iterator();
		if (!iterator.hasNext()) {
			return;
		}

		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);

			try (PreparedStatement st = con.prepareStatement(INSERT_QUERY)) {
				while (iterator.hasNext()) {
					Map.Entry<Integer, ItemCooldown> entry = iterator.next();
					ItemCooldown cooldown = entry.getValue();

					if (cooldown.getReuseTime() <= System.currentTimeMillis() + 30000) {
						continue;
					}

					st.setInt(1, player.getObjectId());
					st.setInt(2, entry.getKey());
					st.setInt(3, cooldown.getUseDelay());
					st.setLong(4, cooldown.getReuseTime());
					st.addBatch();
				}
				st.executeBatch();
			}

			con.commit();
		} catch (SQLException e) {
			log.error(I18n.get("log.77f5a5e723bf", player.getObjectId(), e), e);
		}
	}

	/**
	 * 删除玩家全部物品冷却。
	 * Deletes all item cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	private void deleteItemCooldowns(final Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

			stmt.setInt(1, player.getObjectId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.8c3c2893f6d3", player.getObjectId(), e), e);
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
		return DAOUtils.supports(arg0, arg1, arg2);
	}
}
