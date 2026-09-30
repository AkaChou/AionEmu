package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * quest_work_items 通道（P0c-10m）：真端表物品符号（ITEM_...）→ 生产 item_id。
 * <p>
 * 直接由官方真端元数据（QuestMetadata）提供，消灭对遗留 XML 的依赖。
 * The quest_data work-items channel maps retail item symbols to production item ids;
 * now driven directly by canonical QuestMetadata.
 */
public final class RetailQuestWorkItems {

	private static final Map<Integer, int[]> CACHE = new ConcurrentHashMap<>();

	private RetailQuestWorkItems() {
	}

	/** 单符号解析：命中返回 work_items 首项 id，越界返回 null。 / First work item, or null. */
	public static Integer first(String symbol, int questId) {
		int[] items = workItems(questId);
		if (items.length == 0) {
			return null;
		}
		// 单符号行：stem 须出现在该任务符号集（由调用方保证与真端表一致）；此处按清单首项回放。
		return items[0];
	}

	/** 任务的全部 work_items（懒加载缓存）。 / All work items for the quest (cached). */
	public static int[] workItems(int questId) {
		return CACHE.computeIfAbsent(questId, RetailQuestWorkItems::load);
	}

	private static int[] load(int questId) {
		return ProductionQuestDefinitions.catalog().findMetadata(questId)
			.map(m -> m.questWorkItems().stream().mapToInt(QuestItemRequirement::itemId).toArray())
			.orElseGet(() -> new int[0]);
	}
}
