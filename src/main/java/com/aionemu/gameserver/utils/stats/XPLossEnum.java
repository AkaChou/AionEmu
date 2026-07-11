package com.aionemu.gameserver.utils.stats;

/**
 * 按等级定义死亡经验损失系数
 * XP loss coefficients by player level on death
 */
public enum XPLossEnum {
	/** 6 级，损失系数 1.0 / Level 6, loss param 1.0 */
	LEVEL_6(6, 1.0),
	/** 10 级，损失系数 1.0 / Level 10, loss param 1.0 */
	LEVEL_10(10, 1.0),
	/** 20 级，损失系数 1.0 / Level 20, loss param 1.0 */
	LEVEL_20(20, 1.0),
	/** 30 级，损失系数 1.0 / Level 30, loss param 1.0 */
	LEVEL_30(30, 1.0),
	/** 40 级，损失系数 1.0 / Level 40, loss param 1.0 */
	LEVEL_40(40, 1.0),
	/** 50 级，损失系数 1.0 / Level 50, loss param 1.0 */
	LEVEL_50(50, 1.0),
	/** 60 级，损失系数 1.0 / Level 60, loss param 1.0 */
	LEVEL_60(60, 1.0),
	// 经验损失枚举 5.0 / XP-LossEnum 5.0
	/** 70 级，损失系数 0.25 / Level 70, loss param 0.25 */
	LEVEL_70(70, 0.25),
	/** 75 级，损失系数 0.25 / Level 75, loss param 0.25 */
	LEVEL_75(75, 0.25),
	/** 80 级，损失系数 0.25 / Level 80, loss param 0.25 */
	LEVEL_80(80, 0.25),
	/** 83 级，损失系数 0.25 / Level 83, loss param 0.25 */
	LEVEL_83(83, 0.25);

	/** 等级阈值 / Level threshold */
	private int level;
	/** 经验损失系数 / XP loss parameter */
	private double param;

	/**
	 * 构造经验损失条目
	 * Construct an XP loss entry
	 *
	 * @param level 等级阈值 / Level threshold
	 * @param param 损失系数 / Loss parameter
	 */
	private XPLossEnum(int level, double param) {
		this.level = level;
		this.param = param;
	}

	/**
	 * 获取等级阈值
	 * Get level threshold
	 *
	 * Level
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * 获取经验损失系数
	 * Get XP loss parameter
	 *
	 * Loss parameter
	 */
	public double getParam() {
		return param;
	}

	/**
	 * 计算指定等级死亡时的经验损失量
	 * Calculate XP lost on death for the given level
	 *
	 * @param level 玩家等级 / Player level
	 * @param expNeed 升级所需经验 / XP needed for next level
	 * XP lost
	 */
	public static long getExpLoss(int level, long expNeed) {
		if (level < 11) { // 5.0
			return 0;
		}
		for (XPLossEnum xpLossEnum : values()) {
			if (level <= xpLossEnum.getLevel()) {
				return Math.round(expNeed / 100 * xpLossEnum.getParam());
			}
		}
		return 0;
	}
}
