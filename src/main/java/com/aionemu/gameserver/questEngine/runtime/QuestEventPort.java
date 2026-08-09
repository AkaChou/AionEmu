package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/** Read-only domain boundary for authoritative player facts. */
public interface QuestEventPort {
	QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event) throws SQLException;

	/** Capture optional start-eligibility facts only for a transition that actually declares that condition. */
	default QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility) throws SQLException {
		return snapshot(connection, playerId, questId, event);
	}

	/** Captures only event-service facts referenced by the selected transition. */
	default QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility, Set<Integer> eventActivityQuestIds) throws SQLException {
		return snapshot(connection, playerId, questId, event, includeStartEligibility);
	}
}
