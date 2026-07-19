package com.aionemu.gameserver.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.instance.DynamicInstanceMember;

public class DynamicInstancesDAO extends com.aionemu.gameserver.dao.DynamicInstancesDAO {
	private static final String INSERT_INSTANCE = "INSERT INTO dynamic_instances "
			+ "(world_id,creation_id,client_instance_id,runtime_instance_id,owner_type,owner_id,difficulty,status,spawn_page,"
			+ "created_at,active_until,empty_until,destroy_at,state_version,state_json,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String UPDATE_INSTANCE = "UPDATE dynamic_instances SET status=?,active_until=?,empty_until=?,"
			+ "destroy_at=?,state_json=?,updated_at=? WHERE instance_uid=?";
	private static final String SELECT_RECOVERABLE = "SELECT * FROM dynamic_instances WHERE status IN (?,?) "
			+ "AND (destroy_at=0 OR destroy_at>?) ORDER BY instance_uid";
	private static final String UPSERT_MEMBER = "INSERT INTO dynamic_instance_members "
			+ "(instance_uid,player_id,team_id_at_entry,side,permitted,joined_at,left_at,reentry_until,exit_world_id,exit_alias,reward_status) "
			+ "VALUES (?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE team_id_at_entry=VALUES(team_id_at_entry),"
			+ "side=VALUES(side),permitted=VALUES(permitted),joined_at=IF(joined_at=0,VALUES(joined_at),joined_at),left_at=VALUES(left_at),"
			+ "reentry_until=VALUES(reentry_until),exit_world_id=VALUES(exit_world_id),exit_alias=VALUES(exit_alias)";

	@Override
	public long create(DynamicInstance instance) {
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement(INSERT_INSTANCE, Statement.RETURN_GENERATED_KEYS)) {
			bindInstance(statement, instance);
			statement.executeUpdate();
			try (ResultSet keys = statement.getGeneratedKeys()) {
				if (!keys.next()) {
					throw new SQLException("No instance_uid generated");
				}
				return keys.getLong(1);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to create dynamic instance " + instance.getWorldId(), e);
		}
	}

	@Override
	public void update(DynamicInstance instance) {
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement(UPDATE_INSTANCE)) {
			statement.setByte(1, instance.getStatus());
			statement.setLong(2, instance.getActiveUntil());
			statement.setLong(3, instance.getEmptyUntil());
			statement.setLong(4, instance.getDestroyAt());
			statement.setString(5, instance.getStateJson());
			statement.setLong(6, instance.getUpdatedAt());
			statement.setLong(7, instance.getInstanceUid());
			if (statement.executeUpdate() != 1) {
				throw new SQLException("Unknown instance_uid " + instance.getInstanceUid());
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to update dynamic instance " + instance.getInstanceUid(), e);
		}
	}

	@Override
	public List<DynamicInstance> loadRecoverable(long now) {
		List<DynamicInstance> instances = new ArrayList<>();
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement(SELECT_RECOVERABLE)) {
			statement.setByte(1, DynamicInstance.ACTIVE);
			statement.setByte(2, DynamicInstance.EMPTY);
			statement.setLong(3, now);
			try (ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					instances.add(readInstance(rows));
				}
			}
			return instances;
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to load dynamic instances", e);
		}
	}

	@Override
	public Map<Integer, Integer> loadMaxRuntimeInstanceIds() {
		Map<Integer, Integer> result = new LinkedHashMap<>();
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT world_id,MAX(runtime_instance_id) max_id FROM dynamic_instances GROUP BY world_id");
				ResultSet rows = statement.executeQuery()) {
			while (rows.next()) {
				result.put(rows.getInt("world_id"), rows.getInt("max_id"));
			}
			return result;
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to load dynamic instance id counters", e);
		}
	}

	@Override
	public void saveMember(DynamicInstanceMember member) {
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement(UPSERT_MEMBER)) {
			statement.setLong(1, member.getInstanceUid());
			statement.setInt(2, member.getPlayerId());
			statement.setInt(3, member.getTeamIdAtEntry());
			statement.setByte(4, member.getSide());
			statement.setBoolean(5, member.isPermitted());
			statement.setLong(6, member.getJoinedAt());
			statement.setLong(7, member.getLeftAt());
			statement.setLong(8, member.getReentryUntil());
			statement.setInt(9, member.getExitWorldId());
			statement.setString(10, member.getExitAlias());
			statement.setByte(11, member.getRewardStatus());
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to save dynamic instance member " + member.getPlayerId(), e);
		}
	}

	@Override
	public void markMemberLeft(long instanceUid, int playerId, long leftAt, long reentryUntil) {
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement("UPDATE dynamic_instance_members "
						+ "SET left_at=?,reentry_until=? WHERE instance_uid=? AND player_id=?")) {
			statement.setLong(1, leftAt);
			statement.setLong(2, reentryUntil);
			statement.setLong(3, instanceUid);
			statement.setInt(4, playerId);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to mark dynamic instance member left " + playerId, e);
		}
	}

	@Override
	public void markMemberJoined(long instanceUid, int playerId, long joinedAt) {
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement("UPDATE dynamic_instance_members "
						+ "SET joined_at=IF(joined_at=0,?,joined_at),left_at=0,reentry_until=0 "
						+ "WHERE instance_uid=? AND player_id=? AND permitted=1")) {
			statement.setLong(1, joinedAt);
			statement.setLong(2, instanceUid);
			statement.setInt(3, playerId);
			if (statement.executeUpdate() != 1) {
				throw new SQLException("Missing reserved member " + instanceUid + ":" + playerId);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to mark dynamic instance member joined " + playerId, e);
		}
	}

	@Override
	public boolean hasJoined(long instanceUid, int playerId) {
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT joined_at FROM dynamic_instance_members "
						+ "WHERE instance_uid=? AND player_id=? AND permitted=1")) {
			statement.setLong(1, instanceUid);
			statement.setInt(2, playerId);
			try (ResultSet row = statement.executeQuery()) {
				return row.next() && row.getLong(1) > 0;
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to inspect dynamic instance member " + playerId, e);
		}
	}

	@Override
	public int countMembers(long instanceUid) {
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM dynamic_instance_members "
						+ "WHERE instance_uid=? AND permitted=1")) {
			statement.setLong(1, instanceUid);
			try (ResultSet row = statement.executeQuery()) {
				row.next();
				return row.getInt(1);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to count dynamic instance members " + instanceUid, e);
		}
	}

	@Override
	public void removeReservedMember(long instanceUid, int playerId) {
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM dynamic_instance_members "
						+ "WHERE instance_uid=? AND player_id=? AND joined_at=0")) {
			statement.setLong(1, instanceUid);
			statement.setInt(2, playerId);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to remove reserved dynamic instance member " + playerId, e);
		}
	}

	@Override
	public void revokeMember(long instanceUid, int playerId) {
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement("UPDATE dynamic_instance_members "
						+ "SET permitted=0,reentry_until=0 WHERE instance_uid=? AND player_id=?")) {
			statement.setLong(1, instanceUid);
			statement.setInt(2, playerId);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to revoke dynamic instance member " + playerId, e);
		}
	}

	@Override
	public List<DynamicInstanceMember> loadMembers(long instanceUid) {
		List<DynamicInstanceMember> members = new ArrayList<>();
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT * FROM dynamic_instance_members WHERE instance_uid=?")) {
			statement.setLong(1, instanceUid);
			try (ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					members.add(readMember(rows));
				}
			}
			return members;
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to load dynamic instance members " + instanceUid, e);
		}
	}

	@Override
	public Long findReentryInstanceUid(int playerId, int worldId, long now) {
		String sql = "SELECT m.instance_uid FROM dynamic_instance_members m JOIN dynamic_instances d "
				+ "ON d.instance_uid=m.instance_uid WHERE m.player_id=? AND d.world_id=? AND m.permitted=1 "
				+ "AND m.joined_at>0 AND d.status IN (?,?) AND (d.destroy_at=0 OR d.destroy_at>?) "
				+ "AND (m.left_at=0 OR m.reentry_until=0 OR m.reentry_until>=?) ORDER BY m.joined_at DESC LIMIT 1";
		try (Connection connection = DatabaseFactory.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, playerId);
			statement.setInt(2, worldId);
			statement.setByte(3, DynamicInstance.ACTIVE);
			statement.setByte(4, DynamicInstance.EMPTY);
			statement.setLong(5, now);
			statement.setLong(6, now);
			try (ResultSet rows = statement.executeQuery()) {
				return rows.next() ? rows.getLong(1) : null;
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to find dynamic instance membership for " + playerId, e);
		}
	}

	private static void bindInstance(PreparedStatement statement, DynamicInstance instance) throws SQLException {
		statement.setInt(1, instance.getWorldId());
		statement.setInt(2, instance.getCreationId());
		statement.setInt(3, instance.getClientInstanceId());
		statement.setInt(4, instance.getRuntimeInstanceId());
		statement.setByte(5, instance.getOwnerType());
		statement.setInt(6, instance.getOwnerId());
		statement.setByte(7, instance.getDifficulty());
		statement.setByte(8, instance.getStatus());
		statement.setByte(9, instance.getSpawnPage());
		statement.setLong(10, instance.getCreatedAt());
		statement.setLong(11, instance.getActiveUntil());
		statement.setLong(12, instance.getEmptyUntil());
		statement.setLong(13, instance.getDestroyAt());
		statement.setInt(14, instance.getStateVersion());
		statement.setString(15, instance.getStateJson());
		statement.setLong(16, instance.getUpdatedAt());
	}

	private static DynamicInstance readInstance(ResultSet row) throws SQLException {
		return new DynamicInstance(row.getLong("instance_uid"), row.getInt("world_id"), row.getInt("creation_id"),
				row.getInt("client_instance_id"), row.getInt("runtime_instance_id"), row.getByte("owner_type"),
				row.getInt("owner_id"), row.getByte("difficulty"), row.getByte("status"), row.getByte("spawn_page"),
				row.getLong("created_at"), row.getLong("active_until"), row.getLong("empty_until"),
				row.getLong("destroy_at"), row.getInt("state_version"), row.getString("state_json"),
				row.getLong("updated_at"));
	}

	private static DynamicInstanceMember readMember(ResultSet row) throws SQLException {
		return new DynamicInstanceMember(row.getLong("instance_uid"), row.getInt("player_id"),
				row.getInt("team_id_at_entry"), row.getByte("side"), row.getBoolean("permitted"),
				row.getLong("joined_at"), row.getLong("left_at"), row.getLong("reentry_until"),
				row.getInt("exit_world_id"), row.getString("exit_alias"), row.getByte("reward_status"));
	}

	@Override
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return DAOUtils.supports(database, majorVersion, minorVersion);
	}
}
