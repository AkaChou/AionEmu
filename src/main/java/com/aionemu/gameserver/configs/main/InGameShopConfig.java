package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 游戏内商城相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * In-game shop related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 * @author xTz
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
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

	@Getter
	private final IngameshopProperties ingameshop = new IngameshopProperties();
	@Getter
	private final GameshopProperties gameshop = new GameshopProperties();

	@Getter
	public static class IngameshopProperties {
		private final AllowProperties allow = new AllowProperties();

		public boolean isEnable() {
			return ENABLE_IN_GAME_SHOP;
		}

		public void setEnable(boolean enable) {
			ENABLE_IN_GAME_SHOP = enable;
		}

		public boolean isGift() {
			return ENABLE_GIFT_OTHER_RACE;
		}

		public void setGift(boolean gift) {
			ENABLE_GIFT_OTHER_RACE = gift;
		}

		public static class AllowProperties {
			public boolean isGift() {
				return ALLOW_GIFTS;
			}

			public void setGift(boolean gift) {
				ALLOW_GIFTS = gift;
			}
		}
	}

	@Getter
	public static class GameshopProperties {
		private final LimitProperties limit = new LimitProperties();

		public byte getCategory() {
			return GAMESHOP_CATEGORY;
		}

		public void setCategory(byte category) {
			GAMESHOP_CATEGORY = category;
		}

		public static class LimitProperties {
			public LimitProperties() {}

			/**
			 * 兼容旧版裸标量 gameserver.gameshop.limit=false 的绑定入口。
			 * Binding entry for the legacy bare scalar gameserver.gameshop.limit=false.
			 */
			public static LimitProperties valueOf(String value) {
				try {
					GAMESHOP_LIMIT = Boolean.parseBoolean(value);
				} catch (RuntimeException ignored) {
				}
				return new LimitProperties();
			}

			public boolean isValue() {
				return GAMESHOP_LIMIT;
			}

			public void setValue(boolean value) {
				GAMESHOP_LIMIT = value;
			}

			public long getTime() {
				return GAMESHOP_LIMIT_TIME;
			}

			public void setTime(long time) {
				GAMESHOP_LIMIT_TIME = time;
			}
		}
	}
}
