package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.sql.SQLException;

/** Read-only boundary that freezes all authoritative quest-start eligibility checks. */
public interface QuestStartEligibilityPort {
	QuestStartEligibility snapshot(int playerId, int questId, QuestEvent event) throws SQLException;
}
