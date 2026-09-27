package com.aionemu.gameserver.questEngine.retail;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * 真端表驱动的任务来源目录（"真端优先、XML 降级"的判定点）。
 * <p>
 * 运行时任务引擎仍然消费编译后的定义 IR；变化的是**定义从哪里来**：
 * <ol>
 * <li>{@link Source#RETAIL_TABLE}：任务在真端模板表里有行 → 由表驱动（本目录给出执行计划，后续由表→IR 编译器接管）；</li>
 * <li>{@link Source#XML_FALLBACK}：真端表无法表达（无模板行 / per-quest 脚本 / 未覆盖）→ 保留现有 quest-definition XML；</li>
 * <li>{@link Source#UNKNOWN}：两边都没有（未移植任务）。</li>
 * </ol>
 * 这样"删 XML"是有判据的迁移：覆盖一类、删一类。
 * <p>
 * Directory that decides whether a quest is retail-table driven or must fall back to XML.
 */
public final class RetailQuestCatalog {

	/** 任务定义来源。 / Where a quest definition comes from. */
	public enum Source {
		/** 真端模板表可驱动。 / Covered by a retail template table. */
		RETAIL_TABLE,
		/** 真端表无法表达，降级到现有 quest-definition XML。 / Falls back to the current XML definition. */
		XML_FALLBACK,
		/** 两边都没有。 / Known to neither source. */
		UNKNOWN
	}

	private final RetailSimpleHuntTable simpleHunt;
	private final RetailCombineTaskTable combineTask;
	private final RetailNpcNameIndex npcNames;

	public RetailQuestCatalog(RetailSimpleHuntTable simpleHunt, RetailNpcNameIndex npcNames) {
		this(simpleHunt, null, npcNames);
	}

	/**
	 * 带 CombineTask 表的构造（判源与诊断用；{@code combineTask} 可为 null 表示未装载）。
	 * CombineTask-aware constructor; a null table means the family is not loaded.
	 */
	public RetailQuestCatalog(RetailSimpleHuntTable simpleHunt, RetailCombineTaskTable combineTask,
			RetailNpcNameIndex npcNames) {
		this.simpleHunt = simpleHunt;
		this.combineTask = combineTask;
		this.npcNames = npcNames;
	}

	/** 是否有真端 CombineTask 行。 / Whether a retail CombineTask row exists. */
	public boolean hasCombineTask(int questId) {
		return combineTask != null && combineTask.find(questId).isPresent();
	}

	/** 是否有真端 SimpleHunt 行。 / Whether a retail SimpleHunt row exists. */
	public boolean hasSimpleHunt(int questId) {
		return simpleHunt.find(questId).isPresent();
	}

	/** 真端 SimpleHunt 执行计划（表 + 名索引绑定）。 / Bound retail plan for a SimpleHunt quest. */
	public Optional<RetailSimpleHuntPlan> simpleHuntPlan(int questId) {
		return simpleHunt.find(questId).map(entry -> RetailSimpleHuntPlan.bind(entry, npcNames));
	}

	/**
	 * 判定任务的定义来源。<p>
	 * xmlPorted 由调用方从现有 XML 目录查出（真端表未覆盖时才有意义）。
	 * Classifies the definition source; {@code xmlPorted} comes from the current XML catalog.
	 */
	public Source sourceOf(int questId, boolean xmlPorted) {
		if (hasSimpleHunt(questId) || hasCombineTask(questId)) {
			return Source.RETAIL_TABLE;
		}
		return xmlPorted ? Source.XML_FALLBACK : Source.UNKNOWN;
	}

	/** 该任务是否应当走表驱动（降级链第一判定）。 / Whether the quest should be table driven. */
	public boolean isTableDriven(int questId) {
		return hasSimpleHunt(questId) || hasCombineTask(questId);
	}

	/**
	 * 表驱动下的击杀推进：返回计数后的 packed 进度；不可计数时返回原值。
	 * Table-driven kill advance: returns the packed progress after counting, unchanged when not countable.
	 */
	public OptionalInt onKill(int questId, int npcId, int packedProgress) {
		Optional<RetailSimpleHuntPlan> plan = simpleHuntPlan(questId);
		if (plan.isEmpty() || !plan.get().canCount(npcId, packedProgress)) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(plan.get().count(npcId, packedProgress));
	}

	public int retailSimpleHuntRows() {
		return simpleHunt.size();
	}

	/** 真端 CombineTask 行数（诊断用；未装载时为 0）。 / Retail CombineTask row count, 0 when unloaded. */
	public int retailCombineTaskRows() {
		return combineTask == null ? 0 : combineTask.size();
	}
}
