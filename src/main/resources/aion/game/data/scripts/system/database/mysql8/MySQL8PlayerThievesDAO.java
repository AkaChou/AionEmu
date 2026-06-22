package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerThievesListDAO;
import com.aionemu.gameserver.services.events.thievesguildservice.ThievesStatusList;

/**
 * @author Dision
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8PlayerThievesDAO extends PlayerThievesListDAO {
	
	private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerThievesDAO.class);
	
	private static final String SELECT_QUERY = "SELECT * FROM player_thieves WHERE `player_id`=?";
	private static final String INSERT_QUERY = "INSERT INTO player_thieves (`player_id`, rank, thieves_count, prison_count, " + "last_kinah, `revenge_name`, revenge_count, revenge_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String UPDATE_QUERY = "UPDATE player_thieves SET rank=?, thieves_count=?, prison_count=?, " + "last_kinah=?, revenge_name=?, revenge_count=?, revenge_date=? WHERE player_id=?";
	
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
			log.error("Error in MySQL8PlayerThievesDAO.loadThieves, playerId: " + playerId, e);
		}
		return thieves;
	}
	
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
			log.error("Error in MySQL8PlayerThievesDAO.saveNewThieves", e);
			return false;
		}
	}

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
			log.error("Error storing thieves data for player: " + thieves.getPlayerId(), e);
		}
	}
	
	@Override
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(database, majorVersion, minorVersion);
	}
}