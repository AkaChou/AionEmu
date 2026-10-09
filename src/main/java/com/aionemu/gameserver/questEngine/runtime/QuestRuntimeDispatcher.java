package com.aionemu.gameserver.questEngine.runtime;

import java.util.List;
import java.util.OptionalInt;

import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogRegistry;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;

/**
 * 任务运行时统一入口，隔离 XML owner 与原版 owner 的目录和派发边界。
 * Unified quest-runtime entry point that isolates XML-owned and retail-owned catalogs and dispatch boundaries.
 * <p>
 * 实现必须保证单个 owner 只由一个子运行时拥有；跨 owner 事件由路由器组合结果，但不得让两个
 * 子运行时同时持有同一任务 ID。
 * Implementations must keep each owner owned by exactly one child runtime. Cross-owner events may
 * compose child results, but two child runtimes must never own the same quest id.</p>
 */
public interface QuestRuntimeDispatcher {

	QuestCatalogRegistry catalogRegistry();

	List<QuestCatalogDrop> questDrops(int npcId);

	boolean owns(int questId);

	boolean hasRoutes(QuestEvent event);

	boolean hasRoutes(QuestEvent event, int questId);

	boolean hasMatchingRoutes(QuestEvent event, int questId);

	List<Integer> owners();

	boolean dispatchRewardWindowAction(QuestEvent.TalkToNpc event, int playerId, int questId);

	OptionalInt itemPlayAnimationMillis(int itemId);

	QuestEventRouter.DispatchResult dispatch(QuestEvent event, int playerId, int questId,
		QuestDispatchContract contract);

	boolean dispatchOwners(QuestEvent event, int playerId, int[] questIds, QuestDispatchContract contract);

	void dispatchQuestStateChanged(int playerId, int changedQuestId);

	boolean dispatchSharedQuestAccept(int playerId, int questId, int dialogId);
}
