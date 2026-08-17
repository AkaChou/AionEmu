package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 任务驱动的持久化玩家成长变更边界。
 * Transactional boundary for quest-driven durable player progression changes.
 */
public interface QuestProgressionPort {
	/**
	 * 在不修改状态的情况下验证玩家成长动作。
	 * Validates player-progression actions without mutating state.
	 *
	 * @param connection 调用方拥有的 JDBC 连接 / JDBC connection owned by the caller
	 * @param snapshot 玩家和任务快照 / player and quest snapshot
	 * @param promotions 待验证的晋升动作 / promotion actions to validate
	 * @throws SQLException 验证失败 / if validation fails
	 */
	void preflight(Connection connection, QuestSnapshot snapshot,
		List<QuestAction.PromoteArchDaeva> promotions) throws SQLException;

	/**
	 * 在调用方事务中持久化玩家成长动作，并返回提交后在线状态发布参与者。
	 * Persists player-progression actions in the caller transaction and returns the live-state publication participant.
	 *
	 * @param connection 调用方拥有的 JDBC 连接 / JDBC connection owned by the caller
	 * @param snapshot 玩家和任务快照 / player and quest snapshot
	 * @param promotions 待持久化的晋升动作 / promotion actions to persist
	 * @return 提交后发布在线状态的事务参与者 / participant that publishes live state after commit
	 * @throws SQLException 持久化失败 / if persistence fails
	 */
	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
		List<QuestAction.PromoteArchDaeva> promotions) throws SQLException;
}
