package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 角色与欧比斯数据清理相关配置。
 * Character and abyss data cleaning related configuration.
 */
public class CleaningConfig {

	/**
	 * 是否启用角色清理。
	 * Whether character cleaning is enabled.
	 */
	@Property(key = "gameserver.cleaning.enable", defaultValue = "false")
	public static boolean CLEANING_ENABLE;

	/**
	 * 角色清理周期（天）。
	 * Character cleaning period in days.
	 */
	@Property(key = "gameserver.cleaning.period", defaultValue = "180")
	public static int CLEANING_PERIOD;

	/**
	 * 角色清理线程数。
	 * Number of character cleaning threads.
	 */
	@Property(key = "gameserver.cleaning.threads", defaultValue = "2")
	public static int CLEANING_THREADS;

	/**
	 * 每次清理的角色数量上限。
	 * Maximum characters cleaned per run.
	 */
	@Property(key = "gameserver.cleaning.limit", defaultValue = "5000")
	public static int CLEANING_LIMIT;

	/**
	 * 是否启用欧比斯数据清理。
	 * Whether abyss ranking cleaning is enabled.
	 */
	@Property(key = "gameserver.abyss.cleaning.enable", defaultValue = "false")
	public static boolean ABYSS_CLEANING_ENABLE;

	/**
	 * 欧比斯数据清理周期（天）。
	 * Abyss ranking cleaning period in days.
	 */
	@Property(key = "gameserver.abyss.cleaning.period", defaultValue = "180")
	public static int ABYSS_CLEANING_PERIOD;
}
