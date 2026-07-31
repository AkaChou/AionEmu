package com.aionemu.gameserver.dao.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphZoneMissionSignalBridge.Signal;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphZoneMissionSignalBridge.SignalClaim;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphZoneMissionSignalBridge.SignalLease;

/** MySQL durable ledger for cross-owner zone-mission signals. */
public final class QuestGraphZoneMissionSignalDAO extends com.aionemu.gameserver.dao.QuestGraphZoneMissionSignalDAO {
	private static final long CLAIM_LEASE_MILLIS = 30_000;

	static final String SELECT_QUERY = "SELECT `event_id`, `status`, `claim_generation`, `lease_until` FROM `quest_graph_zone_mission_signals` "
		+ "WHERE `player_id` = ? AND `event_hash` = ?";
	static final String INSERT_QUERY = "INSERT INTO `quest_graph_zone_mission_signals` (`player_id`, `event_hash`, `event_id`, "
		+ "`occurred_at`, `source_quest_id`, `target_quest_id`, `status`, `claim_generation`, `lease_until`) "
		+ "VALUES (?, ?, ?, ?, ?, ?, 'ACCEPTED', 1, ?)";
	static final String RECLAIM_QUERY = "UPDATE `quest_graph_zone_mission_signals` SET `claim_generation` = `claim_generation` + 1, "
		+ "`lease_until` = ? WHERE `player_id` = ? AND `event_hash` = ? AND BINARY `event_id` = BINARY ? "
		+ "AND `status` = 'ACCEPTED' AND `lease_until` <= ? AND `claim_generation` < 9223372036854775807";
	static final String ACK_QUERY = "UPDATE `quest_graph_zone_mission_signals` SET `status` = 'ACKED' "
		+ "WHERE `player_id` = ? AND `event_hash` = ? AND BINARY `event_id` = BINARY ? AND `status` = 'ACCEPTED' "
		+ "AND `claim_generation` = ?";

	@Override
	public SignalLease accept(Signal signal) {
		validate(signal);
		long now = System.currentTimeMillis();
		long leaseUntil = Math.addExact(now, CLAIM_LEASE_MILLIS);
		try (Connection connection = DatabaseFactory.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement(INSERT_QUERY)) {
				statement.setInt(1, signal.playerId());
				statement.setBytes(2, hash(signal.eventId()));
				statement.setString(3, signal.eventId());
				statement.setLong(4, signal.occurredAt());
				statement.setInt(5, signal.sourceQuestId());
				statement.setInt(6, signal.targetQuestId());
				statement.setLong(7, leaseUntil);
				try {
					if (statement.executeUpdate() == 1) {
						return SignalLease.applied(1);
					}
				} catch (SQLIntegrityConstraintViolationException duplicate) {
					// Resolve the exact existing row below; a hash collision is rejected there.
				}
			}
			ExistingSignal existing = existing(connection, signal);
			if (existing.status() == SignalClaim.ALREADY_APPLIED) {
				return SignalLease.of(SignalClaim.ALREADY_APPLIED);
			}
			if (existing.leaseUntil() > now) {
				return SignalLease.of(SignalClaim.BUSY);
			}
			try (PreparedStatement statement = connection.prepareStatement(RECLAIM_QUERY)) {
				statement.setLong(1, leaseUntil);
				statement.setInt(2, signal.playerId());
				statement.setBytes(3, hash(signal.eventId()));
				statement.setString(4, signal.eventId());
				statement.setLong(5, now);
				if (statement.executeUpdate() == 1) {
					return SignalLease.applied(Math.addExact(existing.claimGeneration(), 1));
				}
			}
			ExistingSignal current = existing(connection, signal);
			return SignalLease.of(current.status() == SignalClaim.ALREADY_APPLIED ? SignalClaim.ALREADY_APPLIED : SignalClaim.BUSY);
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to accept quest graph zone-mission signal", e);
		}
	}

	@Override
	public SignalClaim acknowledge(Signal signal, long claimGeneration) {
		validate(signal);
		if (claimGeneration <= 0) {
			return SignalClaim.REJECTED;
		}
		try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(ACK_QUERY)) {
			statement.setInt(1, signal.playerId());
			statement.setBytes(2, hash(signal.eventId()));
			statement.setString(3, signal.eventId());
			statement.setLong(4, claimGeneration);
			if (statement.executeUpdate() == 1) {
				return SignalClaim.APPLIED;
			}
			return existing(connection, signal).status() == SignalClaim.ALREADY_APPLIED ? SignalClaim.ALREADY_APPLIED : SignalClaim.REJECTED;
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to acknowledge quest graph zone-mission signal", e);
		}
	}

	private static ExistingSignal existing(Connection connection, Signal signal) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(SELECT_QUERY)) {
			statement.setInt(1, signal.playerId());
			statement.setBytes(2, hash(signal.eventId()));
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					throw new IllegalStateException("Zone-mission signal disappeared after duplicate insert");
				}
				if (!signal.eventId().equals(resultSet.getString("event_id")) || resultSet.next()) {
					throw new IllegalStateException("Zone-mission signal hash collision or duplicate row");
				}
				String status = resultSet.getString("status");
				if (!"ACCEPTED".equals(status) && !"ACKED".equals(status)) {
					throw new IllegalStateException("Unknown zone-mission signal status " + status);
				}
				return new ExistingSignal("ACKED".equals(status) ? SignalClaim.ALREADY_APPLIED : SignalClaim.BUSY,
					resultSet.getLong("claim_generation"), resultSet.getLong("lease_until"));
			}
		}
	}

	private record ExistingSignal(SignalClaim status, long claimGeneration, long leaseUntil) {
		private ExistingSignal {
			if (status != SignalClaim.BUSY && status != SignalClaim.ALREADY_APPLIED || claimGeneration <= 0 || leaseUntil <= 0) {
				throw new IllegalArgumentException("Persisted zone-mission signal claim is invalid");
			}
		}
	}

	private static void validate(Signal signal) {
		if (signal == null) {
			throw new IllegalArgumentException("Zone-mission signal is missing");
		}
	}

	private static byte[] hash(String eventId) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(eventId.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
