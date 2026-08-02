package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy;

/** Typed boundary for the after-commit quest timer lifecycle. */
public interface QuestTimerPort {
	/**
	 * 启动玩家可见任务计时器;超时由权威客户端/引擎 {@code QuestTimerEnd} 回调。
	 *
	 * @return true 表示已启动; false 表示玩家离线/失败 (best-effort)
	 */
	boolean startQuestTimer(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds, QuestTimerPolicy policy);

	default boolean startQuestTimer(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds) {
		return startQuestTimer(snapshot, plan, seconds, QuestTimerPolicy.visible());
	}

	/** 启动不可见任务计时器;超时由引擎 {@code InvisibleTimerEnd} 回调。 */
	boolean startInvisibleTimer(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds, QuestTimerPolicy policy);

	default boolean startInvisibleTimer(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds) {
		return startInvisibleTimer(snapshot, plan, seconds, QuestTimerPolicy.invisible());
	}

	/** 取消任务计时器 (UI 清除 + 取消调度)。 */
	boolean cancelQuestTimer(QuestSnapshot snapshot, QuestMutationPlan plan, QuestTimerPolicy.Identity identity);

	default boolean cancelQuestTimer(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return cancelQuestTimer(snapshot, plan, QuestTimerPolicy.visible().identity());
	}
}
