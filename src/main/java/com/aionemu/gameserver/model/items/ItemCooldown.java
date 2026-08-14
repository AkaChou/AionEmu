package com.aionemu.gameserver.model.items;

import lombok.Getter;

/**
 * 物品冷却模型。
 * Item Cooldown model.
 *
 * @author ATracer
 */
public class ItemCooldown {

	/**
	 * 下次可再次使用的时间。
	 * Time of next reuse.
	 */
	private long time;
	/**
	 * 使用延迟（毫秒）。
	 * Use delay in ms.
	 */
	@Getter
	private int useDelay;

	/**
	 * 构造物品冷却信息。
	 * Constructs item cooldown info.
	 *
	 * @param time 下次可再次使用的时间 / time of next reuse
	 * @param useDelay 使用延迟（毫秒） / use delay in ms
	 */
	public ItemCooldown(long time, int useDelay) {
		this.time = time;
		this.useDelay = useDelay;
	}

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
