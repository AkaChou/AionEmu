package com.aionemu.gameserver.utils.stats;

import java.util.NoSuchElementException;

/**
 * 按等级差调整经验奖励百分比
 * XP reward percentage modifiers by level difference
 *
 * @author ATracer
 */
public enum XPRewardEnum {
	/** 等级差 -11，经验 0% / Level diff -11, XP 0% */
	MINUS_11(-11, 0),
	/** 等级差 -10，经验 1% / Level diff -10, XP 1% */
	MINUS_10(-10, 1),
	/** 等级差 -9，经验 10% / Level diff -9, XP 10% */
	MINUS_9(-9, 10),
	/** 等级差 -8，经验 20% / Level diff -8, XP 20% */
	MINUS_8(-8, 20),
	/** 等级差 -7，经验 30% / Level diff -7, XP 30% */
	MINUS_7(-7, 30),
	/** 等级差 -6，经验 40% / Level diff -6, XP 40% */
	MINUS_6(-6, 40),
	/** 等级差 -5，经验 50% / Level diff -5, XP 50% */
	MINUS_5(-5, 50),
	/** 等级差 -4，经验 70% / Level diff -4, XP 70% */
	MINUS_4(-4, 70),
	/** 等级差 -3，经验 90% / Level diff -3, XP 90% */
	MINUS_3(-3, 90),
	/** 等级差 -2，经验 100% / Level diff -2, XP 100% */
	MINUS_2(-2, 100),
	/** 等级差 -1，经验 100% / Level diff -1, XP 100% */
	MINUS_1(-1, 100),
	/** 等级差 0，经验 100% / Level diff 0, XP 100% */
	ZERO(0, 100),
	/** 等级差 +1，经验 105% / Level diff +1, XP 105% */
	PLUS_1(1, 105),
	/** 等级差 +2，经验 110% / Level diff +2, XP 110% */
	PLUS_2(2, 110),
	/** 等级差 +3，经验 115% / Level diff +3, XP 115% */
	PLUS_3(3, 115),
	/** 等级差 +4，经验 120% / Level diff +4, XP 120% */
	PLUS_4(4, 120);

	/** 经验奖励百分比 / XP reward percent */
	private int xpRewardPercent;

	/** 等级差 / Level difference */
	private int levelDifference;

	/**
	 * 构造经验奖励条目
	 * Construct an XP reward entry
	 *
	 * Level difference
	 * @param xpRewardPercent 经验百分比 / XP percent
	 */
	private XPRewardEnum(int levelDifference, int xpRewardPercent) {
		this.levelDifference = levelDifference;
		this.xpRewardPercent = xpRewardPercent;
	}

	/**
	 * 获取经验奖励百分比
	 * Get XP reward percent
	 *
	 * @return 经验百分比 / XP percent
	 */
	public int rewardPercent() {
		return xpRewardPercent;
	}

	/**
	 * 按等级差返回经验奖励百分比（越界取边界值）
	 * Return XP reward percent by level difference (clamped to bounds)
	 *
	 * @param levelDifference 双方等级差 / Level difference between two objects
	 * @return 经验奖励百分比 / XP reward percentage
	 */
	public static int xpRewardFrom(int levelDifference) {
		if (levelDifference < MINUS_11.levelDifference) {
			return MINUS_11.xpRewardPercent;
		}
		if (levelDifference > PLUS_4.levelDifference) {
			return PLUS_4.xpRewardPercent;
		}

		for (XPRewardEnum xpReward : values()) {
			if (xpReward.levelDifference == levelDifference) {
				return xpReward.xpRewardPercent;
			}
		}

		throw new NoSuchElementException("XP reward for such level difference was not found");
	}
}
