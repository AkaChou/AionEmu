package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

/** Typed boundary for broadcasting a zone-mission-end event to other quests. */
public interface QuestBroadcastPort {
	/**
	 * Dispatches zone-mission-end to the given quests so they can start or advance.
	 * The dispatcher is authoritative; a broadcast is best-effort after commit.
	 *
	 * @return true 表示已分发; false 表示失败 (best-effort, 记录审计)
	 */
	boolean broadcastZoneMissionEnd(QuestSnapshot snapshot, QuestMutationPlan plan, int[] questIds);
}