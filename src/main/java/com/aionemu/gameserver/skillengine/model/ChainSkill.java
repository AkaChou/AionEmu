package com.aionemu.gameserver.skillengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

/**
 * 单条连锁技能状态：类别、连击计数与最近使用时间。
 * Single chain-skill state: category, chain count and last use time.
 */
@Getter
@Setter
@AllArgsConstructor
public class ChainSkill {

	/**
	 * 获取连锁类别。
	 * Gets chain category.
	 *
	 * @return 类别 / category
	 */
	private String category;
	/**
	 * 获取连击计数。
	 * Gets chain count.
	 *
	 * @return 连击计数 / chain count
	 */
	private int chainCount = 0;
	/**
	 * 获取最近使用时间。
	 * Gets last use timestamp.
	 *
	 * @return 毫秒时间戳 / epoch millis
	 */
	private long useTime;

	/**
	 * 重置为新类别并清零计数、刷新使用时间。
	 * Resets to a new category, clears count and refreshes use time.
	 *
	 * @param category 新连锁类别 / new chain category
	 */
	public void updateChainSkill(String category) {
		this.category = category;
		chainCount = 0;
		useTime = System.currentTimeMillis();
	}

	/**
	 * 连击计数加一。
	 * Increments chain count by one.
	 */
	public void increaseChainCount() {
		chainCount++;
	}
}
