package com.aionemu.gameserver.utils.stats;

import java.util.NoSuchElementException;

/**
 * 按等级差调整掉落奖励百分比
 * Drop reward percentage modifiers by level difference
 */
public enum DropRewardEnum {
	/** 等级差 -10，掉落 0% / Level diff -10, drop 0% */
	MINUS_10(-10, 0),
	/** 等级差 -9，掉落 39% / Level diff -9, drop 39% */
	MINUS_9(-9, 39),
	/** 等级差 -8，掉落 79% / Level diff -8, drop 79% */
	MINUS_8(-8, 79),
	/** 等级差 -7，掉落 100% / Level diff -7, drop 100% */
	MINUS_7(-7, 100);

	/** 掉落奖励百分比 / Drop reward percent */
	private int dropRewardPercent;
	/** 等级差 / Level difference */
	private int levelDifference;

	/**
	 * 构造掉落奖励条目
	 * Construct a drop reward entry
	 *
	 * Level difference
	 * @param dropRewardPercent 掉落百分比 / Drop percent
	 */
	private DropRewardEnum(int levelDifference, int dropRewardPercent) {
		this.levelDifference = levelDifference;
		this.dropRewardPercent = dropRewardPercent;
	}

	/**
	 * 获取掉落奖励百分比
	 * Get drop reward percent
	 *
	 * @return 掉落百分比 / Drop percent
	 */
	public int rewardPercent() {
		return dropRewardPercent;
	}

	/**
	 * 按等级差返回掉落奖励百分比（越界取边界值）
	 * Return drop reward percent by level difference (clamped to bounds)
	 *
	 * @param levelDifference 双方等级差 / Level difference between entities
	 * @return 掉落奖励百分比 / Drop reward percentage
	 */
	public static int dropRewardFrom(int levelDifference) {
		if (levelDifference < MINUS_10.levelDifference) {
			return MINUS_10.dropRewardPercent;
		}
		if (levelDifference > MINUS_7.levelDifference) {
			return MINUS_7.dropRewardPercent;
		}
		for (DropRewardEnum dropReward : values()) {
			if (dropReward.levelDifference == levelDifference) {
				return dropReward.dropRewardPercent;
			}
		}
		throw new NoSuchElementException("Drop reward for such level difference was not found");
	}
}
