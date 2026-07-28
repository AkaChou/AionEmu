package com.aionemu.gameserver.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateCodec;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 使用单行原子快照持久化 PLAYER scope 任务图状态。
 * Persists PLAYER-scope quest graph state as one atomic row snapshot.
 */
public final class PlayerQuestGraphStateDAO extends com.aionemu.gameserver.dao.PlayerQuestGraphStateDAO {

	static final String SELECT_QUERY = "SELECT `quest_id`, `definition_version`, `revision`, `node_id`, `lifecycle`, "
		+ "`instance_run_id`, `next_deadline_at`, `state_payload` FROM `player_quest_graph_states` "
		+ "WHERE `player_id` = ? ORDER BY `quest_id`";
	static final String UPSERT_QUERY = "INSERT INTO `player_quest_graph_states` (`player_id`, `quest_id`, `definition_version`, "
		+ "`revision`, `node_id`, `lifecycle`, `instance_run_id`, `next_deadline_at`, `state_payload`) "
		+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
		+ "`definition_version` = IF(VALUES(`revision`) > `revision`, VALUES(`definition_version`), `definition_version`), "
		+ "`node_id` = IF(VALUES(`revision`) > `revision`, VALUES(`node_id`), `node_id`), "
		+ "`lifecycle` = IF(VALUES(`revision`) > `revision`, VALUES(`lifecycle`), `lifecycle`), "
		+ "`instance_run_id` = IF(VALUES(`revision`) > `revision`, VALUES(`instance_run_id`), `instance_run_id`), "
		+ "`next_deadline_at` = IF(VALUES(`revision`) > `revision`, VALUES(`next_deadline_at`), `next_deadline_at`), "
		+ "`state_payload` = IF(VALUES(`revision`) > `revision`, VALUES(`state_payload`), `state_payload`), "
		+ "`revision` = GREATEST(`revision`, VALUES(`revision`))";
	static final String DELETE_QUERY = "DELETE FROM `player_quest_graph_states` WHERE `player_id` = ? AND `quest_id` = ?";
	static final String INSERT_QUERY = "INSERT INTO `player_quest_graph_states` (`player_id`, `quest_id`, `definition_version`, "
		+ "`revision`, `node_id`, `lifecycle`, `instance_run_id`, `next_deadline_at`, `state_payload`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	static final String UPDATE_CAS_QUERY = "UPDATE `player_quest_graph_states` SET `definition_version` = ?, `revision` = ?, "
		+ "`node_id` = ?, `lifecycle` = ?, `instance_run_id` = ?, `next_deadline_at` = ?, `state_payload` = ? "
		+ "WHERE `player_id` = ? AND `quest_id` = ? AND `revision` = ?";

	/**
	 * 加载并严格校验玩家全部任务图状态。
	 * Loads and strictly validates all quest graph states for a player.
	 */
	@Override
	public PlayerQuestGraphStateList load(Player player) {
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		try (Connection connection = DatabaseFactory.getConnection(); PreparedStatement statement = connection.prepareStatement(SELECT_QUERY)) {
			statement.setInt(1, player.getObjectId());
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					Long instanceRunId = nullableLong(resultSet, "instance_run_id");
					Long indexedDeadline = nullableLong(resultSet, "next_deadline_at");
					PlayerQuestGraphState state = PlayerQuestGraphStateCodec.decode(resultSet.getInt("quest_id"),
						resultSet.getInt("definition_version"), resultSet.getLong("revision"), resultSet.getString("node_id"),
						instanceRunId, Lifecycle.valueOf(resultSet.getString("lifecycle")), resultSet.getBytes("state_payload"));
					if (!Objects.equals(indexedDeadline, state.nextDeadlineAt())) {
						throw new IllegalArgumentException("Quest " + state.getQuestId() + " has a mismatched deadline index");
					}
					states.addLoaded(state);
				}
			}
			return states;
		} catch (SQLException | RuntimeException e) {
			throw new IllegalStateException("Failed to load quest graph states for player " + player.getObjectId(), e);
		}
	}

	/**
	 * 在一个事务中 upsert 当前快照并删除已移除任务。
	 * Upserts the current snapshot and deletes removed quests in one transaction.
	 */
	@Override
	public void store(Player player) {
		PlayerQuestGraphStateList stateList = player.getQuestGraphStateList();
		synchronized (stateList) {
			// ponytail: 当前活动图状态很少，先全量 upsert；测得写放大后再按 revision 追踪 dirty 行。
			// Full-snapshot upsert is sufficient while active graph state is small; add dirty revisions only after measured write amplification.
			Collection<PlayerQuestGraphState> states = stateList.snapshot();
			Set<Integer> deletedQuestIds = stateList.deletedQuestIds();
			if (states.isEmpty() && deletedQuestIds.isEmpty()) {
				return;
			}
			try (Connection connection = DatabaseFactory.getConnection()) {
				connection.setAutoCommit(false);
				try {
					upsert(connection, player.getObjectId(), states);
					delete(connection, player.getObjectId(), deletedQuestIds);
					connection.commit();
				} catch (SQLException | RuntimeException e) {
					try {
						connection.rollback();
					} catch (SQLException rollbackFailure) {
						e.addSuppressed(rollbackFailure);
					}
					throw e;
				}
			} catch (SQLException | RuntimeException e) {
				throw new IllegalStateException("Failed to store quest graph states for player " + player.getObjectId(), e);
			}
			stateList.acknowledgeDeleted(deletedQuestIds);
		}
	}

	/**
	 * 使用主键插入或 revision 条件更新实现单状态 CAS。
	 * Implements single-state CAS with primary-key insertion or revision-guarded update.
	 */
	@Override
	public boolean compareAndSet(int playerId, Long expectedRevision, PlayerQuestGraphState state) {
		if (playerId <= 0 || expectedRevision != null && expectedRevision < 0) {
			throw new IllegalArgumentException("Player id/expected revision is invalid");
		}
		long nextRevision = expectedRevision == null ? 0 : Math.addExact(expectedRevision, 1);
		if (state.getRevision() != nextRevision) {
			throw new IllegalArgumentException("Quest graph CAS must advance exactly one revision");
		}
		try (Connection connection = DatabaseFactory.getConnection()) {
			if (expectedRevision == null) {
				try (PreparedStatement statement = connection.prepareStatement(INSERT_QUERY)) {
					setStateParameters(statement, playerId, state);
					return statement.executeUpdate() == 1;
				} catch (SQLIntegrityConstraintViolationException e) {
					if (e.getErrorCode() == 1062) {
						return false;
					}
					throw e;
				}
			}
			try (PreparedStatement statement = connection.prepareStatement(UPDATE_CAS_QUERY)) {
				setCasParameters(statement, playerId, expectedRevision, state);
				return statement.executeUpdate() == 1;
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to compare-and-set quest graph state for player " + playerId, e);
		}
	}

	/**
	 * 批量 upsert 当前任务图状态。
	 * Batch-upserts the current quest graph states.
	 */
	private static void upsert(Connection connection, int playerId, Collection<PlayerQuestGraphState> states) throws SQLException {
		if (states.isEmpty()) {
			return;
		}
		try (PreparedStatement statement = connection.prepareStatement(UPSERT_QUERY)) {
			for (PlayerQuestGraphState state : states) {
				setStateParameters(statement, playerId, state);
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	/**
	 * 批量删除已移除的任务图状态。
	 * Batch-deletes removed quest graph states.
	 */
	private static void delete(Connection connection, int playerId, Set<Integer> questIds) throws SQLException {
		if (questIds.isEmpty()) {
			return;
		}
		try (PreparedStatement statement = connection.prepareStatement(DELETE_QUERY)) {
			for (int questId : questIds) {
				statement.setInt(1, playerId);
				statement.setInt(2, questId);
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	/**
	 * 绑定单行任务图状态参数。
	 * Binds one quest graph state row.
	 */
	private static void setStateParameters(PreparedStatement statement, int playerId, PlayerQuestGraphState state) throws SQLException {
		statement.setInt(1, playerId);
		statement.setInt(2, state.getQuestId());
		statement.setInt(3, state.getDefinitionVersion());
		statement.setLong(4, state.getRevision());
		statement.setString(5, state.getNodeId());
		statement.setString(6, state.getLifecycle().name());
		setNullableLong(statement, 7, state.getInstanceRunId());
		setNullableLong(statement, 8, state.nextDeadlineAt());
		statement.setBytes(9, PlayerQuestGraphStateCodec.encode(state));
	}

	/**
	 * 绑定 revision 条件更新参数。
	 * Binds revision-guarded update parameters.
	 */
	private static void setCasParameters(PreparedStatement statement, int playerId, long expectedRevision, PlayerQuestGraphState state)
			throws SQLException {
		statement.setInt(1, state.getDefinitionVersion());
		statement.setLong(2, state.getRevision());
		statement.setString(3, state.getNodeId());
		statement.setString(4, state.getLifecycle().name());
		setNullableLong(statement, 5, state.getInstanceRunId());
		setNullableLong(statement, 6, state.nextDeadlineAt());
		statement.setBytes(7, PlayerQuestGraphStateCodec.encode(state));
		statement.setInt(8, playerId);
		statement.setInt(9, state.getQuestId());
		statement.setLong(10, expectedRevision);
	}

	/**
	 * 读取可空 BIGINT。
	 * Reads a nullable BIGINT.
	 */
	private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
		long value = resultSet.getLong(column);
		return resultSet.wasNull() ? null : value;
	}

	/**
	 * 绑定可空 BIGINT。
	 * Binds a nullable BIGINT.
	 */
	private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
		if (value == null) {
			statement.setNull(index, Types.BIGINT);
		} else {
			statement.setLong(index, value);
		}
	}

	/**
	 * 判断当前数据库是否由 MySQL DAO 支持。
	 * Returns whether the current database is supported by this MySQL DAO.
	 */
	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
