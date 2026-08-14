package com.aionemu.gameserver.services.mail;

/**
 * 欧比斯攻城等级枚举，标识邮件相关攻城层级。
 * Abyss siege level enum identifying mail-related siege tiers.
 */
public enum AbyssSiegeLevel {
	/** 无 / None. */
	NONE(0),
	/** 英雄装饰 / Hero decoration. */
	HERO_DECORATION(1),
	/** 勋章 / Medal. */
	MEDAL(2),
	/** 精英士兵 / Elite soldier. */
	ELITE_SOLDIER(3),
	/** 老兵士兵 / Veteran soldier. */
	VETERAN_SOLDIER(4);

	private int value;

	private AbyssSiegeLevel(int value) {
		this.value = value;
	}

	/**
	 * getId 方法。
	 * getId method.
	 * result
	 */
	public int getId() {
		return this.value;
	}
}