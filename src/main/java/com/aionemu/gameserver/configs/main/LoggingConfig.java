package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 服务器日志开关相关配置。
 * Server logging toggle related configuration.
 */
public class LoggingConfig {
	/**
	 * 是否启用高级日志。
	 * Whether advanced logging is enabled.
	 */
	@Property(key = "gameserver.enable.advanced.logging", defaultValue = "false")
	public static boolean ENABLE_ADVANCED_LOGGING;
	/**
	 * 是否记录审计日志。
	 * Whether audit logging is enabled.
	 */
	@Property(key = "gameserver.log.audit", defaultValue = "true")
	public static boolean LOG_AUDIT;
	/**
	 * 是否记录聊天日志。
	 * Whether chat logging is enabled.
	 */
	@Property(key = "gameserver.log.chat", defaultValue = "true")
	public static boolean LOG_CHAT;
	/**
	 * 是否记录阵营相关日志。
	 * Whether faction logging is enabled.
	 */
	@Property(key = "gameserver.log.faction", defaultValue = "false")
	public static boolean LOG_FACTION;
	/**
	 * 是否记录 GM 审计日志。
	 * Whether GM audit logging is enabled.
	 */
	@Property(key = "gameserver.log.gmaudit", defaultValue = "true")
	public static boolean LOG_GMAUDIT;
	/**
	 * 是否记录物品日志。
	 * Whether item logging is enabled.
	 */
	@Property(key = "gameserver.log.item", defaultValue = "true")
	public static boolean LOG_ITEM;
	/**
	 * 是否记录击杀日志。
	 * Whether kill logging is enabled.
	 */
	@Property(key = "gameserver.log.kill", defaultValue = "false")
	public static boolean LOG_KILL;
	/**
	 * 是否记录数据包长度相关日志。
	 * Whether packet length (PL) logging is enabled.
	 */
	@Property(key = "gameserver.log.pl", defaultValue = "false")
	public static boolean LOG_PL;
	/**
	 * 是否记录攻城日志。
	 * Whether siege logging is enabled.
	 */
	@Property(key = "gameserver.log.siege", defaultValue = "false")
	public static boolean LOG_SIEGE;
	/**
	 * 是否记录房屋拍卖日志。
	 * Whether house auction logging is enabled.
	 */
	@Property(key = "gameserver.log.auction", defaultValue = "true")
	public static boolean LOG_HOUSE_AUCTION;
}
