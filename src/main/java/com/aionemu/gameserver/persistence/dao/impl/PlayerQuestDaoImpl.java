package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.persistence.dao.PlayerQuestDao;
import com.aionemu.gameserver.persistence.entity.PlayerQuestPo;
import com.aionemu.gameserver.persistence.mapper.PlayerQuestMapper;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家任务状态数据访问实现。
 * Data access implementation for player quest states.
 */
@Slf4j
@Service
@Lazy
public class PlayerQuestDaoImpl extends ServiceImpl<PlayerQuestMapper, PlayerQuestPo> implements PlayerQuestDao {

	/** 批处理大小 / Batch size */
	private static final int BATCH_SIZE = 100;

	/** 筛选需新增的任务状态 / Filter quests to insert */
	private static final Predicate<QuestState> questsToAddPredicate =
		input -> input != null && PersistentState.NEW == input.getPersistentState();

	/** 筛选需更新的任务状态 / Filter quests to update */
	private static final Predicate<QuestState> questsToUpdatePredicate =
		input -> input != null && PersistentState.UPDATE_REQUIRED == input.getPersistentState();

	/** 筛选需删除的任务状态 / Filter quests to delete */
	private static final Predicate<QuestState> questsToDeletePredicate =
		input -> input != null && PersistentState.DELETED == input.getPersistentState();

	private final TransactionTemplate transactionTemplate;

	public PlayerQuestDaoImpl(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public QuestStateList load(Player player) {
		QuestStateList questStateList = new QuestStateList();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerQuestPo::getPlayerId).eq(player.getObjectId()))
				.forEach(po -> {
					Integer reward = po.getReward() == null ? 0 : po.getReward();
					QuestStatus status = QuestStatus.valueOf(po.getStatus());
					QuestState questState = new QuestState(po.getQuestId(), status, po.getQuestVars(),
						po.getCompleteCount(), po.getNextRepeatTime(), reward, po.getCompleteTime());
					questState.setPersistentState(PersistentState.UPDATED);
					questStateList.addQuest(po.getQuestId(), questState);
				});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.1ce22f7418a0", player.getObjectId(), e.getMessage()), e);
		}
		return questStateList;
	}

	@Override
	public void store(Player player) {
		Collection<QuestState> qsList = player.getQuestStateList().getAllQuestState();
		if (GenericValidator.isBlankOrNull(qsList)) {
			return;
		}
		boolean hasDirty = false;
		for (QuestState qs : qsList) {
			if (qs.getPersistentState() != PersistentState.UPDATED) {
				hasDirty = true;
				break;
			}
		}
		if (!hasDirty) {
			return;
		}
		try {
			transactionTemplate.executeWithoutResult(_ -> {
				try (Connection con = DatabaseAccess.getConnection()) {
					store(con, player);
				} catch (SQLException e) {
					throw new DataAccessException("Failed to store quest states", e) {};
				}
			});
			for (QuestState qs : qsList) {
				qs.setPersistentState(PersistentState.UPDATED);
			}
		} catch (RuntimeException e) {
			log.error(I18n.get("log.0b87b3157dfe", player.getObjectId()), e);
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
	 */
	private void addQuests(Connection con, int playerId, Collection<QuestState> states) throws SQLException {
		Collection<QuestState> statesToAdd = Collections2.filter(states, questsToAddPredicate);
		if (GenericValidator.isBlankOrNull(statesToAdd)) {
			return;
		}
		try (PreparedStatement ps = con.prepareStatement(PlayerQuestMapper.INSERT_JDBC)) {
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
	 */
	private void updateQuests(Connection con, int playerId, Collection<QuestState> states) throws SQLException {
		Collection<QuestState> statesToUpdate = Collections2.filter(states, questsToUpdatePredicate);
		if (GenericValidator.isBlankOrNull(statesToUpdate)) {
			return;
		}
		try (PreparedStatement ps = con.prepareStatement(PlayerQuestMapper.UPDATE_JDBC)) {
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
	 */
	private void deleteQuest(Connection con, int playerId, Collection<QuestState> states) throws SQLException {
		Collection<QuestState> statesToDelete = Collections2.filter(states, questsToDeletePredicate);
		if (GenericValidator.isBlankOrNull(statesToDelete)) {
			return;
		}
		try (PreparedStatement ps = con.prepareStatement(PlayerQuestMapper.DELETE_JDBC)) {
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
}
