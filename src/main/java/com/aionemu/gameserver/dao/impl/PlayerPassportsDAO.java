package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.database.DatabaseFactory;

/**
 * 玩家护照 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerPassportsDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author Alcapwnd, Lyras, FrozenKiller
 */
@Slf4j
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

	/**
	 * 插入护照记录。
	 * Inserts a passport record.
	 *
	 * 账号 ID / account id
	 * passport id
	 * stamps
	 * @param last_stamp 最近盖章时间 / last stamp time
	 */
	@Override
	public void insertPassport(final int accountId, final int passportId, final int stamps, final Timestamp last_stamp) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

			stmt.setInt(1, accountId);
			stmt.setInt(2, passportId);
			stmt.setInt(3, stamps);
			stmt.setTimestamp(4, last_stamp);
			stmt.executeUpdate();
		} catch (Exception e) {
			log.error(I18n.get("log.06e64ddd17f9", e.getMessage(), e), e);
		}
	}

	/**
	 * 更新护照记录。
	 * Updates a passport record.
	 *
	 * 账号 ID / account id
	 * passport id
	 * stamps
	 * @param rewarded 是否已领奖 / whether rewarded
	 * @param last_stamp 最近盖章时间 / last stamp time
	 */
	@Override
	public void updatePassport(final int accountId, final int passportId, final int stamps, final boolean rewarded, final Timestamp last_stamp) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

			stmt.setInt(1, stamps);
			stmt.setInt(2, rewarded ? 1 : 0);
			stmt.setTimestamp(3, last_stamp);
			stmt.setInt(4, accountId);
			stmt.setInt(5, passportId);
			stmt.executeUpdate();
		} catch (Exception e) {
			log.error(I18n.get("log.b7b8fa49af32", e), e);
		}
	}

	/**
	 * 获取护照印章数。
	 * Gets the stamp count of a passport.
	 *
	 * 账号 ID / account id
	 * passport id
	 * stamps
	 */
	@Override
	public int getStamps(final int accountId, final int passportId) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement s = con.prepareStatement(SELECT_STAMPS_QUERY)) {

			s.setInt(1, accountId);
			s.setInt(2, passportId);

			try (ResultSet rs = s.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("stamps");
				}
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.d8f278a870b7", accountId, passportId, e), e);
		}
		return 0;
	}

	/**
	 * 获取护照最近盖章时间。
	 * Gets the last stamp time of a passport.
	 *
	 * 账号 ID / account id
	 * passport id
	 * @return 最近盖章时间 / last stamp time
	 */
	@Override
	public Timestamp getLastStamp(final int accountId, final int passportId) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement s = con.prepareStatement(SELECT_LAST_STAMP_QUERY)) {

			s.setInt(1, accountId);
			s.setInt(2, passportId);

			try (ResultSet rs = s.executeQuery()) {
				if (rs.next()) {
					return rs.getTimestamp("last_stamp");
				}
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.52b96dcdcf0e", accountId, e), e);
		}
		return new Timestamp(System.currentTimeMillis());
	}

	/**
	 * 获取账号全部护照 ID。
	 * Gets all passport ids of an account.
	 *
	 * 账号 ID / account id
	 * passport id list
	 */
	@Override
	public List<Integer> getPassports(final int accountId) {
		final List<Integer> ids = new ArrayList<Integer>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_PASSPORTS_QUERY)) {

			stmt.setInt(1, accountId);

			try (ResultSet resultSet = stmt.executeQuery()) {
				while (resultSet.next()) {
					ids.add(resultSet.getInt("passport_id"));
				}
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.add8e8df4ec5", accountId, e), e);
		}
		return ids;
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * @param s 数据库名 / database name
	 * @param i 主版本 / major version
	 * @param i1 次版本 / minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String s, int i, int i1) {
		return DAOUtils.supports(s, i, i1);
	}
}
