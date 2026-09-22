package com.aionemu.gameserver.model.gameobjects.player;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.model.items.ItemCooldown;
import com.aionemu.gameserver.model.templates.item.ItemUseLimits;

/**
 * 玩家物品使用冷却注册表。
 * Item-use cooldown registry for a player.
 * <p>该类型只服务 {@link Player}：负责物品冷却表的惰性创建、查询、过期条目清除与移除；
 * 对外仍通过 {@link Player} 的原公开方法访问（门面签名不变），并保留冷却表未创建时返回
 * {@code null} 的可空语义与 live map 视图语义。该类型非线程安全，冷却表只应在玩家自身
 * 的任务/消息线程上访问。
 * This type only serves {@link Player}: it owns lazy map creation, lookup, expired-entry
 * eviction and removal. External callers keep using the original {@link Player} facade methods,
 * including the nullable-map and live-view semantics. Not thread-safe; the map is only accessed
 * on the owning player's task/packet threads.</p>
 */
final class PlayerCooldowns {

	private Map<Integer, ItemCooldown> itemCoolDowns;

	/**
	 * 判断指定使用限制是否仍在冷却中；已过期的冷却条目会在检查时顺带清除。
	 * Whether the given use limits are still cooling down; expired entries are evicted during the check.
	 * @param limits 物品使用限制 / item use limits
	 * @return 仍在冷却时为 true / true while still cooling down
	 */
	boolean isUseDisabled(ItemUseLimits limits) {
		if (limits == null) {
			return false;
		}
		if (itemCoolDowns == null || !itemCoolDowns.containsKey(limits.getDelayId())) {
			return false;
		}
		Long coolDown = itemCoolDowns.get(limits.getDelayId()).getReuseTime();

		if (coolDown < System.currentTimeMillis()) {
			itemCoolDowns.remove(limits.getDelayId());
			return false;
		}
		return true;
	}

	/**
	 * 获取指定冷却 ID 的剩余冷却结束时间。
	 * Returns the cooldown end time for the given delay id.
	 * @param delayId 冷却 ID / cooldown id
	 * @return 结束时间，未设置时为 0 / end time, or 0 when absent
	 */
	long getCoolDown(int delayId) {
		if (itemCoolDowns == null || !itemCoolDowns.containsKey(delayId)) {
			return 0;
		}
		return itemCoolDowns.get(delayId).getReuseTime();
	}

	/**
	 * 记录物品使用冷却；冷却表在首次写入时才创建。
	 * Records an item-use cooldown; the backing map is created on first write.
	 * @param delayId 冷却 ID / cooldown id
	 * @param time 冷却结束时间 / cooldown end time
	 * @param useDelay 使用延迟（毫秒）/ use delay in ms
	 */
	void addCoolDown(int delayId, long time, int useDelay) {
		if (itemCoolDowns == null) {
			itemCoolDowns = new LinkedHashMap<>();
		}
		itemCoolDowns.put(delayId, new ItemCooldown(time, useDelay));
	}

	/**
	 * 移除指定冷却 ID 的冷却条目。
	 * Removes the cooldown entry for the given delay id.
	 * @param delayId 冷却 ID / cooldown id
	 */
	void removeCoolDown(int delayId) {
		if (itemCoolDowns == null) {
			return;
		}
		itemCoolDowns.remove(delayId);
	}

	/**
	 * 返回原始冷却表。
	 * Returns the backing cooldown map.
	 * @return 冷却表（live 视图），未创建时为 null / live cooldown map, or null when not created
	 */
	Map<Integer, ItemCooldown> getItemCoolDowns() {
		return itemCoolDowns;
	}
}
