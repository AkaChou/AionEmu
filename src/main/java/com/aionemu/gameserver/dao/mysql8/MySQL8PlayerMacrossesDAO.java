package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerMacrossesDAO;
import com.aionemu.gameserver.model.gameobjects.player.MacroList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 玩家宏 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerMacrossesDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author Aquanox
 */
@Slf4j
public class MySQL8PlayerMacrossesDAO extends PlayerMacrossesDAO {

	/** 插入宏 SQL / Insert macro SQL*/
	private static final String INSERT_QUERY = "INSERT INTO `player_macrosses` (`player_id`, `order`, `macro`) VALUES (?,?,?)";
	/** 更新宏 SQL / Update macro SQL*/
	private static final String UPDATE_QUERY = "UPDATE `player_macrosses` SET `macro`=? WHERE `player_id`=? AND `order`=?";
	/** 删除宏 SQL / Delete macro SQL*/
	private static final String DELETE_QUERY = "DELETE FROM `player_macrosses` WHERE `player_id`=? AND `order`=?";
	/** 查询宏 SQL / Select macros SQL*/
	private static final String SELECT_QUERY = "SELECT `order`, `macro` FROM `player_macrosses` WHERE `player_id`=?";

	/**
	 * 新增玩家宏。
	 * Adds a player macro.
	 *
	 * player id
	 * 宏槽位 / macro slot
	 * macro text
	 */
	@Override
	public void addMacro(final int playerId, final int macroPosition, final String macro) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

			log.debug("[DAO: MySQL8PlayerMacrossesDAO] storing macro " + playerId + " " + macroPosition);
			stmt.setInt(1, playerId);
			stmt.setInt(2, macroPosition);
			stmt.setString(3, macro);
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.6fb6dc66106b", playerId, macroPosition, e));
		}
	}

	/**
	 * 更新玩家宏。
	 * Updates a player macro.
	 *
	 * player id
	 * 宏槽位 / macro slot
	 * macro text
	 */
	@Override
	public void updateMacro(final int playerId, final int macroPosition, final String macro) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

			log.debug("[DAO: MySQL8PlayerMacrossesDAO] updating macro " + playerId + " " + macroPosition);
			stmt.setString(1, macro);
			stmt.setInt(2, playerId);
			stmt.setInt(3, macroPosition);
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.a1520e1b6fea", playerId, macroPosition, e));
		}
	}

	/**
	 * 删除玩家宏。
	 * Deletes a player macro.
	 *
	 * player id
	 * 宏槽位 / macro slot
	 */
	@Override
	public void deleteMacro(final int playerId, final int macroPosition) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

			log.debug("[DAO: MySQL8PlayerMacrossesDAO] removing macro " + playerId + " " + macroPosition);
			stmt.setInt(1, playerId);
			stmt.setInt(2, macroPosition);
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.7e1652b6543b", playerId, macroPosition, e));
		}
	}

	/**
	 * 恢复玩家全部宏。
	 * Restores all macros for a player.
	 *
	 * player id
	 * macro list
	 */
	@Override
	public MacroList restoreMacrosses(final int playerId) {
		final Map<Integer, String> macrosses = new HashMap<Integer, String>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

			stmt.setInt(1, playerId);
			log.debug("[DAO: MySQL8PlayerMacrossesDAO] loading macroses for playerId: " + playerId);

			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int order = rset.getInt("order");
					String text = rset.getString("macro");
					macrosses.put(order, text);
				}
			}
		} catch (Exception e) {
			log.error(I18n.get("log.11ce99709119", playerId, " from DB", e));
		}
		return new MacroList(macrosses);
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * database name
	 * major version
	 * minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
