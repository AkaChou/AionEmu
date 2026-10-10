package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.BindPointPosition;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家绑定点 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerBindPointDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author evilset
 */
@Slf4j
@Repository
public class PlayerBindPointDAO extends com.aionemu.gameserver.dao.PlayerBindPointDAO {

	/** Replace/upsert bind point SQL / Replace/upsert bind point SQL */
	private static final String INSERT_QUERY = "REPLACE INTO `player_bind_point` (`player_id`, `map_id`, `x`, `y`, `z`, `heading`) VALUES (?,?,?,?,?,?)";
	/** 查询绑定点 SQL / Select bind point SQL */
	private static final String SELECT_QUERY = "SELECT `map_id`, `x`, `y`, `z`, `heading` FROM `player_bind_point` WHERE `player_id`=?";
	/** 更新绑定点 SQL / Update bind point SQL */
	private static final String UPDATE_QUERY = "UPDATE player_bind_point set `map_id`=?, `x`=?, `y`=?, `z`=?, `heading`=? WHERE `player_id`=?";

	private final JdbcClient jdbcClient;

	public PlayerBindPointDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public PlayerBindPointDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
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
	 * 加载玩家绑定点。
	 * Loads the bind point for a player.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadBindPoint(Player player) {
		try {
			jdbcClient.sql(SELECT_QUERY)
				.param(player.getObjectId())
				.query((rs, rowNum) -> {
					int mapId = rs.getInt("map_id");
					float x = rs.getFloat("x");
					float y = rs.getFloat("y");
					float z = rs.getFloat("z");
					byte heading = rs.getByte("heading");
					BindPointPosition bind = new BindPointPosition(mapId, x, y, z, heading);
					bind.setPersistentState(PersistentState.UPDATED);
					player.setBindPoint(bind);
					return null;
				})
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.43ab488ee287", player.getObjectId(), " from DB"), e);
		}
	}

	/**
	 * 插入玩家绑定点。
	 * Inserts a player bind point.
	 * @param player 玩家 / player
	 * @return 是否插入成功 / whether insert succeeded
	 */
	@Override
	public boolean insertBindPoint(Player player) {
		try {
			BindPointPosition bpp = player.getBindPoint();
			int affected = jdbcClient.sql(INSERT_QUERY)
				.param(player.getObjectId())
				.param(bpp.getMapId())
				.param(bpp.getX())
				.param(bpp.getY())
				.param(bpp.getZ())
				.param(bpp.getHeading())
				.update();
			return affected > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8f29cc17afb0", player.getObjectId(), " from DB"), e);
			return false;
		}
	}

	/**
	 * 更新玩家绑定点。
	 * Updates a player bind point.
	 * @param player 玩家 / player
	 * @return 是否更新成功 / whether update succeeded
	 */
	@Override
	public boolean updateBindPoint(Player player) {
		try {
			BindPointPosition bpp = player.getBindPoint();
			int affected = jdbcClient.sql(UPDATE_QUERY)
				.param(bpp.getMapId())
				.param(bpp.getX())
				.param(bpp.getY())
				.param(bpp.getZ())
				.param(bpp.getHeading())
				.param(player.getObjectId())
				.update();
			return affected > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f96acb94f4a3", player.getObjectId(), " from DB"), e);
			return false;
		}
	}

	/**
	 * 按持久化状态保存玩家绑定点。
	 * Stores the player bind point according to its persistent state.
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
