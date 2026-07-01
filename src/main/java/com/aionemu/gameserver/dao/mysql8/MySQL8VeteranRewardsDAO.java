package com.aionemu.gameserver.dao.mysql8;

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
 * Updated for MySQL 8 - Fixed connection leaks
 */
@Slf4j
public class MySQL8VeteranRewardsDAO extends VeteranRewardsDAO {


	private static final String SELECT_QUERY = "SELECT * FROM veteran_rewards ORDER BY id";
	private static final String DELETE_QUERY = "DELETE FROM veteran_rewards WHERE id = ?";

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
			log.error("[VETERANREWARD] getVeteranReward", e);
		}
		return result;
	}

	@Override
	public void delVeteranReward(final int id_veteran_reward) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt_2 = con.prepareStatement(DELETE_QUERY)) {
			
			stmt_2.setInt(1, id_veteran_reward);
			stmt_2.executeUpdate();
		} catch (Exception e) {
			log.error("[VETERANREWARD] delVeteranReward", e);
		}
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}