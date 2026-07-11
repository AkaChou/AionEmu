package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 排行榜更新相关配置。
 * Top ranking update related configuration.
 */
public class RankingConfig {
	/**
	 * 是否使用自定义排行榜更新设置。
	 * Whether custom top ranking update settings are used.
	 */
	@Property(key = "gameserver.top.ranking.update.setting", defaultValue = "true")
	public static boolean TOP_RANKING_UPDATE_SETTING;

	/**
	 * 排行榜更新 Cron 规则。
	 * Cron rule for top ranking update.
	 */
	@Property(key = "gameserver.top.ranking.update.hour", defaultValue = "0 0 */2 ? * *")
	public static String TOP_RANKING_UPDATE_RULE;

	/**
	 * 排行榜更新备用分钟间隔。
	 * Alternative ranking update interval in minutes.
	 */
	@Property(key = "gameserver.top.ranking.update.minute", defaultValue = "10")
	public static int TOP_RANKING_UPDATE_RULE2;

	/**
	 * 排行榜统计允许的最大离线天数（0 表示不限制）。
	 * Max offline days allowed in ranking (0 means unlimited).
	 */
	@Property(key = "gameserver.top.ranking.max.offline.days", defaultValue = "0")
	public static int TOP_RANKING_MAX_OFFLINE_DAYS;
}
