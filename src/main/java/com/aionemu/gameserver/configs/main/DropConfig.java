package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 掉落相关配置。
 * Drop related configuration.
 */
public class DropConfig {
	/**
	 * 是否禁用掉落衰减。
	 * Whether drop reduction is disabled.
	 */
	@Property(key = "gameserver.drop.reduction.disable", defaultValue = "false")
	public static boolean DISABLE_DROP_REDUCTION;

	/**
	 * 是否启用独特掉落全服公告。
	 * Whether unique drop announce is enabled.
	 */
	@Property(key = "gameserver.unique.drop.announce.enable", defaultValue = "true")
	public static boolean ENABLE_UNIQUE_DROP_ANNOUNCE;

	/**
	 * 禁用掉落衰减的区域列表。
	 * Zones where drop reduction is disabled.
	 */
	@Property(key = "gameserver.drop.noreduction", defaultValue = "0")
	public static String DISABLE_DROP_REDUCTION_IN_ZONES;

	/**
	 * 是否启用全局掉落。
	 * Whether global drops are enabled.
	 */
	@Property(key = "gameserver.drop.enable.global.drops", defaultValue = "true")
	public static boolean ENABLE_GLOBAL_DROPS;

	/**
	 * 独立基纳掉落倍率。
	 * Dedicated Kinah drop multiplier.
	 */
	@Property(key = "gameserver.drop.kinah.rate", defaultValue = "1.0")
	public static float KINAH_RATE;

	/**
	 * 每个 NPC 允许的全局掉落数量上限。
	 * Maximum global drop items allowed per NPC.
	 */
	@Property(key = "gameserver.drop.max.global.drops.per.npc", defaultValue = "10")
	public static int MAX_GLOBAL_DROPS_PER_NPC;

	/** 校验掉落配置。 / Validates drop configuration. */
	public static void refresh() {
		if (!Float.isFinite(KINAH_RATE) || KINAH_RATE <= 0f) {
			throw new IllegalArgumentException("Kinah drop rate must be finite and greater than zero");
		}
	}
}
