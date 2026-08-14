package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/** 权威玩家事实的只读领域边界。 / Read-only domain boundary for authoritative player facts. */
public interface QuestEventPort {
	QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event) throws SQLException;

	/** 仅为实际声明该条件的转换捕获可选开始资格事实。 / Capture optional start-eligibility facts only for a transition that actually declares that condition. */
	default QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility) throws SQLException {
		return snapshot(connection, playerId, questId, event);
	}

	/** 仅捕获所选转换引用的事件服务事实。 / Captures only event-service facts referenced by the selected transition. */
	default QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility, Set<Integer> eventActivityQuestIds) throws SQLException {
		return snapshot(connection, playerId, questId, event, includeStartEligibility);
	}
}
