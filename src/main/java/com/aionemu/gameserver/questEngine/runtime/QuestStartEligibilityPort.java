package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.sql.SQLException;

/** 冻结所有权威任务开始资格检查的只读边界。 / Read-only boundary that freezes all authoritative quest-start eligibility checks. */
public interface QuestStartEligibilityPort {
	QuestStartEligibility snapshot(int playerId, int questId, QuestEvent event) throws SQLException;
}
