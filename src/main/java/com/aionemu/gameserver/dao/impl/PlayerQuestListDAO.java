package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import java.sql.*;
import java.util.Collection;

/**
 * 玩家任务列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerQuestListDAO.
 *
 * @author MrPoke
 * @modified vlog, Rolandas
 * @updated for MySQL 8 with optimizations
 */
@Slf4j
public class PlayerQuestListDAO extends com.aionemu.gameserver.dao.PlayerQuestListDAO {

	/** 查询玩家任务列表 / Select player quest list */
	private static final String SELECT_QUERY = "SELECT `quest_id`, `status`, `quest_vars`, `complete_count`, `next_repeat_time`, `reward`, `complete_time` FROM `player_quests` WHERE `player_id` = ?";
	/** 更新玩家任务状态 / Update player quest state */
	private static final String UPDATE_QUERY = "UPDATE `player_quests` SET `status` = ?, `quest_vars` = ?, `complete_count` = ?, `next_repeat_time` = ?, `reward` = ?, `complete_time` = ? WHERE `player_id` = ? AND `quest_id` = ?";
	/** 删除玩家任务 / Delete player quest */
	private static final String DELETE_QUERY = "DELETE FROM `player_quests` WHERE `player_id` = ? AND `quest_id` = ?";
	/** 插入新玩家任务 / Insert new player quest */
	private static final String INSERT_QUERY = "INSERT INTO `player_quests` (`player_id`, `quest_id`, `status`, `quest_vars`, `complete_count`, `next_repeat_time`, `reward`, `complete_time`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	/** 批处理大小 / Batch size */
	private static final int BATCH_SIZE = 100;

	/** 筛选需新增的任务状态 / Filter quests to insert */
	private static final Predicate<QuestState> questsToAddPredicate = new Predicate<QuestState>() {
		@Override
		public boolean apply(QuestState input) {
			return input != null && PersistentState.NEW == input.getPersistentState();
		}
	};

	/** 筛选需更新的任务状态 / Filter quests to update */
	private static final Predicate<QuestState> questsToUpdatePredicate = new Predicate<QuestState>() {
		@Override
		public boolean apply(QuestState input) {
			return input != null && PersistentState.UPDATE_REQUIRED == input.getPersistentState();
		}
	};

	/** 筛选需删除的任务状态 / Filter quests to delete */
	private static final Predicate<QuestState> questsToDeletePredicate = new Predicate<QuestState>() {
		@Override
		public boolean apply(QuestState input) {
			return input != null && PersistentState.DELETED == input.getPersistentState();
		}
	};

	/**
	 * 从数据库加载玩家的任务状态列表。
	 * Loads the player's quest state list from the database.
	 *
	 * @param player 玩家 / player
	 * @return 任务状态列表 / quest state list
	 */
	@Override
	public QuestStateList load(final Player player) {
		QuestStateList questStateList = new QuestStateList();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

			stmt.setInt(1, player.getObjectId());

			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int questId = rset.getInt("quest_id");
					int questVars = rset.getInt("quest_vars");
					int completeCount = rset.getInt("complete_count");
					Timestamp nextRepeatTime = rset.getTimestamp("next_repeat_time");
					Integer reward = rset.getInt("reward");
					if (rset.wasNull()) reward = 0;
					Timestamp completeTime = rset.getTimestamp("complete_time");
					QuestStatus status = QuestStatus.valueOf(rset.getString("status"));

					QuestState questState = new QuestState(questId, status, questVars, completeCount, nextRepeatTime, reward, completeTime);
					questState.setPersistentState(PersistentState.UPDATED);
					questStateList.addQuest(questId, questState);
				}
			}

		} catch (SQLException e) {
			log.error(I18n.get("log.1ce22f7418a0", player.getObjectId(), e.getMessage(), e));
		}

		return questStateList;
	}

	/**
	 * 将玩家任务状态列表持久化到数据库（按 NEW/UPDATE/DELETED 批量处理）。
	 * Persists the player's quest state list (batch insert/update/delete by persistent state).
	 *
	 * 玩家 / player
	 */
	@Override
	public void store(Player player) {
		Collection<QuestState> qsList = player.getQuestStateList().getAllQuestState();
		if (GenericValidator.isBlankOrNull(qsList)) {
			return;
		}

		Connection con = null;
		try {
			con = DatabaseFactory.getConnection();
			con.setAutoCommit(false);
			store(con, player);
			con.commit();
			for (QuestState qs : qsList) {
				qs.setPersistentState(PersistentState.UPDATED);
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.0b87b3157dfe", player.getObjectId(), e));
			try {
				if (con != null) {
					con.rollback();
				}
			} catch (SQLException rollbackEx) {
				log.error(I18n.get("log.cc07b223d6ff", player.getObjectId(), rollbackEx));
			}
		} finally {
			DatabaseFactory.close(con);
		}

	}

	@Override
	public void store(Connection con, Player player) throws SQLException {
		if (con == null || player == null) {
			throw new IllegalArgumentException("connection and player are required");
		}
		if (con.getAutoCommit()) {
			throw new IllegalStateException("caller-owned quest transaction must disable auto-commit");
		}
		Collection<QuestState> qsList = player.getQuestStateList().getAllQuestState();
		if (GenericValidator.isBlankOrNull(qsList)) {
			return;
		}
		deleteQuest(con, player.getObjectId(), qsList);
		addQuests(con, player.getObjectId(), qsList);
		updateQuests(con, player.getObjectId(), qsList);
	}

	@Override
	public void store(Connection con, int playerId, Collection<QuestState> states) throws SQLException {
		if (con == null) {
			throw new IllegalArgumentException("connection is required");
		}
		if (con.getAutoCommit()) {
			throw new IllegalStateException("caller-owned quest transaction must disable auto-commit");
		}
		if (GenericValidator.isBlankOrNull(states)) {
			return;
		}
		deleteQuest(con, playerId, states);
		addQuests(con, playerId, states);
		updateQuests(con, playerId, states);
	}

	/**
	 * 批量插入新增任务状态。
	 * Batch-inserts newly created quest states.
	 *
	 * @param con 数据库连接 / database connection
	 * player id
	 * @param states 任务状态集合 / quest state collection
	 */
	private void addQuests(Connection con, int playerId, Collection<QuestState> states) throws SQLException {
		Collection<QuestState> statesToAdd = Collections2.filter(states, questsToAddPredicate);

		if (GenericValidator.isBlankOrNull(statesToAdd)) {
			return;
		}

		try (PreparedStatement ps = con.prepareStatement(INSERT_QUERY)) {
			int count = 0;

			for (QuestState qs : statesToAdd) {
				setInsertParameters(ps, playerId, qs);
				ps.addBatch();

				if (++count % BATCH_SIZE == 0) {
					ps.executeBatch();
				}
			}

			ps.executeBatch();
			log.debug("Inserted {} quests for player {}", statesToAdd.size(), playerId);
		}
	}

	/**
	 * 设置插入语句参数。
	 * Sets parameters for the insert prepared statement.
	 *
	 * @param ps 预处理语句 / prepared statement
	 * player id
	 * @param qs 任务状态 / quest state
	 * SQL exception
	 */
	private void setInsertParameters(PreparedStatement ps, int playerId, QuestState qs) throws SQLException {
		ps.setInt(1, playerId);
		ps.setInt(2, qs.getQuestId());
		ps.setString(3, qs.getStatus().toString());
		ps.setInt(4, qs.getQuestVars().getQuestVars());
		ps.setInt(5, qs.getCompleteCount());

		if (qs.getNextRepeatTime() != null) {
			ps.setTimestamp(6, qs.getNextRepeatTime());
		} else {
			ps.setNull(6, Types.TIMESTAMP);
		}

		setRewardParameter(ps, 7, qs);

		if (qs.getCompleteTime() == null) {
			ps.setNull(8, Types.TIMESTAMP);
		} else {
			ps.setTimestamp(8, qs.getCompleteTime());
		}
	}

	/**
	 * 批量更新已修改的任务状态。
	 * Batch-updates quest states that require update.
	 *
	 * @param con 数据库连接 / database connection
	 * player id
	 * @param states 任务状态集合 / quest state collection
	 */
	private void updateQuests(Connection con, int playerId, Collection<QuestState> states) throws SQLException {
		Collection<QuestState> statesToUpdate = Collections2.filter(states, questsToUpdatePredicate);

		if (GenericValidator.isBlankOrNull(statesToUpdate)) {
			return;
		}

		try (PreparedStatement ps = con.prepareStatement(UPDATE_QUERY)) {
			int count = 0;

			for (QuestState qs : statesToUpdate) {
				setUpdateParameters(ps, playerId, qs);
				ps.addBatch();

				if (++count % BATCH_SIZE == 0) {
					ps.executeBatch();
				}
			}

			ps.executeBatch();
			log.debug("Updated {} quests for player {}", statesToUpdate.size(), playerId);
		}
	}

	/**
	 * 设置更新语句参数。
	 * Sets parameters for the update prepared statement.
	 *
	 * @param ps 预处理语句 / prepared statement
	 * player id
	 * @param qs 任务状态 / quest state
	 * SQL exception
	 */
	private void setUpdateParameters(PreparedStatement ps, int playerId, QuestState qs) throws SQLException {
		ps.setString(1, qs.getStatus().toString());
		ps.setInt(2, qs.getQuestVars().getQuestVars());
		ps.setInt(3, qs.getCompleteCount());

		if (qs.getNextRepeatTime() != null) {
			ps.setTimestamp(4, qs.getNextRepeatTime());
		} else {
			ps.setNull(4, Types.TIMESTAMP);
		}

		setRewardParameter(ps, 5, qs);

		if (qs.getCompleteTime() == null) {
			ps.setNull(6, Types.TIMESTAMP);
		} else {
			ps.setTimestamp(6, qs.getCompleteTime());
		}

		ps.setInt(7, playerId);
		ps.setInt(8, qs.getQuestId());
	}

	static void setRewardParameter(PreparedStatement ps, int parameterIndex, QuestState qs) throws SQLException {
		Integer reward = qs.getRewardOrNull();
		if (reward == null) {
			ps.setNull(parameterIndex, Types.INTEGER);
		} else {
			ps.setInt(parameterIndex, reward);
		}
	}

	/**
	 * 批量删除标记为 DELETED 的任务状态。
	 * Batch-deletes quest states marked as DELETED.
	 *
	 * @param con 数据库连接 / database connection
	 * player id
	 * @param states 任务状态集合 / quest state collection
	 */
	private void deleteQuest(Connection con, int playerId, Collection<QuestState> states) throws SQLException {
		Collection<QuestState> statesToDelete = Collections2.filter(states, questsToDeletePredicate);

		if (GenericValidator.isBlankOrNull(statesToDelete)) {
			return;
		}

		try (PreparedStatement ps = con.prepareStatement(DELETE_QUERY)) {
			int count = 0;

			for (QuestState qs : statesToDelete) {
				ps.setInt(1, playerId);
				ps.setInt(2, qs.getQuestId());
				ps.addBatch();

				if (++count % BATCH_SIZE == 0) {
					ps.executeBatch();
				}
			}

			ps.executeBatch();
			log.debug("Deleted {} quests for player {}", statesToDelete.size(), playerId);
		}
	}

	/**
	 * 判断当前数据库是否受本 DAO 支持。
	 * Checks whether the given database is supported by this DAO.
	 *
	 * @param databaseName 数据库名称 / database name
	 * major version
	 * minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
