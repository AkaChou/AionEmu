package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** 装备变更的类型化事务边界。 / Typed transactional boundary for equipment mutations. */
public interface QuestEquipmentPort {
	void preflight(Connection connection, QuestSnapshot snapshot,
		List<QuestAction.UnequipItem> unequips) throws SQLException;

	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
		List<QuestAction.UnequipItem> unequips) throws SQLException;
}
