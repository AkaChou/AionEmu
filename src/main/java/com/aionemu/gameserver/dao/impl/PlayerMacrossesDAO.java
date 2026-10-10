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
import com.aionemu.gameserver.model.gameobjects.player.MacroList;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家宏 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerMacrossesDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author Aquanox
 */
@Slf4j
@Repository
public class PlayerMacrossesDAO extends com.aionemu.gameserver.dao.PlayerMacrossesDAO {

	/** 插入宏 SQL / Insert macro SQL*/
	private static final String INSERT_QUERY = "INSERT INTO `player_macrosses` (`player_id`, `order`, `macro`) VALUES (?,?,?)";
	/** 更新宏 SQL / Update macro SQL*/
	private static final String UPDATE_QUERY = "UPDATE `player_macrosses` SET `macro`=? WHERE `player_id`=? AND `order`=?";
	/** 删除宏 SQL / Delete macro SQL*/
	private static final String DELETE_QUERY = "DELETE FROM `player_macrosses` WHERE `player_id`=? AND `order`=?";
	/** 查询宏 SQL / Select macros SQL*/
	private static final String SELECT_QUERY = "SELECT `order`, `macro` FROM `player_macrosses` WHERE `player_id`=?";

	private final JdbcClient jdbcClient;

	public PlayerMacrossesDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public PlayerMacrossesDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 新增玩家宏。
	 * Adds a player macro.
	 * @param playerId 玩家 ID / player id
	 * @param macroPosition 宏槽位 / macro slot
	 * @param macro 宏文本 / macro text
	 */
	@Override
	public void addMacro(final int playerId, final int macroPosition, final String macro) {
		try {
			log.debug("[DAO: PlayerMacrossesDAO] storing macro {} {}", playerId, macroPosition);
			jdbcClient.sql(INSERT_QUERY)
				.param(playerId)
				.param(macroPosition)
				.param(macro)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6fb6dc66106b", playerId, macroPosition), e);
		}
	}

	/**
	 * 更新玩家宏。
	 * Updates a player macro.
	 * @param playerId 玩家 ID / player id
	 * @param macroPosition 宏槽位 / macro slot
	 * @param macro 宏文本 / macro text
	 */
	@Override
	public void updateMacro(final int playerId, final int macroPosition, final String macro) {
		try {
			log.debug("[DAO: PlayerMacrossesDAO] updating macro {} {}", playerId, macroPosition);
			jdbcClient.sql(UPDATE_QUERY)
				.param(macro)
				.param(playerId)
				.param(macroPosition)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a1520e1b6fea", playerId, macroPosition), e);
		}
	}

	/**
	 * 删除玩家宏。
	 * Deletes a player macro.
	 * @param playerId 玩家 ID / player id
	 * @param macroPosition 宏槽位 / macro slot
	 */
	@Override
	public void deleteMacro(final int playerId, final int macroPosition) {
		try {
			log.debug("[DAO: PlayerMacrossesDAO] removing macro {} {}", playerId, macroPosition);
			jdbcClient.sql(DELETE_QUERY)
				.param(playerId)
				.param(macroPosition)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7e1652b6543b", playerId, macroPosition), e);
		}
	}

	/**
	 * 恢复玩家全部宏。
	 * Restores all macros for a player.
	 * @param playerId 玩家 ID / player id
	 * @return 宏列表 / macro list
	 */
	@Override
	public MacroList restoreMacrosses(final int playerId) {
		final Map<Integer, String> macrosses = new HashMap<>();

		try {
			log.debug("[DAO: PlayerMacrossesDAO] loading macroses for playerId: {}", playerId);
			jdbcClient.sql(SELECT_QUERY)
				.param(playerId)
				.query((rset, rowNum) -> {
					int order = rset.getInt("order");
					String text = rset.getString("macro");
					macrosses.put(order, text);
					return null;
				})
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.11ce99709119", playerId, " from DB"), e);
		}
		return new MacroList(macrosses);
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
}
