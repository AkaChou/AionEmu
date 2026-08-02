package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Typed transactional boundary for durable non-currency quest rewards. */
public interface QuestRewardPort {
	void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.GrantReward> rewards) throws SQLException;

	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.GrantReward> rewards) throws SQLException;
}
