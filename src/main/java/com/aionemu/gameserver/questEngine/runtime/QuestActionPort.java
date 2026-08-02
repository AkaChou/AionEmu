package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Typed boundary for required item/currency/reward mutations. */
public interface QuestActionPort {
	void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) throws SQLException;

	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions)
		throws SQLException;
}
