package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerMacrossesDAO;
import com.aionemu.gameserver.model.gameobjects.player.MacroList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Aquanox
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8PlayerMacrossesDAO extends PlayerMacrossesDAO {

	private static Logger log = LoggerFactory.getLogger(MySQL8PlayerMacrossesDAO.class);
	
	private static final String INSERT_QUERY = "INSERT INTO `player_macrosses` (`player_id`, `order`, `macro`) VALUES (?,?,?)";
	private static final String UPDATE_QUERY = "UPDATE `player_macrosses` SET `macro`=? WHERE `player_id`=? AND `order`=?";
	private static final String DELETE_QUERY = "DELETE FROM `player_macrosses` WHERE `player_id`=? AND `order`=?";
	private static final String SELECT_QUERY = "SELECT `order`, `macro` FROM `player_macrosses` WHERE `player_id`=?";

	@Override
	public void addMacro(final int playerId, final int macroPosition, final String macro) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
			
			log.debug("[DAO: MySQL8PlayerMacrossesDAO] storing macro " + playerId + " " + macroPosition);
			stmt.setInt(1, playerId);
			stmt.setInt(2, macroPosition);
			stmt.setString(3, macro);
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Error adding macro for player: {} position: {}", playerId, macroPosition, e);
		}
	}

	@Override
	public void updateMacro(final int playerId, final int macroPosition, final String macro) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
			
			log.debug("[DAO: MySQL8PlayerMacrossesDAO] updating macro " + playerId + " " + macroPosition);
			stmt.setString(1, macro);
			stmt.setInt(2, playerId);
			stmt.setInt(3, macroPosition);
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Error updating macro for player: {} position: {}", playerId, macroPosition, e);
		}
	}

	@Override
	public void deleteMacro(final int playerId, final int macroPosition) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
			
			log.debug("[DAO: MySQL8PlayerMacrossesDAO] removing macro " + playerId + " " + macroPosition);
			stmt.setInt(1, playerId);
			stmt.setInt(2, macroPosition);
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Error deleting macro for player: {} position: {}", playerId, macroPosition, e);
		}
	}

	@Override
	public MacroList restoreMacrosses(final int playerId) {
		final Map<Integer, String> macrosses = new HashMap<Integer, String>();
		
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
			
			stmt.setInt(1, playerId);
			log.debug("[DAO: MySQL8PlayerMacrossesDAO] loading macroses for playerId: " + playerId);
			
			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int order = rset.getInt("order");
					String text = rset.getString("macro");
					macrosses.put(order, text);
				}
			}
		} catch (Exception e) {
			log.error("Could not restore MacroList data for player " + playerId + " from DB", e);
		}
		return new MacroList(macrosses);
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}