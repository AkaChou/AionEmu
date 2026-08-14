package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.cp.PlayerCPEntry;
import com.aionemu.gameserver.model.cp.PlayerCPList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家创造力点数（CP）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerCreativityPointsDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
public class PlayerCreativityPointsDAO extends com.aionemu.gameserver.dao.PlayerCreativityPointsDAO {


	/** 插入或更新创意点 SQL / Insert or update CP SQL*/
	private static final String INSERT_OR_UPDATE = "INSERT INTO `player_cp` (`player_id`, `slot`, `point`) VALUES(?,?,?) " + "ON DUPLICATE KEY UPDATE `slot` = VALUES(`slot`), `point` = VALUES(`point`)";
	/** 查询创意点列表 SQL / Select CP list SQL*/
	private static final String SELECT_QUERY = "SELECT `slot`,`point` FROM `player_cp` WHERE `player_id`=?";
	/** 删除创意点 SQL / Delete CP SQL*/
	private static final String DELETE_QUERY = "DELETE FROM `player_cp` WHERE `player_id`=? AND `slot`=?";
	/** 统计创意点槽位 SQL / Count CP slots SQL*/
	private static final String SELECT_COUNT_QUERY = "SELECT COUNT(*) AS `size` FROM `player_cp` WHERE `player_id`=?";
	/** 查询创意点槽位 SQL / Select CP slot SQL */
	private static final String SELECT_SLOT_QUERY = "SELECT `slot` FROM `player_cp` WHERE `player_id`=?";

	/**
	 * 加载玩家 CP 列表。
	 * Loads the player's CP list.
	 *
	 * @param player 玩家 / player
	 * CP list
	 */
	@Override
	public PlayerCPList loadCP(Player player) {
		List<PlayerCPEntry> cp = new ArrayList<PlayerCPEntry>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

			stmt.setInt(1, player.getObjectId());

			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int slot = rset.getInt("slot");
					int point = rset.getInt("point");
					cp.add(new PlayerCPEntry(slot, point, PersistentState.UPDATED));
				}
			}
		} catch (Exception e) {
			log.error(I18n.get("log.f66364d8a626", player.getObjectId(), " from DB", e));
		}
		return new PlayerCPList(cp);
	}

	/**
	 * 保存玩家 CP。
	 * Stores a player CP entry.
	 *
	 * @param objectId 玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @param point 点数 / points
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean storeCP(int objectId, int slot, int point) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_OR_UPDATE)) {

			stmt.setInt(1, objectId);
			stmt.setInt(2, slot);
			stmt.setInt(3, point);
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error(I18n.get("log.ca5c95d176db", objectId, " from DB", e));
			return false;
		}
	}

	/**
	 * 删除玩家 CP 槽位。
	 * Deletes a player CP slot.
	 *
	 * @param objectId 玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean deleteCP(int objectId, int slot) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

			stmt.setInt(1, objectId);
			stmt.setInt(2, slot);
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error(I18n.get("log.55ed70097989", objectId, " from DB", e));
			return false;
		}
	}

	/**
	 * 获取玩家 CP 槽位数量。
	 * Gets the player's CP slot count.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 槽位数量 / slot count
	 */
	@Override
	public int getSlotSize(int playerObjId) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_COUNT_QUERY)) {

			stmt.setInt(1, playerObjId);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("size");
				}
			}
		} catch (Exception e) {
			log.error(I18n.get("log.ddd6a72fd9a0", playerObjId, e));
		}
		return 0;
	}

	/**
	 * 获取玩家首个 CP 槽位 ID。
	 * Gets the first CP slot id of a player.
	 *
	 * @param obj 玩家对象 ID / player object id
	 * @return 槽位 ID / slot id
	 */
	@Override
	public int getCPSlotObjId(final int obj) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement s = con.prepareStatement(SELECT_SLOT_QUERY)) {

			s.setInt(1, obj);

			try (ResultSet rs = s.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("slot");
				}
			}
		} catch (Exception e) {
			log.error(I18n.get("log.08a2600ad0a0", obj, e));
		}
		return 0;
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
