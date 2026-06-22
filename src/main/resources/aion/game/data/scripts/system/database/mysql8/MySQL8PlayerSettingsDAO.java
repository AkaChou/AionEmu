package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerSettingsDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author ATracer
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8PlayerSettingsDAO extends PlayerSettingsDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerSettingsDAO.class);
	
	private static final String SELECT_QUERY = "SELECT * FROM player_settings WHERE player_id = ?";
	private static final String REPLACE_QUERY = "REPLACE INTO player_settings VALUES (?, ?, ?)";

	@Override
	public void loadSettings(final Player player) {
		final int playerId = player.getObjectId();
		final PlayerSettings playerSettings = new PlayerSettings();
		
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement statement = con.prepareStatement(SELECT_QUERY)) {
			
			statement.setInt(1, playerId);
			
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					int type = resultSet.getInt("settings_type");
					switch (type) {
						case 0:
							playerSettings.setUiSettings(resultSet.getBytes("settings"));
							break;
						case 1:
							playerSettings.setShortcuts(resultSet.getBytes("settings"));
							break;
						case 2:
							playerSettings.setHouseBuddies(resultSet.getBytes("settings"));
							break;
						case -1:
							playerSettings.setDisplay(resultSet.getInt("settings"));
							break;
						case -2:
							playerSettings.setDeny(resultSet.getInt("settings"));
							break;
					}
				}
			}
		} catch (Exception e) {
			log.error("Could not restore PlayerSettings data for player " + playerId + " from DB", e);
		}
		
		playerSettings.setPersistentState(PersistentState.UPDATED);
		player.setPlayerSettings(playerSettings);
	}

	@Override
	public void saveSettings(final Player player) {
		final int playerId = player.getObjectId();
		PlayerSettings playerSettings = player.getPlayerSettings();
		
		if (playerSettings.getPersistentState() == PersistentState.UPDATED) {
			return;
		}

		final byte[] uiSettings = playerSettings.getUiSettings();
		final byte[] shortcuts = playerSettings.getShortcuts();
		final byte[] houseBuddies = playerSettings.getHouseBuddies();
		final int display = playerSettings.getDisplay();
		final int deny = playerSettings.getDeny();

		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);
			
			try (PreparedStatement stmt = con.prepareStatement(REPLACE_QUERY)) {
				if (uiSettings != null) {
					stmt.setInt(1, playerId);
					stmt.setInt(2, 0);
					stmt.setBytes(3, uiSettings);
					stmt.addBatch();
				}

				if (shortcuts != null) {
					stmt.setInt(1, playerId);
					stmt.setInt(2, 1);
					stmt.setBytes(3, shortcuts);
					stmt.addBatch();
				}
				
				if (houseBuddies != null) {
					stmt.setInt(1, playerId);
					stmt.setInt(2, 2);
					stmt.setBytes(3, houseBuddies);
					stmt.addBatch();
				}

				stmt.setInt(1, playerId);
				stmt.setInt(2, -1);
				stmt.setInt(3, display);
				stmt.addBatch();

				stmt.setInt(1, playerId);
				stmt.setInt(2, -2);
				stmt.setInt(3, deny);
				stmt.addBatch();
				
				stmt.executeBatch();
			}
			
			con.commit();
		} catch (SQLException e) {
			log.error("Error saving player settings for player: " + playerId, e);
		}

		playerSettings.setPersistentState(PersistentState.UPDATED);
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}