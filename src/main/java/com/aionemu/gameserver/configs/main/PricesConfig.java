package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 物价、税率与商店买卖系数相关配置。
 * Prices, taxes and vendor buy/sell modifier related configuration.
 *
 * @author Sarynth
 */
public class PricesConfig {

	/**
	 * 影响力界面显示的默认物价系数。
	 * Default prices value shown in the influence tab.
	 */
	@Property(key = "gameserver.prices.default.prices", defaultValue = "100")
	public static int DEFAULT_PRICES;

	/**
	 * 所有价格的隐藏修正系数。
	 * Hidden modifier applied to all prices.
	 */
	@Property(key = "gameserver.prices.default.modifier", defaultValue = "100")
	public static int DEFAULT_MODIFIER;

	/**
	 * 默认税率（值为 100 + 税率百分比）。
	 * Default taxes (value = 100 + tax percent).
	 */
	@Property(key = "gameserver.prices.default.taxes", defaultValue = "100")
	public static int DEFAULT_TAXES;

	/**
	 * NPC 商店购买价格修正系数。
	 * Vendor buy price modifier.
	 */
	@Property(key = "gameserver.prices.vendor.buymod", defaultValue = "100")
	public static int VENDOR_BUY_MODIFIER;

	/**
	 * NPC 商店出售价格修正系数。
	 * Vendor sell price modifier.
	 */
	@Property(key = "gameserver.prices.vendor.sellmod", defaultValue = "20")
	public static int VENDOR_SELL_MODIFIER;
}
