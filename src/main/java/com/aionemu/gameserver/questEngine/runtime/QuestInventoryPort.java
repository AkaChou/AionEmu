package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Typed transactional boundary for inventory mutations. */
public interface QuestInventoryPort {
	void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals) throws SQLException;

	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals) throws SQLException;
}
