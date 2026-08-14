package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 必需物品/货币/奖励变更的类型化边界。
 * Typed boundary for required item/currency/reward mutations.
 */
public interface QuestActionPort {
	/**
	 * 预检动作列表，在不落库的情况下验证其可执行性。
	 * Preflights the action list, validating executability without persisting.
	 */
	void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) throws SQLException;

	/**
	 * 将动作列表作为事务参与者应用，失败时支持回滚。
	 * Applies the action list as a transaction participant, supporting rollback on failure.
	 */
	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions)
		throws SQLException;
}
