package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.BindPointPosition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 玩家绑定点 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerBindPointDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author evilset
 */
@Slf4j
public class PlayerBindPointDAO extends com.aionemu.gameserver.dao.PlayerBindPointDAO {

	/** Replace/upsert bind point SQL / Replace/upsert bind point SQL */
	private static final String INSERT_QUERY = "REPLACE INTO `player_bind_point` (`player_id`, `map_id`, `x`, `y`, `z`, `heading`) VALUES (?,?,?,?,?,?)";
	/** 查询绑定点 SQL / Select bind point SQL */
	private static final String SELECT_QUERY = "SELECT `map_id`, `x`, `y`, `z`, `heading` FROM `player_bind_point` WHERE `player_id`=?";
	/** 更新绑定点 SQL / Update bind point SQL */
	private static final String UPDATE_QUERY = "UPDATE player_bind_point set `map_id`=?, `x`=?, `y`=?, `z`=?, `heading`=? WHERE `player_id`=?";

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
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}

	/**
	 * 加载玩家绑定点。
	 * Loads the bind point for a player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void loadBindPoint(Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

			stmt.setInt(1, player.getObjectId());

			try (ResultSet rset = stmt.executeQuery()) {
				if (rset.next()) {
					int mapId = rset.getInt("map_id");
					float x = rset.getFloat("x");
					float y = rset.getFloat("y");
					float z = rset.getFloat("z");
					byte heading = rset.getByte("heading");
					BindPointPosition bind = new BindPointPosition(mapId, x, y, z, heading);
					bind.setPersistentState(PersistentState.UPDATED);
					player.setBindPoint(bind);
				}
			}
		} catch (Exception e) {
			log.error(I18n.get("log.43ab488ee287", player.getObjectId(), " from DB", e), e);
		}
	}

	/**
	 * 插入玩家绑定点。
	 * Inserts a player bind point.
	 *
	 * @param player 玩家 / player
	 * @return 是否插入成功 / whether insert succeeded
	 */
	@Override
	public boolean insertBindPoint(Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

			BindPointPosition bpp = player.getBindPoint();
			stmt.setInt(1, player.getObjectId());
			stmt.setInt(2, bpp.getMapId());
			stmt.setFloat(3, bpp.getX());
			stmt.setFloat(4, bpp.getY());
			stmt.setFloat(5, bpp.getZ());
			stmt.setByte(6, bpp.getHeading());
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error(I18n.get("log.8f29cc17afb0", player.getObjectId(), " from DB", e), e);
			return false;
		}
	}

	/**
	 * 更新玩家绑定点。
	 * Updates a player bind point.
	 *
	 * @param player 玩家 / player
	 * @return 是否更新成功 / whether update succeeded
	 */
	@Override
	public boolean updateBindPoint(Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

			BindPointPosition bpp = player.getBindPoint();
			stmt.setInt(1, bpp.getMapId());
			stmt.setFloat(2, bpp.getX());
			stmt.setFloat(3, bpp.getY());
			stmt.setFloat(4, bpp.getZ());
			stmt.setByte(5, bpp.getHeading());
			stmt.setInt(6, player.getObjectId());
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error(I18n.get("log.f96acb94f4a3", player.getObjectId(), " from DB", e), e);
			return false;
		}
	}

	/**
	 * 按持久化状态保存玩家绑定点。
	 * Stores the player bind point according to its persistent state.
	 *
	 * @param player 玩家 / player
	 * @return 是否保存成功 / whether store succeeded
	 */
	@Override
	public boolean store(final Player player) {
		boolean insert = false;
		BindPointPosition bind = player.getBindPoint();

		switch (bind.getPersistentState()) {
			case NEW:
				insert = insertBindPoint(player);
				break;
			case UPDATE_REQUIRED:
				insert = updateBindPoint(player);
				break;
			default:
				return true;
		}
		bind.setPersistentState(PersistentState.UPDATED);
		return insert;
	}
}
