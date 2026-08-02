package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Typed transactional boundary for kinah/AP/GP and other durable currency rewards. */
public interface QuestCurrencyPort {
	void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.GrantReward> rewards) throws SQLException;

	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.GrantReward> rewards) throws SQLException;
}
