package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 要塞攻城系统相关配置。
 * Fortress siege system related configuration.
 */
public class SiegeConfig {
	/**
	 * 是否启用攻城系统。
	 * Whether the siege system is enabled.
	 */
	@Property(key = "gameserver.siege.enable", defaultValue = "true")
	public static boolean SIEGE_ENABLED;

	/**
	 * 攻城勋章奖励倍率。
	 * Siege medal reward rate.
	 */
	@Property(key = "gameserver.siege.medal.rate", defaultValue = "1")
	public static int SIEGE_MEDAL_RATE;

	/**
	 * 是否启用攻城护盾。
	 * Whether siege shields are enabled.
	 */
	@Property(key = "gameserver.siege.shield.enable", defaultValue = "true")
	public static boolean SIEGE_SHIELD_ENABLED;

	/**
	 * 是否启用龙族自动突袭。
	 * Whether Balaur auto assault is enabled.
	 */
	@Property(key = "gameserver.siege.assault.enable", defaultValue = "false")
	public static boolean BALAUR_AUTO_ASSAULT;

	/**
	 * 龙族突袭强度倍率。
	 * Balaur assault rate multiplier.
	 */
	@Property(key = "gameserver.siege.assault.rate", defaultValue = "1")
	public static float BALAUR_ASSAULT_RATE;

	/**
	 * 是否启用自动分配攻城阵营。
	 * Whether auto siege race assignment is enabled.
	 */
	@Property(key = "gameserver.auto.siege.race", defaultValue = "false")
	public static boolean SIEGE_AUTO_RACE;

	/**
	 * 自动攻城地点 ID 列表。
	 * Auto siege location ID list.
	 */
	@Property(key = "gameserver.auto.siege.id", defaultValue = "")
	public static String SIEGE_AUTO_LOCID;
}
