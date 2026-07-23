package com.aionemu.gameserver.scriptEngine;

import com.aionemu.gameserver.questEngine.handlers.QuestHandler;

/**
 * 数据驱动任务脚本契约，对应真端 ScriptDLL64 的 {@code Simple*Quest} 族
 * （SimpleHuntQuest / SimpleCollectQuest / SimpleTalkQuest / SimpleSerialHuntQuest /
 * SimpleItemPlayQuest / SimpleUseItemQuest）。
 * Data-driven quest-script contract, mirroring the retail {@code Simple*Quest} family.
 * XML 数据实例化后由统一注册表持有，并复用完整的任务事件处理契约。
 */
@FunctionalInterface
public interface ScriptQuest {

	/**
	 * 返回承载当前任务完整事件语义的处理器。
	 * Return the handler carrying the quest's complete event semantics.
	 *
	 * <p>迁移期直接复用现有 XML 任务处理器，避免维护第二套任务执行器。
	 * During migration this reuses the existing XML quest handler instead of
	 * maintaining a second quest runtime.
	 *
	 * @return XML 任务处理器 / XML quest handler
	 */
	QuestHandler getHandler();

	/**
	 * 返回该脚本绑定的任务 ID。
	 * Return the quest id this script is bound to.
	 *
	 * @return 任务 ID / quest id
	 */
	default int getQuestId() {
		return getHandler().getQuestId();
	}
}
