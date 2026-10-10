package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.cp.PlayerCPEntry;
import com.aionemu.gameserver.model.cp.PlayerCPList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家创造力点数（CP）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerCreativityPointsDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
@Repository
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

	private final JdbcClient jdbcClient;

	public PlayerCreativityPointsDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public PlayerCreativityPointsDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 加载玩家 CP 列表。
	 * Loads the player's CP list.
	 * @param player 玩家 / player
	 * @return CP 列表 / CP list
	 */
	@Override
	public PlayerCPList loadCP(Player player) {
		List<PlayerCPEntry> cp = new ArrayList<>();

		try {
			cp = jdbcClient.sql(SELECT_QUERY)
				.param(player.getObjectId())
				.query((rset, rowNum) -> new PlayerCPEntry(
					rset.getInt("slot"),
					rset.getInt("point"),
					PersistentState.UPDATED
				))
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f66364d8a626", player.getObjectId(), " from DB"), e);
		}
		return new PlayerCPList(cp);
	}

	/**
	 * 保存玩家 CP。
	 * Stores a player CP entry.
	 * @param objectId 玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @param point 点数 / points
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean storeCP(int objectId, int slot, int point) {
		try {
			return jdbcClient.sql(INSERT_OR_UPDATE)
				.param(objectId)
				.param(slot)
				.param(point)
				.update() > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ca5c95d176db", objectId, " from DB"), e);
			return false;
		}
	}

	/**
	 * 删除玩家 CP 槽位。
	 * Deletes a player CP slot.
	 * @param objectId 玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean deleteCP(int objectId, int slot) {
		try {
			return jdbcClient.sql(DELETE_QUERY)
				.param(objectId)
				.param(slot)
				.update() > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.55ed70097989", objectId, " from DB"), e);
			return false;
		}
	}

	/**
	 * 获取玩家 CP 槽位数量。
	 * Gets the player's CP slot count.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 槽位数量 / slot count
	 */
	@Override
	public int getSlotSize(int playerObjId) {
		try {
			Integer size = jdbcClient.sql(SELECT_COUNT_QUERY)
				.param(playerObjId)
				.query((rs, rowNum) -> rs.getInt("size"))
				.optional()
				.orElse(0);
			return size != null ? size : 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ddd6a72fd9a0", playerObjId), e);
			return 0;
		}
	}

	/**
	 * 获取玩家首个 CP 槽位 ID。
	 * Gets the first CP slot id of a player.
	 * @param obj 玩家对象 ID / player object id
	 * @return 槽位 ID / slot id
	 */
	@Override
	public int getCPSlotObjId(final int obj) {
		try {
			Integer slot = jdbcClient.sql(SELECT_SLOT_QUERY)
				.param(obj)
				.query((rs, rowNum) -> rs.getInt("slot"))
				.optional()
				.orElse(0);
			return slot != null ? slot : 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.08a2600ad0a0", obj), e);
			return 0;
		}
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
