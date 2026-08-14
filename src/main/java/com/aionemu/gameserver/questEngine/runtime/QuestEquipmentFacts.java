package com.aionemu.gameserver.questEngine.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 在任务事件边界捕获的不可变已装备套装事实。 / Immutable equipped-set facts captured at the quest event boundary. */
public record QuestEquipmentFacts(Map<Integer, Integer> itemSetParts,
	Map<Integer, Integer> equippedItems) {
	/** 仅捕获套装事实的调用方的兼容构造器。 / Compatibility constructor for callers that only capture item-set facts. */
	public QuestEquipmentFacts(Map<Integer, Integer> itemSetParts) {
		this(itemSetParts, Map.of());
	}

	public QuestEquipmentFacts {
		Objects.requireNonNull(itemSetParts, "itemSetParts");
		if (itemSetParts.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getKey() <= 0
			|| entry.getValue() == null || entry.getValue() < 0)) {
			throw new IllegalArgumentException("itemSetParts must contain positive set ids and non-negative counts");
		}
		Objects.requireNonNull(equippedItems, "equippedItems");
		if (equippedItems.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getKey() <= 0
			|| entry.getValue() == null || entry.getValue() < 0)) {
			throw new IllegalArgumentException("equippedItems must contain positive item ids and non-negative counts");
		}
		itemSetParts = Map.copyOf(itemSetParts);
		equippedItems = Map.copyOf(equippedItems);
	}

	/** 任意列出的套装已装备部件的数量恰好等于指定值时返回 true。 / Returns true when any listed set has exactly the requested number of equipped parts. */
	public boolean anySetHasExactly(Set<Integer> setIds, int count) {
		Objects.requireNonNull(setIds, "setIds");
		if (setIds.isEmpty() || setIds.stream().anyMatch(id -> id == null || id <= 0)) {
			throw new IllegalArgumentException("setIds must contain positive ids");
		}
		if (count < 0) {
			throw new IllegalArgumentException("count must be non-negative");
		}
		return setIds.stream().anyMatch(id -> itemSetParts.getOrDefault(id, 0) == count);
	}

	/** 返回当前已装备的指定物品数量。 / Returns the number of copies of an item currently equipped. */
	public int equippedItemCount(int itemId) {
		if (itemId <= 0) {
			throw new IllegalArgumentException("itemId must be positive");
		}
		return equippedItems.getOrDefault(itemId, 0);
	}

	/** 至少装备一件指定物品时返回 true。 / Returns true when at least one copy of the requested item is equipped. */
	public boolean hasEquippedItem(int itemId) {
		return equippedItemCount(itemId) > 0;
	}
}
