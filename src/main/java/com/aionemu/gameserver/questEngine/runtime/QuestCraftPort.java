package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** 配方与制作技能任务变更的事务边界。 / Transactional boundary for recipe and crafting-skill quest mutations. */
public interface QuestCraftPort {
	void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) throws SQLException;

	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions)
		throws SQLException;
}
