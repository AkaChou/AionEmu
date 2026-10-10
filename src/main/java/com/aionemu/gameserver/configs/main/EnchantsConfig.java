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
	 * 魔石镶嵌成功率。
	 * Manastone socket success rate.
	 */
	@Property(key = "gameserver.socket.manastone", defaultValue = "50")
	public static float SOCKET_MANASTONE;
	/**
	 * 装备强化成功率。
	 * Equipment enchant success rate.
	 */
	@Property(key = "gameserver.enchant.item", defaultValue = "50")
	public static float ENCHANT_ITEM;
	/**
	 * 装备强化消耗基纳（-1 为默认）。
	 * Equipment enchant kinah cost (-1 for default).
	 */
	@Property(key = "gameserver.enchant.item.kinah", defaultValue = "-1")
	public static int ENCHANT_ITEM_KINAH;
	/**
	 * 装备强化等级上限。
	 * Maximum equipment enchant level.
	 */
	@Property(key = "gameserver.enchant.equipment.max.level", defaultValue = "30")
	public static int MAX_EQUIPMENT_ENCHANT_LEVEL;
	/**
	 * 羽饰强化成功率。
	 * Plume enchant success rate.
	 */
	@Property(key = "gameserver.enchant.plume", defaultValue = "50")
	public static float ENCHANT_PLUME;
	/**
	 * 手镯强化成功率。
	 * Bracelet enchant success rate.
	 */
	@Property(key = "gameserver.enchant.bracelet", defaultValue = "50")
	public static float ENCHANT_BRACELET;
	/**
	 * 饰品强化成功率。
	 * Accessory enchant success rate.
	 */
	@Property(key = "gameserver.enchant.accessory", defaultValue = "50")
	public static float ENCHANT_ACCESSORY;
	/**
	 * 烙印强化成功率。
	 * Stigma enchant success rate.
	 */
	@Property(key = "gameserver.enchant.stigma", defaultValue = "50")
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
		private final CastProperties cast = new CastProperties();

		public void setItem(ItemProperties item) {
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

		@Getter
		public static class ItemProperties {
			public ItemProperties() {}

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
