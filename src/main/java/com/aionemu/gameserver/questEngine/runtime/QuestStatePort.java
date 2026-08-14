package com.aionemu.gameserver.questEngine.runtime;

import java.sql.Connection;
import java.sql.SQLException;

/** 规范 QuestStatus + quest_vars 投影的类型化边界。 / Typed boundary for the canonical QuestStatus + quest_vars projection. */
public interface QuestStatePort {
	/**
	 * 将规范投影写入调用方事务，而不推进实时内存 {@link QuestState}。
	 * 内存发布推迟到 {@link #publish}，使失败的提交永远不会让内存领先数据库。
	 * Writes the canonical projection to the caller-owned transaction without
	 * advancing the live in-memory {@link QuestState}. The memory publish is
	 * deferred to {@link #publish} so a failed commit never leaves memory
	 * ahead of the database.
	 */
	void apply(Connection connection, int playerId, QuestMutationPlan plan) throws SQLException;

	/**
	 * 将已提交投影发布到实时内存任务状态。仅在所属事务成功提交后调用。
	 * Publishes the committed projection to the live in-memory quest state.
	 * Only called after the owning transaction committed successfully.
	 */
	void publish(int playerId, QuestMutationPlan plan);

	/** 数据库提交后首次内存发布失败时重新应用已提交投影。 / Re-applies the committed projection when the first in-memory publish failed after the database commit. */
	default void resynchronize(int playerId, QuestMutationPlan plan) {
		publish(playerId, plan);
	}

	/** 所属事务回滚时清除 {@link #apply} 准备的任何投影。 / Clears any projection prepared by {@link #apply} when the owning transaction rolls back. */
	default void rollback(int playerId, QuestMutationPlan plan) {
	}
}
