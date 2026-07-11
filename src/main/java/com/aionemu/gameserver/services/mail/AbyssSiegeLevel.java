package com.aionemu.gameserver.services.mail;

/**
 * 欧比斯攻城等级枚举，标识邮件相关攻城层级。
 * Abyss siege level enum identifying mail-related siege tiers.
 */
public enum AbyssSiegeLevel {
	NONE(0), HERO_DECORATION(1), MEDAL(2), ELITE_SOLDIER(3), VETERAN_SOLDIER(4);

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