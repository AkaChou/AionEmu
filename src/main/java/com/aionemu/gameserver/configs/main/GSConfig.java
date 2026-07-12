package com.aionemu.gameserver.configs.main;

import java.util.Calendar;

import com.aionemu.commons.configuration.Property;

/**
 * 游戏服务器核心相关配置。
 * Game server core related configuration.
 */
public class GSConfig {
	/**
	 * 服务器国家代码。
	 * Server country code.
	 */
	@Property(key = "gameserver.country.code", defaultValue = "1")
	public static int SERVER_COUNTRY_CODE;
	/**
	 * 玩家最大等级。
	 * Player maximum level.
	 */
	@Property(key = "gameserver.players.max.level", defaultValue = "83")
	public static int PLAYER_MAX_LEVEL;
	/**
	 * 服务器时区 ID（空则使用系统时区）。
	 * Server timezone ID (empty uses system timezone).
	 */
	@Property(key = "gameserver.timezone", defaultValue = "")
	public static String TIME_ZONE_ID = Calendar.getInstance().getTimeZone().getID();
	/**
	 * 是否启用聊天服务器。
	 * Whether chat server is enabled.
	 */
	@Property(key = "gameserver.chatserver.enable", defaultValue = "false")
	public static boolean ENABLE_CHAT_SERVER;
	/**
	 * 是否启用启动进度输出。
	 * Whether startup progress output is enabled.
	 */
	@Property(key = "gameserver.startup.progress.enable", defaultValue = "true")
	public static boolean STARTUP_PROGRESS_ENABLE = true;
	/**
	 * 是否在静态数据加载进度中显示条目数。
	 * Whether static-data progress shows entry counts.
	 */
	@Property(key = "gameserver.static_data.progress.entry_counts.enable", defaultValue = "false")
	public static boolean STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE = false;
	/**
	 * 是否输出静态数据加载摘要日志（硬编码字段）。
	 * Whether static-data summary log is enabled (hardcoded field).
	 */
	public static boolean STATIC_DATA_SUMMARY_LOG;
	/**
	 * 角色创建模式。
	 * Character creation mode.
	 */
	@Property(key = "gameserver.character.creation.mode", defaultValue = "0")
	public static int CHARACTER_CREATION_MODE;
	/**
	 * 每账号角色数量上限。
	 * Maximum character count per account.
	 */
	@Property(key = "gameserver.character.limit.count", defaultValue = "8")
	public static int CHARACTER_LIMIT_COUNT;
	/**
	 * 角色阵营限制模式。
	 * Character faction limitation mode.
	 */
	@Property(key = "gameserver.character.faction.limitation.mode", defaultValue = "0")
	public static int CHARACTER_FACTION_LIMITATION_MODE;
	/**
	 * 是否启用阵营人数比例限制。
	 * Whether faction ratio limitation is enabled.
	 */
	@Property(key = "gameserver.ratio.limitation.enable", defaultValue = "false")
	public static boolean ENABLE_RATIO_LIMITATION;
	/**
	 * 比例限制统计的最低角色等级。
	 * Minimum character level counted for ratio limitation.
	 */
	@Property(key = "gameserver.ratio.min.required.level", defaultValue = "10")
	public static int RATIO_MIN_REQUIRED_LEVEL;
	/**
	 * 比例限制生效所需的最少角色数。
	 * Minimum characters count required before ratio limitation applies.
	 */
	@Property(key = "gameserver.ratio.min.characters_count", defaultValue = "50")
	public static int RATIO_MIN_CHARACTERS_COUNT;
	/**
	 * 是否使用精简欧比斯排行缓存。
	 * Whether abyss ranking uses small cache.
	 */
	@Property(key = "gameserver.abyssranking.small.cache", defaultValue = "false")
	public static boolean ABYSSRANKING_SMALL_CACHE;
	/**
	 * 角色重进世界冷却时间（秒）。
	 * Character re-entry cooldown time in seconds.
	 */
	@Property(key = "gameserver.character.reentry.time", defaultValue = "20")
	public static int CHARACTER_REENTRY_TIME;
	/**
	 * 是否启用 YA 管理面板服务。
	 * Whether YA admin panel server is enabled.
	 */
	@Property(key = "gameserver.yaadminpanel.server.enable", defaultValue = "false")
	public static boolean SERVER_YAADMINPANEL_SWITCH_ON;
}
