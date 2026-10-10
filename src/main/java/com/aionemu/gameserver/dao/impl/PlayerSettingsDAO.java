package com.aionemu.gameserver.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSettings;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家设置 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerSettingsDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author ATracer
 */
@Slf4j
@Repository
public class PlayerSettingsDAO extends com.aionemu.gameserver.dao.PlayerSettingsDAO {

	/** 查询设置 SQL / Select settings SQL*/
	private static final String SELECT_QUERY = "SELECT * FROM player_settings WHERE player_id = ?";
	/** 替换设置 SQL / Replace settings SQL*/
	private static final String REPLACE_QUERY = "REPLACE INTO player_settings VALUES (?, ?, ?)";

	private final JdbcClient jdbcClient;
	private final JdbcTemplate jdbcTemplate;

	public PlayerSettingsDAO() {
		this(DatabaseFactory.getJdbcClient(), DatabaseFactory.getJdbcTemplate());
	}

	@Autowired
	public PlayerSettingsDAO(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
	}

	public PlayerSettingsDAO(JdbcClient jdbcClient) {
		this(jdbcClient, DatabaseFactory.getJdbcTemplate());
	}

	/**
	 * 加载玩家设置。
	 * Loads player settings.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadSettings(final Player player) {
		final int playerId = player.getObjectId();
		final PlayerSettings playerSettings = new PlayerSettings();

		try {
			jdbcClient.sql(SELECT_QUERY)
				.param(playerId)
				.query((resultSet, rowNum) -> {
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
					return null;
				})
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6e9496b8330d", playerId, " from DB"), e);
		}

		playerSettings.setPersistentState(PersistentState.UPDATED);
		player.setPlayerSettings(playerSettings);
	}

	/**
	 * 保存玩家设置。
	 * Saves player settings.
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

		final List<Object[]> batchArgs = new ArrayList<>(5);
		if (uiSettings != null) {
			batchArgs.add(new Object[] { playerId, 0, uiSettings });
		}
		if (shortcuts != null) {
			batchArgs.add(new Object[] { playerId, 1, shortcuts });
		}
		if (houseBuddies != null) {
			batchArgs.add(new Object[] { playerId, 2, houseBuddies });
		}
		batchArgs.add(new Object[] { playerId, -1, display });
		batchArgs.add(new Object[] { playerId, -2, deny });

		try {
			jdbcTemplate.batchUpdate(REPLACE_QUERY, new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement stmt, int i) throws SQLException {
					Object[] row = batchArgs.get(i);
					stmt.setInt(1, (Integer) row[0]);
					stmt.setInt(2, (Integer) row[1]);
					if (row[2] instanceof byte[] bytes) {
						stmt.setBytes(3, bytes);
					} else {
						stmt.setInt(3, (Integer) row[2]);
					}
				}

				@Override
				public int getBatchSize() {
					return batchArgs.size();
				}
			});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.19b35d703bb1", playerId), e);
			return;
		}

		playerSettings.setPersistentState(PersistentState.UPDATED);
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
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
