package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 自动惩罚机制相关配置。
 * Automatic punishment mechanism related configuration.
 *
 * @author synchro2
 */
public class PunishmentConfig {

	/**
	 * 是否启用自动惩罚。
	 * Whether automatic punishment is enabled.
	 */
	@Property(key = "gameserver.punishment.enable", defaultValue = "false")
	public static boolean PUNISHMENT_ENABLE;
	/**
	 * 惩罚类型。
	 * Punishment type.
	 */
	@Property(key = "gameserver.punishment.type", defaultValue = "1")
	public static int PUNISHMENT_TYPE;
	/**
	 * 惩罚时长（分钟）。
	 * Punishment duration in minutes.
	 */
	@Property(key = "gameserver.punishment.time", defaultValue = "1440")
	public static int PUNISHMENT_TIME;
	/**
	 * 惩罚时减少的欧比斯点数。
	 * Abyss points reduced by punishment.
	 */
	@Property(key = "gameserver.punishment.reduceap", defaultValue = "0")
	public static int PUNISHMENT_REDUCEAP;
}
