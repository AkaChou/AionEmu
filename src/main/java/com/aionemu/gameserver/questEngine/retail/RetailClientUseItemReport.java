package com.aionemu.gameserver.questEngine.retail;

import java.util.Map;

/**
 * 客户端报告模式常量登记（原 {@code quest_client_use_item_report.tsv} 已物理退役）。
 * <p>
 * SimpleUseItem 的交付方式：CHECK = 交付物 HasItem 门控（5 个活动任务，交付物 = 用物品本体）；
 * REWARD = 1009 直接交付进领奖。
 * <p>
 * Canonical report-mode rules for SimpleUseItem after the client registry TSV retirement.
 */
public final class RetailClientUseItemReport {

	/**
	 * 交付模式。 / Report mode.
	 */
	public enum Mode {

		/**
		 * select5 有检查按钮：39/20002 交付检查对。 / select5 carries the check button.
		 */
		CHECK,
		/**
		 * select5 无检查按钮：1009 直接交付。 / No check button; direct 1009 hand-in.
		 */
		REWARD
	}

	private static final Map<Integer, Mode> CHECK_MODES = Map.of(
		80482, Mode.CHECK,
		80486, Mode.CHECK,
		80554, Mode.CHECK,
		80558, Mode.CHECK,
		80612, Mode.CHECK);
	private static final Map<Integer, Integer> CHECK_ITEMS = Map.of(
		80482, 182215419,
		80486, 182215421,
		80554, 182215444,
		80558, 182215445,
		80612, 182215579);
	private static final RetailClientUseItemReport DEFAULT = new RetailClientUseItemReport(
		CHECK_MODES, CHECK_ITEMS);
	private static final RetailClientUseItemReport EMPTY =
		new RetailClientUseItemReport(Map.of(), Map.of());

	private final Map<Integer, Mode> modes;
	private final Map<Integer, Integer> items;

	private RetailClientUseItemReport(Map<Integer, Mode> modes, Map<Integer, Integer> items) {
		this.modes = modes != null ? Map.copyOf(modes) : Map.of();
		this.items = items != null ? Map.copyOf(items) : Map.of();
	}

	/**
	 * 缺省规范报告模式。 / Default canonical report-mode registry.
	 */
	public static RetailClientUseItemReport defaultReport() {
		return DEFAULT;
	}

	/**
	 * 空登记（全部按 REWARD 处理）。 / Empty registry: everything is treated as REWARD.
	 */
	public static RetailClientUseItemReport empty() {
		return EMPTY;
	}

	/**
	 * 该任务的交付模式（未登记按 REWARD）。 / The report mode of one quest (REWARD when unknown).
	 */
	public Mode mode(int questId) {
		return modes.getOrDefault(questId, Mode.REWARD);
	}

	/**
	 * CHECK 模式的交付物品 id（-1 = 未登记）。 / The CHECK-mode hand-in item id (-1 when unknown).
	 */
	public int checkItemId(int questId) {
		return items.getOrDefault(questId, -1);
	}
}
