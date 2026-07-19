package com.aionemu.gameserver.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.database.DatabaseFactory;

public class InstanceRewardLedgerDAO extends com.aionemu.gameserver.dao.InstanceRewardLedgerDAO {
	private static final String INSERT = "INSERT INTO instance_reward_ledger "
			+ "(instance_uid,player_id,reward_key,status,payload_hash,payload_json,created_at,completed_at) "
			+ "VALUES (?,?,?,?,?,?,?,0) ON DUPLICATE KEY UPDATE reward_key=VALUES(reward_key)";
	private static final String SELECT_FOR_UPDATE = "SELECT status,payload_hash FROM instance_reward_ledger "
			+ "WHERE instance_uid=? AND player_id=? AND reward_key=? FOR UPDATE";

	@Override
	public boolean queue(long instanceUid, int playerId, String rewardKey, String payloadHash, String payloadJson,
			long createdAt) {
		try (Connection connection = DatabaseFactory.getConnection()) {
			connection.setAutoCommit(false);
			try {
				boolean completed = lockOrCreate(connection, instanceUid, playerId, rewardKey, payloadHash, payloadJson,
						createdAt);
				connection.commit();
				return !completed;
			} catch (SQLException e) {
				connection.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to queue instance reward " + rewardKey + " for " + playerId, e);
		}
	}

	@Override
	public boolean lockOrCreate(Connection connection, long instanceUid, int playerId, String rewardKey,
			String payloadHash, String payloadJson, long createdAt) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
			statement.setLong(1, instanceUid);
			statement.setInt(2, playerId);
			statement.setString(3, rewardKey);
			statement.setByte(4, PENDING);
			statement.setString(5, payloadHash);
			statement.setString(6, payloadJson);
			statement.setLong(7, createdAt);
			statement.executeUpdate();
		}
		try (PreparedStatement statement = connection.prepareStatement(SELECT_FOR_UPDATE)) {
			statement.setLong(1, instanceUid);
			statement.setInt(2, playerId);
			statement.setString(3, rewardKey);
			try (ResultSet row = statement.executeQuery()) {
				if (!row.next()) {
					throw new SQLException("Missing instance reward ledger row after insert");
				}
				if (!payloadHash.equals(row.getString("payload_hash"))) {
					throw new SQLException("Instance reward payload changed for " + instanceUid + ":" + playerId + ":"
							+ rewardKey);
				}
				return row.getByte("status") == COMPLETED;
			}
		}
	}

	@Override
	public void complete(Connection connection, long instanceUid, int playerId, String rewardKey, long completedAt)
			throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("UPDATE instance_reward_ledger SET status=?,"
				+ "completed_at=? WHERE instance_uid=? AND player_id=? AND reward_key=? AND status=?")) {
			statement.setByte(1, COMPLETED);
			statement.setLong(2, completedAt);
			statement.setLong(3, instanceUid);
			statement.setInt(4, playerId);
			statement.setString(5, rewardKey);
			statement.setByte(6, PENDING);
			if (statement.executeUpdate() != 1) {
				throw new SQLException("Instance reward ledger was not pending");
			}
		}
	}

	@Override
	public List<PendingReward> loadPending(int playerId) {
		List<PendingReward> rewards = new ArrayList<>();
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT instance_uid,reward_key,payload_hash,"
						+ "payload_json FROM instance_reward_ledger WHERE player_id=? AND status=? ORDER BY created_at,reward_key")) {
			statement.setInt(1, playerId);
			statement.setByte(2, PENDING);
			try (ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					rewards.add(new PendingReward(rows.getLong("instance_uid"), rows.getString("reward_key"),
							rows.getString("payload_hash"), rows.getString("payload_json")));
				}
			}
			return rewards;
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to load pending instance rewards for " + playerId, e);
		}
	}

	@Override
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return DAOUtils.supports(database, majorVersion, minorVersion);
	}
}
