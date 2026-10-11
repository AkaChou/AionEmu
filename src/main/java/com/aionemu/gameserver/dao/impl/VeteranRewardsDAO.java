package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.veteranrewards.VeteranRewards;

import lombok.extern.slf4j.Slf4j;

/**
 * 老兵奖励 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of VeteranRewardsDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
@Repository
public class VeteranRewardsDAO extends com.aionemu.gameserver.dao.VeteranRewardsDAO {

	/**
	 * 查询老兵奖励 SQL / Select veteran rewards SQL
	 */
	private static final String SELECT_QUERY = "SELECT * FROM veteran_rewards ORDER BY id";
	/**
	 * 删除老兵奖励 SQL / Delete veteran reward SQL
	 */
	private static final String DELETE_QUERY = "DELETE FROM veteran_rewards WHERE id = ?";

	private final JdbcClient jdbcClient;

	public VeteranRewardsDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public VeteranRewardsDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 加载全部老兵奖励记录。
	 * Loads all veteran reward records.
	 *
	 * @return 奖励集 / reward set
	 */
	@Override
	public Set<VeteranRewards> getVeteranReward() {
		final Set<VeteranRewards> result = new HashSet<>();

		try {
			result.addAll(jdbcClient.sql(SELECT_QUERY)
				.query((rs, _) -> new VeteranRewards(
					rs.getInt("id"),
					rs.getString("player"),
					rs.getInt("type"),
					rs.getInt("item"),
					rs.getInt("count"),
					rs.getInt("kinah"),
					rs.getString("sender"),
					rs.getString("title"),
					rs.getString("message")
				))
				.list()
			);
		} catch (Exception e) {
			log.error(I18n.get("log.3324f8b8d45c"), e);
		}
		return result;
	}

	/**
	 * 按 ID 删除老兵奖励。
	 * Deletes a veteran reward by id.
	 *
	 * @param id_veteran_reward 奖励 ID / reward id
	 */
	@Override
	public void deleteInTransaction(Connection con, int rewardId) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
			stmt.setInt(1, rewardId);
			if (stmt.executeUpdate() == 0) {
				throw new SQLException("No veteran reward row deleted for id " + rewardId);
			}
		}
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
