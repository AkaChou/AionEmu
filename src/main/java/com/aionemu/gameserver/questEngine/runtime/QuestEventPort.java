package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.sql.Connection;
import java.sql.SQLException;

/** Read-only domain boundary for authoritative player facts. */
public interface QuestEventPort {
	QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event) throws SQLException;
}
