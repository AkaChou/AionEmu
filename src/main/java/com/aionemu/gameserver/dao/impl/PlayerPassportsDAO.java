package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家护照 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerPassportsDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author Alcapwnd, Lyras, FrozenKiller
 */
@Slf4j
@Repository
public class PlayerPassportsDAO extends com.aionemu.gameserver.dao.PlayerPassportsDAO {

	/** 插入通行证 SQL / Insert passport SQL*/
	private static final String INSERT_QUERY = "INSERT INTO `player_passports` (`account_id`, `passport_id`, `stamps`, `last_stamp`) VALUES (?,?,?,?)";
	/** 更新通行证 SQL / Update passport SQL*/
	private static final String UPDATE_QUERY = "UPDATE player_passports SET stamps = ?, rewarded = ?, last_stamp = ? WHERE account_id = ? AND passport_id = ?";
	/** 查询盖章 SQL / Select stamps SQL*/
	private static final String SELECT_STAMPS_QUERY = "SELECT stamps FROM player_passports WHERE account_id = ? AND passport_id = ?";
	/** 查询上次盖章时间 SQL / Select last stamp time SQL*/
	private static final String SELECT_LAST_STAMP_QUERY = "SELECT last_stamp FROM player_passports WHERE account_id = ? AND passport_id = ?";
	/** 查询账号通行证 SQL / Select account passports SQL*/
	private static final String SELECT_PASSPORTS_QUERY = "SELECT passport_id FROM player_passports WHERE account_id = ?";

	private final JdbcClient jdbcClient;

	public PlayerPassportsDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public PlayerPassportsDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 插入护照记录。
	 * Inserts a passport record.
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @param stamps 印章 / stamps
	 * @param last_stamp 最近盖章时间 / last stamp time
	 */
	@Override
	public void insertPassport(final int accountId, final int passportId, final int stamps, final Timestamp last_stamp) {
		try {
			jdbcClient.sql(INSERT_QUERY)
				.param(accountId)
				.param(passportId)
				.param(stamps)
				.param(last_stamp)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.06e64ddd17f9", e.getMessage()), e);
		}
	}

	/**
	 * 更新护照记录。
	 * Updates a passport record.
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @param stamps 印章 / stamps
	 * @param rewarded 是否已领奖 / whether rewarded
	 * @param last_stamp 最近盖章时间 / last stamp time
	 */
	@Override
	public void updatePassport(final int accountId, final int passportId, final int stamps, final boolean rewarded, final Timestamp last_stamp) {
		try {
			jdbcClient.sql(UPDATE_QUERY)
				.param(stamps)
				.param(rewarded ? 1 : 0)
				.param(last_stamp)
				.param(accountId)
				.param(passportId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b7b8fa49af32"), e);
		}
	}

	/**
	 * 获取护照印章数。
	 * Gets the stamp count of a passport.
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @return 印章 / stamps
	 */
	@Override
	public int getStamps(final int accountId, final int passportId) {
		try {
			Integer val = jdbcClient.sql(SELECT_STAMPS_QUERY)
				.param(accountId)
				.param(passportId)
				.query((rs, rowNum) -> rs.getInt("stamps"))
				.optional()
				.orElse(0);
			return val != null ? val : 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d8f278a870b7", accountId, passportId), e);
			return 0;
		}
	}

	/**
	 * 获取护照最近盖章时间。
	 * Gets the last stamp time of a passport.
	 * @param accountId 账号 ID / account id
	 * @param passportId 通行证 ID / passport id
	 * @return 最近盖章时间 / last stamp time
	 */
	@Override
	public Timestamp getLastStamp(final int accountId, final int passportId) {
		try {
			Timestamp stamp = jdbcClient.sql(SELECT_LAST_STAMP_QUERY)
				.param(accountId)
				.param(passportId)
				.query((rs, rowNum) -> rs.getTimestamp("last_stamp"))
				.optional()
				.orElse(null);
			if (stamp != null) {
				return stamp;
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.52b96dcdcf0e", accountId), e);
		}
		return new Timestamp(System.currentTimeMillis());
	}

	/**
	 * 获取账号全部护照 ID。
	 * Gets all passport ids of an account.
	 * @param accountId 账号 ID / account id
	 * @return 通行证 ID 列表 / passport id list
	 */
	@Override
	public List<Integer> getPassports(final int accountId) {
		List<Integer> ids = new ArrayList<>();

		try {
			ids = jdbcClient.sql(SELECT_PASSPORTS_QUERY)
				.param(accountId)
				.query((resultSet, rowNum) -> resultSet.getInt("passport_id"))
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.add8e8df4ec5", accountId), e);
		}
		return ids;
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
