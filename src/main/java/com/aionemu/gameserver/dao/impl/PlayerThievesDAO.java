package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerThievesListDAO;
import com.aionemu.gameserver.services.events.thievesguildservice.ThievesStatusList;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家盗贼公会状态 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerThievesListDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author Dision
 */
@Slf4j
@Repository
public class PlayerThievesDAO extends PlayerThievesListDAO {

	/** 查询盗贼状态 SQL / Select thieves status SQL*/
	private static final String SELECT_QUERY = "SELECT * FROM player_thieves WHERE `player_id`=?";
	/** 插入盗贼状态 SQL / Insert thieves status SQL*/
	private static final String INSERT_QUERY = "INSERT INTO player_thieves (`player_id`, rank, thieves_count, prison_count, " + "last_kinah, `revenge_name`, revenge_count, revenge_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	/** 更新盗贼状态 SQL / Update thieves status SQL*/
	private static final String UPDATE_QUERY = "UPDATE player_thieves SET rank=?, thieves_count=?, prison_count=?, " + "last_kinah=?, revenge_name=?, revenge_count=?, revenge_date=? WHERE player_id=?";

	private final JdbcClient jdbcClient;

	public PlayerThievesDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public PlayerThievesDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 按玩家 ID 加载盗贼公会状态。
	 * Loads thieves guild status by player id.
	 * @param playerId 玩家 ID / player id
	 * @return 盗贼状态，不存在则 null / thieves status, or null if missing
	 */
	@Override
	public ThievesStatusList loadThieves(int playerId) {
		try {
			return jdbcClient.sql(SELECT_QUERY)
				.param(playerId)
				.query((rs, rowNum) -> {
					ThievesStatusList thieves = new ThievesStatusList();
					thieves.setPlayerId(playerId);
					thieves.setRankId(rs.getInt("rank"));
					thieves.setThievesCount(rs.getInt("thieves_count"));
					thieves.setPrisonCount(rs.getInt("prison_count"));
					thieves.setLastThievesKinah(rs.getLong("last_kinah"));
					thieves.setRevengeName(rs.getString("revenge_name"));
					thieves.setRevengeCount(rs.getInt("revenge_count"));
					thieves.setRevengeDate(rs.getTimestamp("revenge_date"));
					return thieves;
				})
				.optional()
				.orElse(null);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7470c83eff31", playerId), e);
			return null;
		}
	}

	/**
	 * 保存新的盗贼公会状态。
	 * Saves a new thieves guild status entry.
	 * @param thieves 窃贼状态 / thieves status
	 * @return 是否写入成功 / whether the write succeeded
	 */
	@Override
	public boolean saveNewThieves(ThievesStatusList thieves) {
		try {
			return jdbcClient.sql(INSERT_QUERY)
				.param(thieves.getPlayerId())
				.param(thieves.getRankId())
				.param(thieves.getThievesCount())
				.param(thieves.getPrisonCount())
				.param(thieves.getLastThievesKinah())
				.param(thieves.getRevengeName())
				.param(thieves.getRevengeCount())
				.param(thieves.getRevengeDate())
				.update() > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ed90140836fb"), e);
			return false;
		}
	}

	/**
	 * 更新已有盗贼公会状态。
	 * Updates an existing thieves guild status entry.
	 * @param thieves 窃贼状态 / thieves status
	 */
	@Override
	public void storeThieves(final ThievesStatusList thieves) {
		try {
			jdbcClient.sql(UPDATE_QUERY)
				.param(thieves.getRankId())
				.param(thieves.getThievesCount())
				.param(thieves.getPrisonCount())
				.param(thieves.getLastThievesKinah())
				.param(thieves.getRevengeName())
				.param(thieves.getRevengeCount())
				.param(thieves.getRevengeDate())
				.param(thieves.getPlayerId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.887d6060852b", thieves.getPlayerId()), e);
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
