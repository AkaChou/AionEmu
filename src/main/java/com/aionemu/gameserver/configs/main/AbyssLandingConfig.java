package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 欧比斯登陆点任务与积分重置相关配置。
 * Abyss landing quest and points reset related configuration.
 */
public class AbyssLandingConfig {
	/**
	 * 是否启用欧比斯登陆点任务重置。
	 * Whether abyss landing quest reset is enabled.
	 */
	@Property(key = "gameserver.landing.quest.reset.enable", defaultValue = "true")
	public static boolean ABYSS_LANDING_QUEST_RESET_ENABLED;
	/**
	 * 是否启用欧比斯登陆点积分重置。
	 * Whether abyss landing points reset is enabled.
	 */
	@Property(key = "gameserver.landing.points.reset.enable", defaultValue = "true")
	public static boolean ABYSS_LANDING_POINTS_RESET_ENABLED;
	/**
	 * 欧比斯登陆点任务重置 Cron 时间。
	 * Cron schedule for abyss landing quest reset.
	 */
	@Property(key = "gameserver.landing.quest.reset.time", defaultValue = "0 0 12 ? * MON *")
	public static String ABYSS_LANDING_QUEST_RESET_TIME;
	/**
	 * 欧比斯登陆点积分重置 Cron 时间。
	 * Cron schedule for abyss landing points reset.
	 */
	@Property(key = "gameserver.landing.points.reset.time", defaultValue = "0 0 0 ? * MON *")
	public static String ABYSS_LANDING_POINTS_RESET_TIME;
	/**
	 * 欧比斯登陆点任务奖励倍率。
	 * Abyss landing quest reward rate.
	 */
	@Property(key = "gameserver.landing.quest.rate", defaultValue = "1")
	public static int ABYSS_LANDING_QUEST_RATE;
}
