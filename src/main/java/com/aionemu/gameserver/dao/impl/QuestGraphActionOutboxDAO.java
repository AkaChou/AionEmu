package com.aionemu.gameserver.dao.impl;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxCodec;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord.Status;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand;

/** MySQL implementation of the durable quest graph action outbox. */
public final class QuestGraphActionOutboxDAO extends com.aionemu.gameserver.dao.QuestGraphActionOutboxDAO {

	static final String SELECT_COLUMNS = "`outbox_sequence`, `player_id`, `operation_hash`, `operation_key`, `quest_id`, `base_revision`, `transition_id`, "
		+ "`action_index`, `command_payload`, `status`, `claim_generation`, `lease_until`, `accepted_at`, `completed_at`, `graph_acked`";
	static final String SELECT_QUERY = "SELECT " + SELECT_COLUMNS + " FROM `quest_graph_action_outbox` "
		+ "WHERE `player_id` = ? AND `operation_hash` = ?";
	static final String LOCK_PLAYER_QUERY = "SELECT `id` FROM `players` WHERE `id` = ? FOR UPDATE";
	static final String SELECT_DELIVERY_HEAD_QUERY = "SELECT " + SELECT_COLUMNS + " FROM `quest_graph_action_outbox` "
		+ "WHERE `player_id` = ? ORDER BY `outbox_sequence` LIMIT 1 FOR UPDATE";
	static final String INSERT_QUERY = "INSERT INTO `quest_graph_action_outbox` (`player_id`, `operation_hash`, `operation_key`, "
		+ "`quest_id`, `base_revision`, `transition_id`, `action_index`, `command_payload`, `status`, `claim_generation`, "
		+ "`lease_until`, `accepted_at`, `completed_at`, `graph_acked`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACCEPTED', 0, NULL, ?, NULL, 0)";
	static final String CLAIM_QUERY = "UPDATE `quest_graph_action_outbox` SET `status` = 'CLAIMED', "
		+ "`claim_generation` = `claim_generation` + 1, `lease_until` = ? WHERE `player_id` = ? AND `operation_hash` = ? "
		+ "AND BINARY `operation_key` = BINARY ? AND `completed_at` IS NULL AND `claim_generation` < 9223372036854775807 "
		+ "AND (`status` = 'ACCEPTED' OR (`status` = 'CLAIMED' AND `lease_until` <= ?))";
	static final String RECOVERY_CLAIM_QUERY = "UPDATE `quest_graph_action_outbox` SET `status` = 'CLAIMED', "
		+ "`claim_generation` = `claim_generation` + 1, `lease_until` = ? WHERE `player_id` = ? AND `operation_hash` = ? "
		+ "AND BINARY `operation_key` = BINARY ? AND `completed_at` IS NULL AND `claim_generation` < 9223372036854775807 "
		+ "AND `status` IN ('ACCEPTED', 'CLAIMED')";
	static final String COMPLETE_QUERY = "UPDATE `quest_graph_action_outbox` SET `status` = IF(`graph_acked` = 1, "
		+ "'GRAPH_ACKED', 'COMPLETED'), `lease_until` = NULL, `completed_at` = ? WHERE `player_id` = ? "
		+ "AND `operation_hash` = ? AND BINARY `operation_key` = BINARY ? AND `status` = 'CLAIMED' AND `claim_generation` = ? "
		+ "AND `lease_until` > ? AND `completed_at` IS NULL";
	static final String CURRENT_CLAIM_QUERY = "SELECT 1 FROM `quest_graph_action_outbox` WHERE `player_id` = ? "
		+ "AND `operation_hash` = ? AND BINARY `operation_key` = BINARY ? AND `status` = 'CLAIMED' "
		+ "AND `claim_generation` = ? AND `lease_until` > ? AND `completed_at` IS NULL";
	static final String PERSIST_PLAYER_LOCATION_QUERY = "UPDATE `players` SET `x` = ?, `y` = ?, `z` = ?, `heading` = ?, `world_id` = ? "
		+ "WHERE `id` = ?";
	static final String ACK_GRAPH_QUERY = "UPDATE `quest_graph_action_outbox` SET `graph_acked` = 1, "
		+ "`status` = IF(`completed_at` IS NULL, `status`, 'GRAPH_ACKED') WHERE `player_id` = ? AND `operation_hash` = ? "
		+ "AND BINARY `operation_key` = BINARY ? AND `graph_acked` = 0";
	static final String LIST_PENDING_QUERY = "SELECT " + SELECT_COLUMNS + " FROM `quest_graph_action_outbox` "
		+ "WHERE `player_id` = ? ORDER BY `outbox_sequence`";
	static final String DELETE_ACKED_QUERY = "DELETE FROM `quest_graph_action_outbox` WHERE `player_id` = ? "
		+ "AND `operation_hash` = ? AND BINARY `operation_key` = BINARY ? AND `command_payload` = ? "
		+ "AND `status` = 'GRAPH_ACKED' AND `graph_acked` = 1 AND `completed_at` IS NOT NULL";

	@Override
	public QuestGraphActionOutboxRecord acceptExact(TeleportOutboxCommand command, long acceptedAt) {
		if (command == null || acceptedAt <= 0) {
			throw new IllegalArgumentException("Quest graph action outbox acceptance is invalid");
		}
		byte[] payload = QuestGraphActionOutboxCodec.encode(command);
		try (Connection connection = DatabaseFactory.getConnection()) {
			return inTransaction(connection, () -> {
				lockPlayer(connection, command.playerId());
				try (PreparedStatement statement = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS)) {
					statement.setInt(1, command.playerId());
					statement.setBytes(2, hash(command.operationKey()));
					statement.setString(3, command.operationKey());
					statement.setInt(4, command.questId());
					statement.setLong(5, command.baseRevision());
					statement.setString(6, command.transitionId());
					statement.setInt(7, command.actionIndex());
					statement.setBytes(8, payload);
					statement.setLong(9, acceptedAt);
					try {
						if (statement.executeUpdate() != 1) {
							throw new IllegalStateException("Quest graph action outbox insert changed an unexpected row count");
						}
					} catch (SQLException e) {
						if (!isDuplicateKey(e)) {
							throw e;
						}
						QuestGraphActionOutboxRecord existing = find(connection, command.playerId(), command.operationKey());
						if (!hasExactPayload(existing, payload)) {
							throw new OperationConflictException(command.operationKey());
						}
						return existing;
					}
					long sequence = generatedSequence(statement);
					QuestGraphActionOutboxRecord inserted = find(connection, command.playerId(), command.operationKey());
					if (inserted == null || inserted.outboxSequence() != sequence || inserted.acceptedAt() != acceptedAt) {
						throw new IllegalStateException("Quest graph action outbox insert did not materialize its durable sequence");
					}
					return inserted;
				}
			});
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to accept quest graph action outbox command", e);
		} catch (RuntimeException e) {
			throw e;
		}
	}

	@Override
	public QuestGraphActionOutboxRecord find(int playerId, String operationKey) {
		validateOwnerAndKey(playerId, operationKey);
		try (Connection connection = DatabaseFactory.getConnection()) {
			return find(connection, playerId, operationKey);
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to load quest graph action outbox command for player " + playerId, e);
		}
	}

	@Override
	public QuestGraphActionOutboxRecord claim(int playerId, String operationKey, long now, long leaseUntil) {
		validateOwnerAndKey(playerId, operationKey);
		if (now <= 0 || leaseUntil <= now) {
			throw new IllegalArgumentException("Quest graph action outbox lease is invalid");
		}
		try (Connection connection = DatabaseFactory.getConnection()) {
			return inTransaction(connection, () -> claimHead(connection, playerId, operationKey, now, leaseUntil, false));
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to claim quest graph action outbox command", e);
		}
	}

	@Override
	public QuestGraphActionOutboxRecord reclaimForRecovery(int playerId, String operationKey, long now, long leaseUntil) {
		validateOwnerAndKey(playerId, operationKey);
		if (now <= 0 || leaseUntil <= now) {
			throw new IllegalArgumentException("Quest graph action outbox recovery lease is invalid");
		}
		try (Connection connection = DatabaseFactory.getConnection()) {
			return inTransaction(connection, () -> claimHead(connection, playerId, operationKey, now, leaseUntil, true));
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to reclaim quest graph action outbox command for recovery", e);
		}
	}

	private static QuestGraphActionOutboxRecord claimHead(Connection connection, int playerId, String operationKey, long now,
			long leaseUntil, boolean recovery) throws SQLException {
		QuestGraphActionOutboxRecord head = findDeliveryHead(connection, playerId);
		if (head == null || !head.command().operationKey().equals(operationKey)) {
			return null;
		}
		String query = recovery ? RECOVERY_CLAIM_QUERY : CLAIM_QUERY;
		int changed;
		try (PreparedStatement statement = connection.prepareStatement(query)) {
			statement.setLong(1, leaseUntil);
			statement.setInt(2, playerId);
			statement.setBytes(3, hash(operationKey));
			statement.setString(4, operationKey);
			if (!recovery) {
				statement.setLong(5, now);
			}
			changed = statement.executeUpdate();
		}
		QuestGraphActionOutboxRecord claimed = find(connection, playerId, operationKey);
		if (changed == 0) {
			return null;
		}
		if (changed != 1 || claimed == null || claimed.status() != Status.CLAIMED
				|| claimed.outboxSequence() != head.outboxSequence() || !Long.valueOf(leaseUntil).equals(claimed.leaseUntil())) {
			throw new IllegalStateException("Quest graph action outbox claim did not materialize its exact head lease");
		}
		return claimed;
	}

	@Override
	public boolean complete(int playerId, String operationKey, long claimGeneration, long completedAt) {
		validateOwnerAndKey(playerId, operationKey);
		if (claimGeneration <= 0 || completedAt <= 0) {
			throw new IllegalArgumentException("Quest graph action outbox completion is invalid");
		}
		try (Connection connection = DatabaseFactory.getConnection()) {
			return inTransaction(connection, () -> {
				lockPlayer(connection, playerId);
				int changed;
				try (PreparedStatement statement = connection.prepareStatement(COMPLETE_QUERY)) {
					statement.setLong(1, completedAt);
					statement.setInt(2, playerId);
					statement.setBytes(3, hash(operationKey));
					statement.setString(4, operationKey);
					statement.setLong(5, claimGeneration);
					statement.setLong(6, completedAt);
					changed = statement.executeUpdate();
				}
				QuestGraphActionOutboxRecord completed = find(connection, playerId, operationKey);
				if (changed == 0) {
					return isCompletedGeneration(completed, claimGeneration);
				}
				if (changed != 1 || !isCompletedGeneration(completed, claimGeneration)) {
					throw new IllegalStateException("Quest graph action outbox completion did not materialize its generation");
				}
				persistPlayerLocation(connection, completed.command());
				return true;
			});
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to complete quest graph action outbox command", e);
		}
	}

	@Override
	public boolean isCurrentClaim(int playerId, String operationKey, long claimGeneration, long now) {
		validateOwnerAndKey(playerId, operationKey);
		if (claimGeneration <= 0 || now <= 0) {
			throw new IllegalArgumentException("Quest graph action outbox claim authorization is invalid");
		}
		try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(CURRENT_CLAIM_QUERY)) {
			statement.setInt(1, playerId);
			statement.setBytes(2, hash(operationKey));
			statement.setString(3, operationKey);
			statement.setLong(4, claimGeneration);
			statement.setLong(5, now);
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to authorize quest graph action outbox claim", e);
		}
	}

	@Override
	public boolean ackGraph(int playerId, String operationKey) {
		validateOwnerAndKey(playerId, operationKey);
		try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(ACK_GRAPH_QUERY)) {
			statement.setInt(1, playerId);
			statement.setBytes(2, hash(operationKey));
			statement.setString(3, operationKey);
			if (statement.executeUpdate() == 1) {
				return true;
			}
			QuestGraphActionOutboxRecord existing = find(connection, playerId, operationKey);
			return existing != null && existing.graphAcked();
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to acknowledge quest graph action outbox command", e);
		}
	}

	@Override
	public List<QuestGraphActionOutboxRecord> listPendingForPlayer(int playerId) {
		validateOwner(playerId);
		try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(LIST_PENDING_QUERY)) {
			statement.setInt(1, playerId);
			try (ResultSet resultSet = statement.executeQuery()) {
				List<QuestGraphActionOutboxRecord> records = new ArrayList<>();
				while (resultSet.next()) {
					records.add(readRecord(resultSet, playerId, null));
				}
				return List.copyOf(records);
			}
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to list pending quest graph action outbox commands", e);
		}
	}

	@Override
	public boolean deleteAcked(int playerId, String operationKey) {
		validateOwnerAndKey(playerId, operationKey);
		QuestGraphActionOutboxRecord expected = find(playerId, operationKey);
		if (expected == null || !expected.deletable()) {
			return false;
		}
		try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(DELETE_ACKED_QUERY)) {
			statement.setInt(1, playerId);
			statement.setBytes(2, hash(operationKey));
			statement.setString(3, operationKey);
			statement.setBytes(4, QuestGraphActionOutboxCodec.encode(expected.command()));
			boolean deleted = statement.executeUpdate() == 1;
			if (!deleted) {
				find(connection, playerId, operationKey);
			}
			return deleted;
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to delete acknowledged quest graph action outbox command", e);
		}
	}

	private static QuestGraphActionOutboxRecord find(Connection connection, int playerId, String operationKey) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(SELECT_QUERY)) {
			statement.setInt(1, playerId);
			statement.setBytes(2, hash(operationKey));
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return null;
				}
				QuestGraphActionOutboxRecord record = readRecord(resultSet, playerId, operationKey);
				if (resultSet.next()) {
					throw new IllegalStateException("Quest graph action outbox contains duplicate operation rows");
				}
				return record;
			}
		}
	}

	private static QuestGraphActionOutboxRecord findDeliveryHead(Connection connection, int playerId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(SELECT_DELIVERY_HEAD_QUERY)) {
			statement.setInt(1, playerId);
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next() ? readRecord(resultSet, playerId, null) : null;
			}
		}
	}

	private static void lockPlayer(Connection connection, int playerId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(LOCK_PLAYER_QUERY)) {
			statement.setInt(1, playerId);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					throw new IllegalArgumentException("Quest graph action outbox player does not exist");
				}
			}
		}
	}

	private static long generatedSequence(PreparedStatement statement) throws SQLException {
		try (ResultSet resultSet = statement.getGeneratedKeys()) {
			if (!resultSet.next()) {
				throw new IllegalStateException("Quest graph action outbox insert did not return its durable sequence");
			}
			long sequence = resultSet.getLong(1);
			if (sequence <= 0 || resultSet.wasNull() || resultSet.next()) {
				throw new IllegalStateException("Quest graph action outbox generated sequence is invalid");
			}
			return sequence;
		}
	}

	private static void rollback(Connection connection, Exception failure) {
		try {
			connection.rollback();
		} catch (SQLException rollbackFailure) {
			failure.addSuppressed(rollbackFailure);
		}
	}

	static <T> T inTransaction(Connection connection, TransactionWork<T> work) throws SQLException {
		connection.setAutoCommit(false);
		try {
			T result = work.execute();
			connection.commit();
			return result;
		} catch (SQLException | RuntimeException e) {
			rollback(connection, e);
			throw e;
		}
	}

	private static void persistPlayerLocation(Connection connection, TeleportOutboxCommand command) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(PERSIST_PLAYER_LOCATION_QUERY)) {
			statement.setFloat(1, command.x());
			statement.setFloat(2, command.y());
			statement.setFloat(3, command.z());
			statement.setByte(4, command.heading());
			statement.setInt(5, command.worldId());
			statement.setInt(6, command.playerId());
			if (statement.executeUpdate() != 1) {
				throw new IllegalStateException("Teleport outbox completion could not persist the player location");
			}
		}
	}

	static boolean hasExactPayload(QuestGraphActionOutboxRecord existing, byte[] candidatePayload) {
		return existing != null && candidatePayload != null && MessageDigest.isEqual(candidatePayload,
			QuestGraphActionOutboxCodec.encode(existing.command()));
	}

	static boolean isCompletedGeneration(QuestGraphActionOutboxRecord existing, long claimGeneration) {
		return existing != null && existing.claimGeneration() == claimGeneration && existing.completedAt() != null;
	}

	private static QuestGraphActionOutboxRecord readRecord(ResultSet resultSet, int expectedPlayerId, String expectedKey) throws SQLException {
		int playerId = resultSet.getInt("player_id");
		String operationKey = resultSet.getString("operation_key");
		TeleportOutboxCommand command = QuestGraphActionOutboxCodec.decode(resultSet.getBytes("command_payload"));
		if (playerId != expectedPlayerId || !matchesOperationHash(operationKey, resultSet.getBytes("operation_hash"))
				|| expectedKey != null && !expectedKey.equals(operationKey)
				|| command.playerId() != playerId || !command.operationKey().equals(operationKey)
				|| command.questId() != resultSet.getInt("quest_id") || command.baseRevision() != resultSet.getLong("base_revision")
				|| !command.transitionId().equals(resultSet.getString("transition_id"))
				|| command.actionIndex() != resultSet.getInt("action_index")) {
			throw new IllegalArgumentException("Quest graph action outbox row does not match its typed command");
		}
		Status status;
		try {
			status = Status.valueOf(resultSet.getString("status"));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new IllegalArgumentException("Quest graph action outbox status is invalid", e);
		}
		int graphAckedValue = resultSet.getInt("graph_acked");
		if (resultSet.wasNull() || graphAckedValue < 0 || graphAckedValue > 1) {
			throw new IllegalArgumentException("Quest graph action outbox graph acknowledgement is invalid");
		}
		return new QuestGraphActionOutboxRecord(command, resultSet.getLong("outbox_sequence"), status, resultSet.getLong("claim_generation"),
			getNullableLong(resultSet, "lease_until"), resultSet.getLong("accepted_at"),
			getNullableLong(resultSet, "completed_at"), graphAckedValue == 1);
	}

	private static Long getNullableLong(ResultSet resultSet, String column) throws SQLException {
		long value = resultSet.getLong(column);
		return resultSet.wasNull() ? null : value;
	}

	private static void validateOwnerAndKey(int playerId, String operationKey) {
		validateOwner(playerId);
		if (operationKey == null || operationKey.isBlank()
				|| operationKey.length() > TeleportOutboxCommand.MAX_OPERATION_KEY_LENGTH) {
			throw new IllegalArgumentException("Quest graph action outbox operation key is invalid");
		}
	}

	private static void validateOwner(int playerId) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Quest graph action outbox player id is invalid");
		}
	}

	static byte[] hash(String operationKey) {
		try {
			ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(operationKey));
			byte[] bytes = new byte[encoded.remaining()];
			encoded.get(bytes);
			return MessageDigest.getInstance("SHA-256").digest(bytes);
		} catch (CharacterCodingException e) {
			throw new IllegalArgumentException("Quest graph action outbox operation key is not valid UTF-8 text", e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	static boolean matchesOperationHash(String operationKey, byte[] storedHash) {
		return operationKey != null && storedHash != null && storedHash.length == 32
			&& MessageDigest.isEqual(storedHash, hash(operationKey));
	}

	static boolean isDuplicateKey(SQLException exception) {
		for (SQLException current = exception; current != null; current = current.getNextException()) {
			if (current.getErrorCode() == 1062) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}

	@FunctionalInterface
	interface TransactionWork<T> {
		T execute() throws SQLException;
	}
}
