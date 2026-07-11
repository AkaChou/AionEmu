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
	 * time of next reuse
	 */
	private long time;
	/**
	 * Use delay in ms
	 */
	@Getter
	private int useDelay;

	/**
	 * @param time
	 * @param useDelay
	 */
	public ItemCooldown(long time, int useDelay) {
		this.time = time;
		this.useDelay = useDelay;
	}

	/**
	 * @return the time
	 */
	public long getReuseTime() {
		return time;
	}

}
