package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Typed transactional boundary for inventory mutations (remove + give quest work items). */
public interface QuestInventoryPort {
	void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) throws SQLException;

	/** Compatibility-aware preflight when unequips precede inventory removals. */
	default void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives,
			List<QuestAction.UnequipItem> unequips) throws SQLException {
		preflight(connection, snapshot, removals, gives);
	}

	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) throws SQLException;

	/** Compatibility overload; real implementations may use the unequip list for validation. */
	default QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives,
			List<QuestAction.UnequipItem> unequips) throws SQLException {
		return apply(connection, snapshot, removals, gives);
	}
}
