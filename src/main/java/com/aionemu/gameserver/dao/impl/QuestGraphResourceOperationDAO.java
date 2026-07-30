package com.aionemu.gameserver.dao.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.util.Map;
import java.util.TreeMap;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.EscortResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.InstanceSpawnResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateCodec;

/** MySQL implementation of the durable quest graph resource-operation registry. */
public final class QuestGraphResourceOperationDAO extends com.aionemu.gameserver.dao.QuestGraphResourceOperationDAO {
	static final String SELECT_QUERY = "SELECT `operation_key`, `resource_payload` FROM `quest_graph_resource_operations` "
		+ "WHERE `player_id` = ? AND `operation_hash` = ?";
	static final String INSERT_QUERY = "INSERT INTO `quest_graph_resource_operations` (`player_id`, `operation_hash`, "
		+ "`operation_key`, `quest_id`, `capability`, `reserved_object_id`, `resource_payload`) VALUES (?, ?, ?, ?, ?, ?, ?)";
	static final String DELETE_QUERY = "DELETE FROM `quest_graph_resource_operations` WHERE `player_id` = ? "
		+ "AND `operation_hash` = ? AND `operation_key` = ? AND `resource_payload` = ?";
	/** Full rows are decoded during IDFactory startup so a corrupt registry cannot be ignored. */
	static final String USED_IDS_QUERY = "SELECT `player_id`, `operation_key`, `reserved_object_id`, `resource_payload` "
		+ "FROM `quest_graph_resource_operations` ORDER BY `player_id`, `operation_hash`";

	@Override
	public CleanupLease find(int playerId, String operationKey) {
		validateOwnerAndKey(playerId, operationKey);
		try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_QUERY)) {
			statement.setInt(1, playerId);
			statement.setBytes(2, hash(operationKey));
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return null;
				}
				String storedKey = resultSet.getString("operation_key");
				byte[] payload = resultSet.getBytes("resource_payload");
				if (!operationKey.equals(storedKey) || resultSet.next()) {
					throw new IllegalStateException("Quest graph resource operation hash collision or duplicate row");
				}
				CleanupLease lease = PlayerQuestGraphStateCodec.decodeCleanupLease(payload);
				validateLeaseOwner(lease, playerId, operationKey);
				return lease;
			}
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to load quest graph resource operation for player " + playerId, e);
		}
	}

	@Override
	public CleanupLease reserve(CleanupLease candidate) {
		int playerId = validateLease(candidate);
		byte[] payload = PlayerQuestGraphStateCodec.encodeCleanupLease(candidate);
		try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(INSERT_QUERY)) {
			statement.setInt(1, playerId);
			statement.setBytes(2, hash(candidate.resourceKey()));
			statement.setString(3, candidate.resourceKey());
			statement.setInt(4, candidate.identity().questId());
			statement.setString(5, candidate.capability());
			Integer reservedObjectId = ownsObjectId(candidate) ? candidate.identity().objectId() : null;
			if (reservedObjectId == null) {
				statement.setNull(6, Types.INTEGER);
			} else {
				statement.setInt(6, reservedObjectId);
			}
			statement.setBytes(7, payload);
			if (statement.executeUpdate() != 1) {
				throw new IllegalStateException("Quest graph resource operation insert changed an unexpected row count");
			}
			return candidate;
		} catch (SQLIntegrityConstraintViolationException e) {
			if (e.getErrorCode() != 1062) {
				throw new IllegalStateException("Failed to reserve quest graph resource operation", e);
			}
			CleanupLease existing = find(playerId, candidate.resourceKey());
			if (existing == null) {
				throw new ObjectIdReservationConflictException(e);
			}
			return existing;
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to reserve quest graph resource operation", e);
		}
	}

	@Override
	public boolean release(CleanupLease expected) {
		int playerId = validateLease(expected);
		try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(DELETE_QUERY)) {
			statement.setInt(1, playerId);
			statement.setBytes(2, hash(expected.resourceKey()));
			statement.setString(3, expected.resourceKey());
			statement.setBytes(4, PlayerQuestGraphStateCodec.encodeCleanupLease(expected));
			return statement.executeUpdate() == 1;
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to release quest graph resource operation", e);
		}
	}

	@Override
	public int[] getUsedIDs() {
		return getUsedResourceLeases().keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
	}

	@Override
	public Map<Integer, CleanupLease> getUsedResourceLeases() {
		try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(USED_IDS_QUERY);
				ResultSet resultSet = statement.executeQuery()) {
			Map<Integer, CleanupLease> leases = new TreeMap<>();
			while (resultSet.next()) {
				int playerId = resultSet.getInt("player_id");
				String operationKey = resultSet.getString("operation_key");
				CleanupLease lease = PlayerQuestGraphStateCodec.decodeCleanupLease(resultSet.getBytes("resource_payload"));
				validateLeaseOwner(lease, playerId, operationKey);
				int reservedObjectId = resultSet.getInt("reserved_object_id");
				boolean hasReservedObjectId = !resultSet.wasNull();
				if (hasReservedObjectId != ownsObjectId(lease)
						|| hasReservedObjectId && reservedObjectId != lease.identity().objectId()) {
					throw new IllegalArgumentException("Quest graph resource operation reserved object index is corrupt");
				}
				if (hasReservedObjectId) {
					if (leases.putIfAbsent(reservedObjectId, lease) != null) {
						throw new IllegalArgumentException("Duplicate quest graph resource operation object id " + reservedObjectId);
					}
				}
			}
			return Map.copyOf(leases);
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to load reserved quest graph resource object ids", e);
		}
	}

	static boolean ownsObjectId(CleanupLease lease) {
		return lease.identity() instanceof InstanceSpawnResourceIdentity
			|| lease.identity() instanceof EscortResourceIdentity;
	}

	private static int validateLease(CleanupLease lease) {
		if (lease == null || lease.identity() == null || !lease.identity().materialized()) {
			throw new IllegalArgumentException("Resource operation lease is unresolved");
		}
		validateLeaseOwner(lease, lease.identity().playerId(), lease.resourceKey());
		return lease.identity().playerId();
	}

	private static void validateLeaseOwner(CleanupLease lease, int playerId, String operationKey) {
		validateOwnerAndKey(playerId, operationKey);
		if (lease.identity().playerId() != playerId || !lease.resourceKey().equals(operationKey)
				|| !lease.identity().idempotencyKey().equals(operationKey)) {
			throw new IllegalArgumentException("Resource operation owner or key does not match its typed identity");
		}
	}

	private static void validateOwnerAndKey(int playerId, String operationKey) {
		if (playerId <= 0 || operationKey == null || operationKey.isBlank()
				|| operationKey.length() > MAX_OPERATION_KEY_LENGTH) {
			throw new IllegalArgumentException("Resource operation owner/key is invalid");
		}
	}

	private static byte[] hash(String operationKey) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(operationKey.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
