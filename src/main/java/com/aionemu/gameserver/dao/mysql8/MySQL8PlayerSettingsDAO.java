package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerSettingsDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSettings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 玩家设置 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerSettingsDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author ATracer
 */
@Slf4j
public class MySQL8PlayerSettingsDAO extends PlayerSettingsDAO {


	/** 查询设置 SQL / Select settings SQL*/
	private static final String SELECT_QUERY = "SELECT * FROM player_settings WHERE player_id = ?";
	/** 替换设置 SQL / Replace settings SQL*/
	private static final String REPLACE_QUERY = "REPLACE INTO player_settings VALUES (?, ?, ?)";

	/**
	 * 加载玩家设置。
	 * Loads player settings.
	 *
	 * @param player 玩家 / player
	 */
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
			log.error(I18n.get("log.6e9496b8330d", playerId, " from DB", e));
		}

		playerSettings.setPersistentState(PersistentState.UPDATED);
		player.setPlayerSettings(playerSettings);
	}

	/**
	 * 保存玩家设置。
	 * Saves player settings.
	 *
	 * @param player 玩家 / player
	 */
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
			log.error(I18n.get("log.19b35d703bb1", playerId, e));
		}

		playerSettings.setPersistentState(PersistentState.UPDATED);
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
