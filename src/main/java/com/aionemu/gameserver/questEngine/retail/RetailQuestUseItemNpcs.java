package com.aionemu.gameserver.questEngine.retail;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * 交互物 NPC 登记（内存视图）：NPC 模板里 {@code ai="quest_use_item"} 的 id 集。
 * <p>
 * 真端任务表不单独列出这组 NPC；本视图直接复用启动期已构建的
 * {@link RetailNpcNameIndex}（同一批 {@code npc_template_*.xml} 流），
 * 不再从 {@code quest_use_item_npcs.tsv} 读表，也不依赖独立生成器。
 * <p>
 * In-memory interaction-object NPC registry: ids whose retail NPC template declares
 * {@code ai="quest_use_item"}. The set comes from the same {@link RetailNpcNameIndex}
 * built over the loaded {@code npc_template_*.xml} streams; the former TSV registry
 * and its generator are no longer needed.
 */
public final class RetailQuestUseItemNpcs {

	private static final RetailQuestUseItemNpcs EMPTY = new RetailQuestUseItemNpcs(Set.of());

	private final Set<Integer> npcIds;

	private RetailQuestUseItemNpcs(Set<Integer> npcIds) {
		this.npcIds = npcIds;
	}

	/** 空登记（用于不涉及交互物的场景）。 / An empty registry. */
	public static RetailQuestUseItemNpcs empty() {
		return EMPTY;
	}

	/** 从 NPC 模板索引给出的 id 集构建。 / Builds from the id set exposed by the NPC template index. */
	public static RetailQuestUseItemNpcs fromIds(Collection<Integer> npcIds) {
		Objects.requireNonNull(npcIds, "npcIds");
		return npcIds.isEmpty() ? EMPTY : new RetailQuestUseItemNpcs(Set.copyOf(npcIds));
	}

	/** 该 NPC 是否需要任务交互对象路由。 / Whether the npc needs interaction-object routes. */
	public boolean isInteractionObject(int npcId) {
		return npcIds.contains(npcId);
	}

	public int size() {
		return npcIds.size();
	}
}
