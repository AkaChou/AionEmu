package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 周期性数据保存间隔相关配置。
 * Periodic data save interval related configuration.
 */
public class PeriodicSaveConfig {
	/**
	 * 玩家通用数据保存间隔（秒）。
	 * Player general data save interval in seconds.
	 */
	@Property(key = "gameserver.periodicsave.player.general", defaultValue = "900")
	public static int PLAYER_GENERAL;
	/**
	 * 玩家物品数据保存间隔（秒）。
	 * Player items save interval in seconds.
	 */
	@Property(key = "gameserver.periodicsave.player.items", defaultValue = "900")
	public static int PLAYER_ITEMS;
	/**
	 * 军团物品数据保存间隔（秒）。
	 * Legion items save interval in seconds.
	 */
	@Property(key = "gameserver.periodicsave.legion.items", defaultValue = "1200")
	public static int LEGION_ITEMS;
	/**
	 * 玩家宠物数据保存间隔（秒）。
	 * Player pets save interval in seconds.
	 */
	@Property(key = "gameserver.periodicsave.player.pets", defaultValue = "5")
	public static int PLAYER_PETS;
}
