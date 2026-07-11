package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 高级自定义相关配置。
 * Advanced custom related configuration.
 */
public class AdvCustomConfig {
	/**
	 * 默认背包扩展格数。
	 * Default cube expansion size.
	 */
	@Property(key = "gameserver.cube.size", defaultValue = "0")
	public static int CUBE_SIZE;

	/**
	 * 是否启用商城限购。
	 * Whether game shop purchase limit is enabled.
	 */
	@Property(key = "gameserver.gameshop.limit", defaultValue = "false")
	public static boolean GAMESHOP_LIMIT;

	/**
	 * 商城限购分类。
	 * Game shop limit category.
	 */
	@Property(key = "gameserver.gameshop.category", defaultValue = "0")
	public static byte GAMESHOP_CATEGORY;

	/**
	 * 商城限购时间（分钟）。
	 * Game shop limit time in minutes.
	 */
	@Property(key = "gameserver.gameshop.limit.time", defaultValue = "60")
	public static long GAMESHOP_LIMIT_TIME;

	/**
	 * 制作延迟时间倍率。
	 * Crafting delay time rate.
	 */
	@Property(key = "gameserver.craft.delaytime,rate", defaultValue = "2")
	public static Integer CRAFT_DELAYTIME_RATE;
}
