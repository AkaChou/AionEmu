package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Typed transactional boundary for inventory mutations (remove + give quest work items). */
public interface QuestInventoryPort {
	void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) throws SQLException;

	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) throws SQLException;
}
