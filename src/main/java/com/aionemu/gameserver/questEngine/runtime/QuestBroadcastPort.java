package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

/** 向其他任务广播区域任务结束事件的类型化边界。 / Typed boundary for broadcasting a zone-mission-end event to other quests. */
public interface QuestBroadcastPort {
	/**
	 * 向给定任务分发区域任务结束，使其能开始或推进。分发器是权威的；广播是提交后的 best-effort。
	 * Dispatches zone-mission-end to the given quests so they can start or advance.
	 * The dispatcher is authoritative; a broadcast is best-effort after commit.
	 *
	 * @return true 表示已分发；false 表示失败（best-effort，记录审计） / true if dispatched; false if it failed (best-effort, audited)
	 */
	boolean broadcastZoneMissionEnd(QuestSnapshot snapshot, QuestMutationPlan plan, int[] questIds);

	/** 为类型化事件任务拥有者调度内部刷新。 / Schedules an internal refresh for typed event-quest owners. */
	default boolean scheduleEventQuestRefresh(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds,
			int[] questIds) {
		return false;
	}
}
