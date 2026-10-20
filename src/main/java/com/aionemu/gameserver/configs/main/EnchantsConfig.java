package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 强化与镶嵌相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Enchant and socket related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Getter
@Component
@ConfigurationProperties(prefix = "gameserver")
public class EnchantsConfig {

	/**
	 * 魔石镶嵌基础成功率（%），与其他加成累加后判定。
	 * Manastone socketing base success rate (%), bonuses are added on top.
	 */
	@Property(key = "gameserver.socket.manastone.success.rate", defaultValue = "100")
	public static float SOCKET_MANASTONE;
	/**
	 * 物品强化基础成功率（%）。
	 * Item enchant base success rate (%).
	 */
	@Property(key = "gameserver.enchant.item.success.rate", defaultValue = "100")
	public static float ENCHANT_ITEM;
	/**
	 * 装备强化消耗基纳（-1 为默认）。
	 * Equipment enchant kinah cost (-1 for default).
	 */
	@Property(key = "gameserver.enchant.item.kinah", defaultValue = "-1")
	public static int ENCHANT_ITEM_KINAH;
	/**
	 * 装备最大强化等级。
	 * Maximum equipment enchant level.
	 */
	@Property(key = "gameserver.enchant.equipment.max.level", defaultValue = "50")
	public static int MAX_EQUIPMENT_ENCHANT_LEVEL;
	/**
	 * 天烙之石（埃斯蒂玛）强化基础成功率（%），包含普通成功判定与客户端结果展示。
	 * Estima (CP stone) base success rate (%), used for both the roll and the client result.
	 */
	@Property(key = "gameserver.enchant.estima.success.rate", defaultValue = "100")
	public static float ENCHANT_ESTIMA;
	/**
	 * 天烙之石强化施法速度（毫秒）。
	 * Estima enchant cast speed in milliseconds.
	 */
	@Property(key = "gameserver.enchant.estima.cast.time", defaultValue = "500")
	public static int ESTIMA_ENCHANT_SPEED;
	/**
	 * 首饰（耳环/戒指/项链/头盔/腰带）最大觉醒等级。
	 * Maximum accessory (earrings/rings/necklace/helmet/belt) authorize level.
	 */
	@Property(key = "gameserver.enchant.authorize.accessory.max.level", defaultValue = "30")
	public static int MAX_AUTHORIZE_ACCESSORY_LEVEL;
	/**
	 * 羽饰最大觉醒等级。
	 * Maximum plume authorize level.
	 */
	@Property(key = "gameserver.enchant.authorize.plume.max.level", defaultValue = "30")
	public static int MAX_AUTHORIZE_PLUME_LEVEL;
	/**
	 * 手镯最大觉醒等级。
	 * Maximum bracelet authorize level.
	 */
	@Property(key = "gameserver.enchant.authorize.bracelet.max.level", defaultValue = "30")
	public static int MAX_AUTHORIZE_BRACELET_LEVEL;
	/**
	 * 羽饰淬炼成功率（%）。
	 * Plume tempering success rate (%).
	 */
	@Property(key = "gameserver.enchant.plume.success.rate", defaultValue = "100")
	public static float ENCHANT_PLUME;
	/**
	 * 手镯淬炼成功率（%）。
	 * Bracelet tempering success rate (%).
	 */
	@Property(key = "gameserver.enchant.bracelet.success.rate", defaultValue = "100")
	public static float ENCHANT_BRACELET;
	/**
	 * 饰品（耳环/戒指/项链/头盔/腰带）淬炼成功率（%）。
	 * Accessory (earrings/rings/necklace/helmet/belt) tempering success rate (%).
	 */
	@Property(key = "gameserver.enchant.accessory.success.rate", defaultValue = "100")
	public static float ENCHANT_ACCESSORY;
	/**
	 * 烙印之石强化成功率（%）。
	 * Stigma enchant success rate (%).
	 */
	@Property(key = "gameserver.enchant.stigma.success.rate", defaultValue = "100")
	public static float ENCHANT_STIGMA;
	/**
	 * 烙印之石最大强化等级，内置默认 10，硬上限 100；生产配置显式设为 15。
	 * Maximum stigma enchant level, built-in default 10, hard-capped at 100; production config sets 15.
	 */
	@Property(key = "gameserver.enchant.stigma.max.level", defaultValue = "10")
	public static int MAX_STIGMA_ENCHANT_LEVEL;
	/**
	 * 烙印之石强化施法速度（毫秒）。
	 * Stigma enchant cast time in milliseconds.
	 */
	@Property(key = "gameserver.enchant.stigma.cast.time", defaultValue = "3000")
	public static int STIGMA_ENCHANT_SPEED;
	/**
	 * 是否在失败时清理魔石。
	 * Whether manastones are cleaned on failure.
	 */
	@Property(key = "gameserver.manastone.clean", defaultValue = "false")
	public static boolean CLEAN_STONE;
	/**
	 * 强化施法速度（毫秒）。
	 * Enchant cast speed in milliseconds.
	 */
	@Property(key = "gameserver.enchant.cast.time", defaultValue = "500")
	public static int ENCHANT_SPEED;
	/**
	 * 觉醒（淬炼）施法速度（毫秒），觉醒加速生效时取一半。
	 * Authorize (tempering) cast speed in milliseconds, halved while authorize boost is active.
	 */
	@Property(key = "gameserver.enchant.authorize.cast.time", defaultValue = "500")
	public static int AUTHORIZE_SPEED;
	/**
	 * 是否启用大天使物品强化失败损毁。
	 * Whether Arch Daeva item break on enchant failure is enabled.
	 */
	@Property(key = "gameserver.enchant.item.broke", defaultValue = "true")
	public static boolean ENABLE_ARCHDAEVA_ITEM_BROKE;

	private final SocketProperties socket = new SocketProperties();
	private final ManastoneProperties manastone = new ManastoneProperties();
	private final EnchantProperties enchant = new EnchantProperties();

	@Getter
	public static class SocketProperties {
		private final SocketManastoneProperties manastone = new SocketManastoneProperties();

		@Getter
		public static class SocketManastoneProperties {
			private final SuccessProperties success = new SuccessProperties();

			public static class SuccessProperties {
				public float getRate() {
					return SOCKET_MANASTONE;
				}

				public void setRate(float rate) {
					SOCKET_MANASTONE = rate;
				}
			}
		}
	}

	public static class ManastoneProperties {
		public boolean isClean() {
			return CLEAN_STONE;
		}

		public void setClean(boolean clean) {
			CLEAN_STONE = clean;
		}
	}

	@Getter
	public static class EnchantProperties {
		private final ItemProperties item = new ItemProperties();
		private final EquipmentProperties equipment = new EquipmentProperties();
		private final EstimaProperties estima = new EstimaProperties();
		private final AuthorizeProperties authorize = new AuthorizeProperties();
		private final CastProperties cast = new CastProperties();
		private final PlumeProperties plume = new PlumeProperties();
		private final BraceletProperties bracelet = new BraceletProperties();
		private final AccessoryProperties accessory = new AccessoryProperties();
		private final StigmaProperties stigma = new StigmaProperties();

		/**
		 * 裸标量键存在时由 Binder 忽略；保留以兼容 {@code gameserver.enchant.item=100}。
		 * Ignored by the Binder when the bare scalar key is present; kept for
		 * {@code gameserver.enchant.item=100} compatibility.
		 */
		public void setItem(ItemProperties item) {
		}

		@Getter
		public static class PlumeProperties {
			private final RateProperties success = new RateProperties();

			public static class RateProperties {
				public float getRate() {
					return ENCHANT_PLUME;
				}

				public void setRate(float rate) {
					ENCHANT_PLUME = rate;
				}
			}
		}

		@Getter
		public static class BraceletProperties {
			private final RateProperties success = new RateProperties();

			public static class RateProperties {
				public float getRate() {
					return ENCHANT_BRACELET;
				}

				public void setRate(float rate) {
					ENCHANT_BRACELET = rate;
				}
			}
		}

		@Getter
		public static class AccessoryProperties {
			private final RateProperties success = new RateProperties();

			public static class RateProperties {
				public float getRate() {
					return ENCHANT_ACCESSORY;
				}

				public void setRate(float rate) {
					ENCHANT_ACCESSORY = rate;
				}
			}
		}

		@Getter
		public static class StigmaProperties {
			private final RateProperties success = new RateProperties();
			private final MaxProperties max = new MaxProperties();
			private final CastProperties cast = new CastProperties();

			public static class RateProperties {
				public float getRate() {
					return ENCHANT_STIGMA;
				}

				public void setRate(float rate) {
					ENCHANT_STIGMA = rate;
				}
			}

			public static class MaxProperties {
				public int getLevel() {
					return MAX_STIGMA_ENCHANT_LEVEL;
				}

				public void setLevel(int level) {
					MAX_STIGMA_ENCHANT_LEVEL = level;
				}
			}

			public static class CastProperties {
				public int getTime() {
					return STIGMA_ENCHANT_SPEED;
				}

				public void setTime(int time) {
					STIGMA_ENCHANT_SPEED = time;
				}
			}
		}

		/**
		 * 天烙之石强化配置：基础成功率、等级上限与施法速度，绑定至静态配置门面。
		 * Binds Estima base success rate, level cap and cast speed to the static configuration facade.
		 */
		@Getter
		public static class EstimaProperties {
			private final RateProperties success = new RateProperties();
			private final CastProperties cast = new CastProperties();

			/**
			 * 兼容旧的裸标量形式 {@code gameserver.enchant.estima=100}，新配置使用 success.rate。
			 * Supports the legacy bare scalar form {@code gameserver.enchant.estima=100}; new config uses success.rate.
			 */
			public static EstimaProperties valueOf(String value) {
				EstimaProperties props = new EstimaProperties();
				try {
					ENCHANT_ESTIMA = Float.parseFloat(value);
				} catch (NumberFormatException ignored) {
				}
				return props;
			}

			public static class RateProperties {
				public float getRate() {
					return ENCHANT_ESTIMA;
				}

				public void setRate(float rate) {
					ENCHANT_ESTIMA = rate;
				}
			}

			public static class CastProperties {
				public int getTime() {
					return ESTIMA_ENCHANT_SPEED;
				}

				public void setTime(int time) {
					ESTIMA_ENCHANT_SPEED = time;
				}
			}

		}

		@Getter
		public static class ItemProperties {
			private final RateProperties success = new RateProperties();

			/**
			 * 兼容旧的裸标量形式 {@code gameserver.enchant.item=100}。
			 * Supports the legacy bare scalar form {@code gameserver.enchant.item=100}.
			 */
			public static ItemProperties valueOf(String value) {
				ItemProperties props = new ItemProperties();
				try {
					ENCHANT_ITEM = Float.parseFloat(value);
				} catch (NumberFormatException ignored) {
				}
				return props;
			}

			public int getKinah() {
				return ENCHANT_ITEM_KINAH;
			}

			public void setKinah(int kinah) {
				ENCHANT_ITEM_KINAH = kinah;
			}

			public boolean isBroke() {
				return ENABLE_ARCHDAEVA_ITEM_BROKE;
			}

			public void setBroke(boolean broke) {
				ENABLE_ARCHDAEVA_ITEM_BROKE = broke;
			}

			public static class RateProperties {
				public float getRate() {
					return ENCHANT_ITEM;
				}

				public void setRate(float rate) {
					ENCHANT_ITEM = rate;
				}
			}
		}

		@Getter
		public static class EquipmentProperties {
			private final MaxProperties max = new MaxProperties();

			public static class MaxProperties {
				public int getLevel() {
					return MAX_EQUIPMENT_ENCHANT_LEVEL;
				}

				public void setLevel(int level) {
					MAX_EQUIPMENT_ENCHANT_LEVEL = level;
				}
			}
		}

		/**
		 * 觉醒等级上限（首饰 / 羽饰 / 手镯）。
		 * Authorize level caps (accessory / plume / bracelet).
		 */
		@Getter
		public static class AuthorizeProperties {
			private final AccessoryProperties accessory = new AccessoryProperties();
			private final PlumeProperties plume = new PlumeProperties();
			private final BraceletProperties bracelet = new BraceletProperties();
			private final CastProperties cast = new CastProperties();

			public CastProperties getCast() {
				return cast;
			}

			@Getter
			public static class AccessoryProperties {
				private final MaxProperties max = new MaxProperties();

				public static class MaxProperties {
					public int getLevel() {
						return MAX_AUTHORIZE_ACCESSORY_LEVEL;
					}

					public void setLevel(int level) {
						MAX_AUTHORIZE_ACCESSORY_LEVEL = level;
					}
				}
			}

			@Getter
			public static class PlumeProperties {
				private final MaxProperties max = new MaxProperties();

				public static class MaxProperties {
					public int getLevel() {
						return MAX_AUTHORIZE_PLUME_LEVEL;
					}

					public void setLevel(int level) {
						MAX_AUTHORIZE_PLUME_LEVEL = level;
					}
				}
			}

			@Getter
			public static class BraceletProperties {
				private final MaxProperties max = new MaxProperties();

				public static class MaxProperties {
					public int getLevel() {
						return MAX_AUTHORIZE_BRACELET_LEVEL;
					}

					public void setLevel(int level) {
						MAX_AUTHORIZE_BRACELET_LEVEL = level;
					}
				}
			}

			public static class CastProperties {
				public int getTime() {
					return AUTHORIZE_SPEED;
				}

				public void setTime(int time) {
					AUTHORIZE_SPEED = time;
				}
			}
		}

		public static class CastProperties {
			public int getTime() {
				return ENCHANT_SPEED;
			}

			public void setTime(int time) {
				ENCHANT_SPEED = time;
			}
		}
	}
}
