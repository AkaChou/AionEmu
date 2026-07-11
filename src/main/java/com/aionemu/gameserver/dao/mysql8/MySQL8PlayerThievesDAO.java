package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerThievesListDAO;
import com.aionemu.gameserver.services.events.thievesguildservice.ThievesStatusList;

/**
 * 玩家盗贼公会状态 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerThievesListDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author Dision
 */
@Slf4j
public class MySQL8PlayerThievesDAO extends PlayerThievesListDAO {

	/** 查询盗贼状态 SQL / Select thieves status SQL*/
	private static final String SELECT_QUERY = "SELECT * FROM player_thieves WHERE `player_id`=?";
	/** 插入盗贼状态 SQL / Insert thieves status SQL*/
	private static final String INSERT_QUERY = "INSERT INTO player_thieves (`player_id`, rank, thieves_count, prison_count, " + "last_kinah, `revenge_name`, revenge_count, revenge_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	/** 更新盗贼状态 SQL / Update thieves status SQL*/
	private static final String UPDATE_QUERY = "UPDATE player_thieves SET rank=?, thieves_count=?, prison_count=?, " + "last_kinah=?, revenge_name=?, revenge_count=?, revenge_date=? WHERE player_id=?";

	/**
	 * 按玩家 ID 加载盗贼公会状态。
	 * Loads thieves guild status by player id.
	 *
	 * player id
	 *
	 * @param playerId @return 盗贼状态，不存在则 null / thieves status, or null if missing
	 */
	@Override
	public ThievesStatusList loadThieves(int playerId) {
		ThievesStatusList thieves = null;

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement st = con.prepareStatement(SELECT_QUERY)) {

			st.setInt(1, playerId);

			try (ResultSet rs = st.executeQuery()) {
				if (rs.next()) {
					thieves = new ThievesStatusList();
					thieves.setPlayerId(playerId);
					thieves.setRankId(rs.getInt("rank"));
					thieves.setThievesCount(rs.getInt("thieves_count"));
					thieves.setPrisonCount(rs.getInt("prison_count"));
					thieves.setLastThievesKinah(rs.getLong("last_kinah"));
					thieves.setRevengeName(rs.getString("revenge_name"));
					thieves.setRevengeCount(rs.getInt("revenge_count"));
					thieves.setRevengeDate(rs.getTimestamp("revenge_date"));
				}
			}
		} catch (Exception e) {
			log.error(I18n.get("log.7470c83eff31", playerId, e));
		}
		return thieves;
	}

	/**
	 * 保存新的盗贼公会状态。
	 * Saves a new thieves guild status entry.
	 *
	 * thieves status
	 *
	 * @param thieves @return 是否写入成功 / whether the write succeeded
	 */
	@Override
	public boolean saveNewThieves(ThievesStatusList thieves) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

			stmt.setInt(1, thieves.getPlayerId());
			stmt.setInt(2, thieves.getRankId());
			stmt.setInt(3, thieves.getThievesCount());
			stmt.setInt(4, thieves.getPrisonCount());
			stmt.setLong(5, thieves.getLastThievesKinah());
			stmt.setString(6, thieves.getRevengeName());
			stmt.setInt(7, thieves.getRevengeCount());
			stmt.setTimestamp(8, thieves.getRevengeDate());
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error(I18n.get("log.ed90140836fb", e));
			return false;
		}
	}

	/**
	 * 更新已有盗贼公会状态。
	 * Updates an existing thieves guild status entry.
	 *
	 * thieves status
	 */
	@Override
	public void storeThieves(final ThievesStatusList thieves) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

			stmt.setInt(1, thieves.getRankId());
			stmt.setInt(2, thieves.getThievesCount());
			stmt.setInt(3, thieves.getPrisonCount());
			stmt.setLong(4, thieves.getLastThievesKinah());
			stmt.setString(5, thieves.getRevengeName());
			stmt.setInt(6, thieves.getRevengeCount());
			stmt.setTimestamp(7, thieves.getRevengeDate());
			stmt.setInt(8, thieves.getPlayerId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.887d6060852b", thieves.getPlayerId(), e));
		}
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
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(database, majorVersion, minorVersion);
	}
}
