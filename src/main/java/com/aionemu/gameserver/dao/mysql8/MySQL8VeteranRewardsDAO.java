package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.VeteranRewardsDAO;
import com.aionemu.gameserver.model.veteranrewards.VeteranRewards;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.HashSet;

/**
 * 老兵奖励 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of VeteranRewardsDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
public class MySQL8VeteranRewardsDAO extends VeteranRewardsDAO {


	/** 查询老兵奖励 SQL / Select veteran rewards SQL*/
	private static final String SELECT_QUERY = "SELECT * FROM veteran_rewards ORDER BY id";
	/** 删除老兵奖励 SQL / Delete veteran reward SQL*/
	private static final String DELETE_QUERY = "DELETE FROM veteran_rewards WHERE id = ?";

	/**
	 * 加载全部老兵奖励记录。
	 * Loads all veteran reward records.
	 *
	 * reward set
	 */
	@Override
	public Set<VeteranRewards> getVeteranReward() {
		final Set<VeteranRewards> result = new HashSet<VeteranRewards>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
			 ResultSet rset = stmt.executeQuery()) {

			while (rset.next()) {
				result.add(new VeteranRewards(
					rset.getInt("id"),
					rset.getString("player"),
					rset.getInt("type"),
					rset.getInt("item"),
					rset.getInt("count"),
					rset.getInt("kinah"),
					rset.getString("sender"),
					rset.getString("title"),
					rset.getString("message")
				));
			}
		} catch (Exception e) {
			log.error(I18n.get("log.3324f8b8d45c", e));
		}
		return result;
	}

	/**
	 * 按 ID 删除老兵奖励。
	 * Deletes a veteran reward by id.
	 *
	 * reward id
	 */
	@Override
	public void delVeteranReward(final int id_veteran_reward) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt_2 = con.prepareStatement(DELETE_QUERY)) {

			stmt_2.setInt(1, id_veteran_reward);
			stmt_2.executeUpdate();
		} catch (Exception e) {
			log.error(I18n.get("log.b1edbfafc1a6", e));
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
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
