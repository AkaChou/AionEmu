package com.aionemu.gameserver.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerInstanceLimit;

public class PlayerInstanceLimitsDAO extends com.aionemu.gameserver.dao.PlayerInstanceLimitsDAO {
	private static final String SELECT = "SELECT limit_key, reset_at, used, bonus_available, purchased_count, purchase_step "
			+ "FROM player_instance_limits WHERE player_id=?";
	private static final String DELETE = "DELETE FROM player_instance_limits WHERE player_id=?";
	private static final String INSERT = "INSERT INTO player_instance_limits "
			+ "(player_id, limit_key, reset_at, used, bonus_available, purchased_count, purchase_step, updated_at) "
			+ "VALUES (?,?,?,?,?,?,?,?)";

	@Override
	public void load(Player player) {
		Map<Integer, PlayerInstanceLimit> limits = new LinkedHashMap<>();
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement(SELECT)) {
			statement.setInt(1, player.getObjectId());
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					int key = result.getInt("limit_key");
					limits.put(key, new PlayerInstanceLimit(key, result.getLong("reset_at"), result.getInt("used"),
							result.getInt("bonus_available"), result.getInt("purchased_count"),
							result.getInt("purchase_step")));
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to load instance limits for player " + player.getObjectId(), e);
		}
		player.getInstanceLimits().load(limits);
	}

	@Override
	public void store(Player player) {
		Map<Integer, PlayerInstanceLimit> limits = player.getInstanceLimits().snapshot();
		try (Connection connection = DatabaseFactory.getConnection()) {
			connection.setAutoCommit(false);
			try (PreparedStatement delete = connection.prepareStatement(DELETE);
					PreparedStatement insert = connection.prepareStatement(INSERT)) {
				delete.setInt(1, player.getObjectId());
				delete.executeUpdate();
				long updatedAt = System.currentTimeMillis();
				for (PlayerInstanceLimit limit : limits.values()) {
					insert.setInt(1, player.getObjectId());
					insert.setInt(2, limit.getLimitKey());
					insert.setLong(3, limit.getResetAt());
					insert.setInt(4, limit.getUsed());
					insert.setInt(5, limit.getBonusAvailable());
					insert.setInt(6, limit.getPurchasedCount());
					insert.setInt(7, limit.getPurchaseStep());
					insert.setLong(8, updatedAt);
					insert.addBatch();
				}
				insert.executeBatch();
				connection.commit();
			} catch (SQLException e) {
				connection.rollback();
				throw e;
			} finally {
				connection.setAutoCommit(true);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to store instance limits for player " + player.getObjectId(), e);
		}
	}

	@Override
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return DAOUtils.supports(database, majorVersion, minorVersion);
	}
}
