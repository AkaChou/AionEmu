package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** 背包变更（移除 + 发放任务工作物品）的类型化事务边界。 / Typed transactional boundary for inventory mutations (remove + give quest work items). */
public interface QuestInventoryPort {
	void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) throws SQLException;

	/** 卸装先于背包移除时的兼容感知预检。 / Compatibility-aware preflight when unequips precede inventory removals. */
	default void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives,
			List<QuestAction.UnequipItem> unequips) throws SQLException {
		preflight(connection, snapshot, removals, gives);
	}

	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) throws SQLException;

	/** 兼容重载；真实实现可使用卸装列表进行校验。 / Compatibility overload; real implementations may use the unequip list for validation. */
	default QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives,
			List<QuestAction.UnequipItem> unequips) throws SQLException {
		return apply(connection, snapshot, removals, gives);
	}
}
