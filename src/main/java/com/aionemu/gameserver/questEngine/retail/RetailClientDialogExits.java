package com.aionemu.gameserver.questEngine.retail;

import java.util.Set;

/**
 * 客户端对话出口常量登记（原 {@code quest_client_dialog_exits.tsv} 已物理退役）。
 * <p>
 * 仅保留具有 select_none 续页（4763，接取/拒绝按钮所在页）的规范形任务。
 * 历史的微观页码阶梯（SELECT1_1、SELECT2_CONTINUE、SELECT5_CHECK、SELECT6 等）已随通用家族规范形生命周期彻底退役。
 */
public final class RetailClientDialogExits {

	public static final String SELECT_NONE_1 = "SELECT_NONE_1";

	private static final Set<Integer> SELECT_NONE_1_QUESTS = Set.of(
		1888, 2888, 15478, 15479, 15606, 16800, 25050, 25073, 25094, 25478,
		25479, 25606, 26800, 80989);
	private static final RetailClientDialogExits DEFAULT = new RetailClientDialogExits(
		SELECT_NONE_1_QUESTS);
	private static final RetailClientDialogExits EMPTY = new RetailClientDialogExits(Set.of());

	private final Set<Integer> selectNoneQuests;

	private RetailClientDialogExits(Set<Integer> selectNoneQuests) {
		this.selectNoneQuests = selectNoneQuests != null ? Set.copyOf(selectNoneQuests) : Set.of();
	}

	/**
	 * 缺省规范对话出口登记。 / Default canonical dialog exits registry.
	 */
	public static RetailClientDialogExits defaultExits() {
		return DEFAULT;
	}

	/**
	 * 空登记表（用于不涉及对话续页的场景）。 / An empty registry.
	 */
	public static RetailClientDialogExits empty() {
		return EMPTY;
	}

	/**
	 * 该任务是否需要某个对话出口。 / Whether the quest needs the given dialog exit.
	 */
	public boolean requires(int questId, String exit) {
		return SELECT_NONE_1.equals(exit) && selectNoneQuests.contains(questId);
	}

	public int size() {
		return selectNoneQuests.size();
	}
}
