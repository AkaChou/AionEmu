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
}
