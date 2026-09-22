package com.aionemu.gameserver.model.gameobjects.player;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.model.templates.event.MaxCountOfDay;

/**
 * 玩家事件物品每日限购注册表。
 * Daily purchase-limit registry for event items of a player.
 * <p>该类型只服务 {@link Player}：负责物品当日已购/上限计数的惰性创建、查询、移除与清空；
 * 对外仍通过 {@link Player} 的原公开方法访问（门面签名不变），并保留内部表未创建时返回
 * {@code null}、首次写入才创建的惰性语义；{@link #getItemMaxCounts()} 返回 live 视图，
 * 调用方（如 EventItemsDAO）可直接在迭代中删除条目。
 * This type only serves {@link Player}: it owns lazy creation, lookup, removal and clearing of
 * per-day item counts. External callers keep using the original {@link Player} facade methods;
 * the backing map stays {@code null} until first write, and {@link #getItemMaxCounts()} exposes
 * the live view so callers (e.g. EventItemsDAO) may remove entries while iterating.</p>
 */
final class PlayerItemDailyLimits {

	private Map<Integer, MaxCountOfDay> maxCountEvent;

	/**
	 * 记录或更新物品当日次数上限。
	 * Records or updates the per-day count of an item.
	 * @param itemId 物品 ID / item id
	 * @param thisCount 当日次数 / per-day count
	 */
	void addItemMaxCount(int itemId, int thisCount) {
		if (maxCountEvent == null) {
			maxCountEvent = new LinkedHashMap<>();
		}
		if (maxCountEvent.get(itemId) != null) {
			maxCountEvent.get(itemId).setThisCount(thisCount);
		} else {
			maxCountEvent.put(itemId, new MaxCountOfDay(thisCount));
		}
	}

	/**
	 * 返回物品当日次数。
	 * Returns the per-day count of an item.
	 * @param itemId 物品 ID / item id
	 * @return 当日次数，未记录时为 0 / per-day count, or 0 when absent
	 */
	int getItemMaxCount(int itemId) {
		if (maxCountEvent == null || !maxCountEvent.containsKey(itemId)) {
			return 0;
		}
		return maxCountEvent.get(itemId).getThisCount();
	}

	/**
	 * 移除物品的当日次数记录。
	 * Removes the per-day record of an item.
	 * @param itemId 物品 ID / item id
	 */
	void removeItemMaxCount(int itemId) {
		if (maxCountEvent == null) {
			return;
		}
		maxCountEvent.remove(itemId);
	}

	/**
	 * 清空全部当日次数记录。
	 * Clears all per-day records.
	 */
	void clear() {
		if (maxCountEvent == null) {
			return;
		}
		maxCountEvent.clear();
	}

	/**
	 * 返回原始当日次数表。
	 * Returns the backing per-day count map.
	 * @return 当日次数表（live 视图），未创建时为 null / live map, or null when not created
	 */
	Map<Integer, MaxCountOfDay> getItemMaxCounts() {
		return maxCountEvent;
	}
}
