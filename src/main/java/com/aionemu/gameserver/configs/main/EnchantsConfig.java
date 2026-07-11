package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 强化与镶嵌相关配置。
 * Enchant and socket related configuration.
 */
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
}
