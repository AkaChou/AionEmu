package com.aionemu.gameserver.model.items;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 物品冷却模型。
 * Item Cooldown model.
 *
 * @author ATracer
 */
@AllArgsConstructor
public class ItemCooldown {

	/**
	 * 下次可再次使用的时间。
	 * Time of next reuse.
	 */
	private final long time;
	/**
	 * 使用延迟（毫秒）。
	 * Use delay in ms.
	 */
	@Getter
	private final int useDelay;

	/**
	 * 获取下次可再次使用的时间。
	 * Returns the time of next reuse.
	 *
	 * @return 下次可再次使用的时间 / time of next reuse
	 */
	public long getReuseTime() {
		return time;
	}
}
