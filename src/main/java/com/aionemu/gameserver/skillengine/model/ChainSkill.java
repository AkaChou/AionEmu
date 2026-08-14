package com.aionemu.gameserver.skillengine.model;

/**
 * 单条连锁技能状态：类别、连击计数与最近使用时间。
 * Single chain-skill state: category, chain count and last use time.
 */
public class ChainSkill {

	private String category;
	private int chainCount = 0;
	private long useTime;

	/**
	 * 构造连锁技能状态。
	 * Constructs a chain-skill state.
	 *
	 * @param category 连锁类别 / chain category
	 * @param chainCount 连击计数 / chain count
	 * @param useTime 使用时间戳 / use timestamp
	 */
	public ChainSkill(String category, int chainCount, long useTime) {
		this.category = category;
		this.chainCount = chainCount;
		this.useTime = useTime;
	}

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
	 * 获取连锁类别。
	 * Gets chain category.
	 *
	 * @return 类别 / category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * 设置连锁类别。
	 * Sets chain category.
	 *
	 * @param name 类别名 / category name
	 */
	public void setCategory(String name) {
		category = name;
	}

	/**
	 * 获取连击计数。
	 * Gets chain count.
	 *
	 * @return 连击计数 / chain count
	 */
	public int getChainCount() {
		return chainCount;
	}

	/**
	 * 设置连击计数。
	 * Sets chain count.
	 *
	 * @param chainCount 连击计数 / chain count
	 */
	public void setChainCount(int chainCount) {
		this.chainCount = chainCount;
	}

	/**
	 * 连击计数加一。
	 * Increments chain count by one.
	 */
	public void increaseChainCount() {
		chainCount++;
	}

	/**
	 * 获取最近使用时间。
	 * Gets last use timestamp.
	 *
	 * @return 毫秒时间戳 / epoch millis
	 */
	public long getUseTime() {
		return useTime;
	}

	/**
	 * 设置最近使用时间。
	 * Sets last use timestamp.
	 *
	 * @param useTime 毫秒时间戳 / epoch millis
	 */
	public void setUseTime(long useTime) {
		this.useTime = useTime;
	}
}
