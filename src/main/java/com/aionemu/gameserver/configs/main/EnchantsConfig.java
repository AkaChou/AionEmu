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
	@Property(key = "gameserver.socket.manastone", defaultValue = "100")
	public static float SOCKET_MANASTONE;
	/**
	 * 物品强化基础成功率（%）。
	 * Item enchant base success rate (%).
	 */
	@Property(key = "gameserver.enchant.item", defaultValue = "100")
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
	 * 天烙之石（埃斯蒂玛）最大强化等级，默认 15，最高 100。
	 * Maximum Estima (CP stone) enchant level, defaulting to 15 and capped at 100.
	 */
	@Property(key = "gameserver.enchant.estima.max.level", defaultValue = "15")
	public static int MAX_ESTIMA_ENCHANT_LEVEL;
	/**
	 * 天烙之石（埃斯蒂玛）强化基础成功率（%），不随强化等级变化。
	 * Estima (CP stone) enchant base success rate (%), independent of enchant level.
	 */
	@Property(key = "gameserver.enchant.estima", defaultValue = "100")
	public static float ENCHANT_ESTIMA;
	/**
	 * 天烙之石强化施法速度（毫秒）。
	 * Estima enchant cast speed in milliseconds.
	 */
	@Property(key = "gameserver.enchant.estima.speed", defaultValue = "5000")
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
	@Property(key = "gameserver.enchant.plume", defaultValue = "100")
	public static float ENCHANT_PLUME;
	/**
	 * 手镯淬炼成功率（%）。
	 * Bracelet tempering success rate (%).
	 */
	@Property(key = "gameserver.enchant.bracelet", defaultValue = "100")
	public static float ENCHANT_BRACELET;
	/**
	 * 饰品（耳环/戒指/项链/头盔/腰带）淬炼成功率（%）。
	 * Accessory (earrings/rings/necklace/helmet/belt) tempering success rate (%).
	 */
	@Property(key = "gameserver.enchant.accessory", defaultValue = "100")
	public static float ENCHANT_ACCESSORY;
	/**
	 * 烙印之石强化成功率（%）。
	 * Stigma enchant success rate (%).
	 */
	@Property(key = "gameserver.enchant.stigma", defaultValue = "100")
	public static float ENCHANT_STIGMA;
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
	@Property(key = "gameserver.enchant.cast.speed", defaultValue = "4000")
	public static int ENCHANT_SPEED;
	/**
	 * 觉醒（淬炼）施法速度（毫秒），觉醒加速生效时取一半。
	 * Authorize (tempering) cast speed in milliseconds, halved while authorize boost is active.
	 */
	@Property(key = "gameserver.enchant.authorize.speed", defaultValue = "3000")
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

	public static class SocketProperties {
		public float getManastone() {
			return SOCKET_MANASTONE;
		}

		public void setManastone(float manastone) {
			SOCKET_MANASTONE = manastone;
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

		public void setItem(ItemProperties item) {
		}

		/**
		 * 裸标量键存在时由 Binder 忽略；保留以兼容 {@code gameserver.enchant.estima=100}。
		 * Ignored by the Binder when the bare scalar key is present; kept for
		 * {@code gameserver.enchant.estima=100} compatibility.
		 */
		public void setEstima(EstimaProperties estima) {
		}

		public float getPlume() {
			return ENCHANT_PLUME;
		}

		public void setPlume(float plume) {
			ENCHANT_PLUME = plume;
		}

		public float getBracelet() {
			return ENCHANT_BRACELET;
		}

		public void setBracelet(float bracelet) {
			ENCHANT_BRACELET = bracelet;
		}

		public float getAccessory() {
			return ENCHANT_ACCESSORY;
		}

		public void setAccessory(float accessory) {
			ENCHANT_ACCESSORY = accessory;
		}

		public float getStigma() {
			return ENCHANT_STIGMA;
		}

		public void setStigma(float stigma) {
			ENCHANT_STIGMA = stigma;
		}

		/**
		 * 天烙之石强化配置：基础成功率、等级上限与施法速度，绑定至静态配置门面。
		 * Binds Estima base success rate, level cap and cast speed to the static configuration facade.
		 */
		@Getter
		public static class EstimaProperties {
			private final MaxProperties max = new MaxProperties();

			/**
			 * 支持裸标量形式 {@code gameserver.enchant.estima=100}。
			 * Supports the bare scalar form {@code gameserver.enchant.estima=100}.
			 */
			public static EstimaProperties valueOf(String value) {
				EstimaProperties props = new EstimaProperties();
				try {
					ENCHANT_ESTIMA = Float.parseFloat(value);
				} catch (NumberFormatException ignored) {
				}
				return props;
			}

			public float getValue() {
				return ENCHANT_ESTIMA;
			}

			public void setValue(float value) {
				ENCHANT_ESTIMA = value;
			}

			public int getSpeed() {
				return ESTIMA_ENCHANT_SPEED;
			}

			public void setSpeed(int speed) {
				ESTIMA_ENCHANT_SPEED = speed;
			}

			public static class MaxProperties {
				public int getLevel() {
					return MAX_ESTIMA_ENCHANT_LEVEL;
				}

				public void setLevel(int level) {
					MAX_ESTIMA_ENCHANT_LEVEL = level;
				}
			}
		}

		@Getter
		public static class ItemProperties {
			public ItemProperties() {
			}

			public static ItemProperties valueOf(String value) {
				ItemProperties props = new ItemProperties();
				try {
					ENCHANT_ITEM = Float.parseFloat(value);
				} catch (NumberFormatException ignored) {
				}
				return props;
			}

			public float getValue() {
				return ENCHANT_ITEM;
			}

			public void setValue(float value) {
				ENCHANT_ITEM = value;
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

			public int getSpeed() {
				return AUTHORIZE_SPEED;
			}

			public void setSpeed(int speed) {
				AUTHORIZE_SPEED = speed;
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
		}

		public static class CastProperties {
			public int getSpeed() {
				return ENCHANT_SPEED;
			}

			public void setSpeed(int speed) {
				ENCHANT_SPEED = speed;
			}
		}
	}
}
