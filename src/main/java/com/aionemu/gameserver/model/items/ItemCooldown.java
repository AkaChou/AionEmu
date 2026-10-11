package com.aionemu.gameserver.model.items;


/**
 * 物品冷却模型。
 * Item Cooldown model.
 *
 * @param time     下次可再次使用的时间。
 *                 Time of next reuse.
 * @param useDelay 使用延迟（毫秒）。
 *                 Use delay in ms.
 * @author ATracer
 */
public record ItemCooldown(long time, int useDelay) {

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
