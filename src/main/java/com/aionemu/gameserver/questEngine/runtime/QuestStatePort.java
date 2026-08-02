package com.aionemu.gameserver.questEngine.runtime;

import java.sql.Connection;
import java.sql.SQLException;

/** Typed boundary for the canonical QuestStatus + quest_vars projection. */
public interface QuestStatePort {
	/**
	 * Writes the canonical projection to the caller-owned transaction without
	 * advancing the live in-memory {@link QuestState}. The memory publish is
	 * deferred to {@link #publish} so a failed commit never leaves memory
	 * ahead of the database.
	 */
	void apply(Connection connection, int playerId, QuestMutationPlan plan) throws SQLException;

	/**
	 * Publishes the committed projection to the live in-memory quest state.
	 * Only called after the owning transaction committed successfully.
	 */
	void publish(int playerId, QuestMutationPlan plan);
}
