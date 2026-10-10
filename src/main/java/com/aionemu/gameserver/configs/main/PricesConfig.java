package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 物价、税率与商店买卖系数相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Prices, taxes and vendor buy/sell modifier related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 * @author Sarynth
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
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

	@Getter
	private final PricesProperties prices = new PricesProperties();

	@Getter
	public static class PricesProperties {
		private final DefaultProperties defaultProperties = new DefaultProperties();
		private final VendorProperties vendor = new VendorProperties();

		/**
		 * default 为 Java 关键字，手写访问器以对齐键段 default。
		 * default is a Java keyword, manual accessor ensures alignment with key segment "default".
		 */
		public DefaultProperties getDefault() {
			return defaultProperties;
		}

		public static class DefaultProperties {
			public int getPrices() {
				return DEFAULT_PRICES;
			}

			public void setPrices(int prices) {
				DEFAULT_PRICES = prices;
			}

			public int getModifier() {
				return DEFAULT_MODIFIER;
			}

			public void setModifier(int modifier) {
				DEFAULT_MODIFIER = modifier;
			}

			public int getTaxes() {
				return DEFAULT_TAXES;
			}

			public void setTaxes(int taxes) {
				DEFAULT_TAXES = taxes;
			}
		}

		public static class VendorProperties {
			public int getBuymod() {
				return VENDOR_BUY_MODIFIER;
			}

			public void setBuymod(int buymod) {
				VENDOR_BUY_MODIFIER = buymod;
			}

			public int getSellmod() {
				return VENDOR_SELL_MODIFIER;
			}

			public void setSellmod(int sellmod) {
				VENDOR_SELL_MODIFIER = sellmod;
			}
		}
	}
}
