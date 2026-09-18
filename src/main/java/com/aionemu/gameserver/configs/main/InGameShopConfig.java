package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 游戏内商城相关配置。
 * In-game shop related configuration.
 *
 * @author xTz
 */
public class InGameShopConfig {

	/**
	 * 是否启用游戏内商城。
	 * Whether the in-game shop is enabled.
	 */
	@Property(key = "gameserver.ingameshop.enable", defaultValue = "false")
	public static boolean ENABLE_IN_GAME_SHOP;

	/**
	 * 是否允许跨阵营赠送商城礼物。
	 * Whether gift system between factions is enabled.
	 */
	@Property(key = "gameserver.ingameshop.gift", defaultValue = "false")
	public static boolean ENABLE_GIFT_OTHER_RACE;

	/**
	 * 是否允许商城礼物赠送。
	 * Whether in-game shop gifts are allowed.
	 */
	@Property(key = "gameserver.ingameshop.allow.gift", defaultValue = "true")
	public static boolean ALLOW_GIFTS;

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
}
