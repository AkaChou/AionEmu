package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 运营活动相关配置。
 * In-game events related configuration.
 */
public class EventsConfig {
	/**
	 * 是否启用通用活动开关。
	 * Whether the general event system is enabled.
	 */
	@Property(key = "gameserver.event.enable", defaultValue = "false")
	public static boolean EVENT_ENABLED;
	/**
	 * 活动装饰类型 ID。
	 * Event decoration type ID.
	 */
	@Property(key = "gameserver.enable.decor", defaultValue = "0")
	public static int ENABLE_DECOR;
	/**
	 * 活动赠送果汁道具 ID。
	 * Event juice item ID to give.
	 */
	@Property(key = "gameserver.events.give.juice", defaultValue = "160009017")
	public static int EVENT_GIVE_JUICE;
	/**
	 * 活动赠送蛋糕道具 ID。
	 * Event cake item ID to give.
	 */
	@Property(key = "gameserver.events.give.cake", defaultValue = "160010073")
	public static int EVENT_GIVE_CAKE;
	/**
	 * 是否启用活动服务。
	 * Whether the event service is enabled.
	 */
	@Property(key = "gameserver.event.service.enable", defaultValue = "false")
	public static boolean ENABLE_EVENT_SERVICE;

	/**
	 * 是否启用 VIP 票据活动。
	 * Whether VIP tickets event is enabled.
	 */
	@Property(key = "gameserver.vip.tickets.enable", defaultValue = "false")
	public static boolean ENABLE_VIP_TICKETS;
	/**
	 * VIP 票据发放周期。
	 * VIP tickets period.
	 */
	@Property(key = "gameserver.vip.tickets.time", defaultValue = "60")
	public static int VIP_TICKETS_PERIOD;

	/**
	 * 是否启用觉醒活动（日服活动）。
	 * Whether the Awake event (JAP) is enabled.
	 */
	@Property(key = "gameserver.event.awake.enable", defaultValue = "false")
	public static boolean ENABLE_AWAKE_EVENT;
	/**
	 * 种子变身活动周期。
	 * Seed transformation event period.
	 */
	@Property(key = "gameserver.event.seed.transformation.time", defaultValue = "60")
	public static int SEED_TRANSFORMATION_PERIOD;

	/**
	 * 是否启用术古帝国陵墓（4.3）。
	 * Whether Shugo Imperial Tomb (4.3) is enabled.
	 */
	@Property(key = "gameserver.shugo.imperial.tomb.enable", defaultValue = "true")
	public static boolean IMPERIAL_TOMB_ENABLE;
	/**
	 * 术古帝国陵墓从开始到结束的时长。
	 * Shugo Imperial Tomb duration from start to end.
	 */
	@Property(key = "gameserver.shugo.imperial.tomb.timer.from.start.to.end", defaultValue = "10")
	public static long IMPERIAL_TOMB_TIMER;
	/**
	 * 术古帝国陵墓开始 Cron 时间。
	 * Cron schedule for Shugo Imperial Tomb start times.
	 */
	@Property(key = "gameserver.shugo.imperial.tomb.time.to.start", defaultValue = "0 0 0,12,20,0 ? * *")
	public static String IMPERIAL_TOMB_TIMES;

	/**
	 * 是否启用疯狂大天使活动。
	 * Whether Crazy Daeva event is enabled.
	 */
	@Property(key = "gameserver.crazy.daeva.enable", defaultValue = "false")
	public static boolean ENABLE_CRAZY;
	/**
	 * 疯狂大天使最低随机值。
	 * Crazy Daeva lowest random value.
	 */
	@Property(key = "gameserver.crazy.daeva.lowest.rnd", defaultValue = "10")
	public static int CRAZY_LOWEST_RND;
	/**
	 * 疯狂大天使开始 Cron 时间。
	 * Cron schedule for Crazy Daeva start times.
	 */
	@Property(key = "gameserver.crazy.daeva.time.to.start", defaultValue = "0 0 0,12,20,0 ? * *")
	public static String CRAZY_TIMES;
	/**
	 * 疯狂大天使结束时间（分钟）。
	 * Crazy Daeva end time in minutes.
	 */
	@Property(key = "gameserver.crazy.daeva.endtime", defaultValue = "5")
	public static int CRAZY_ENDTIME;

	/**
	 * 是否启用升级街机活动（4.7）。
	 * Whether Upgrade Arcade event (4.7) is enabled.
	 */
	@Property(key = "gameserver.event.arcade.enable", defaultValue = "false")
	public static boolean ENABLE_EVENT_ARCADE;
	/**
	 * 升级街机成功概率。
	 * Upgrade Arcade success chance.
	 */
	@Property(key = "gameserver.upgrade.arcade.chance", defaultValue = "50")
	public static int EVENT_ARCADE_CHANCE;

	/**
	 * 是否启用欧比斯宝藏活动。
	 * Whether Abyss Treasure event is enabled.
	 */
	@Property(key = "gameserver.event.abyss.treasure.enable", defaultValue = "false")
	public static boolean ENABLE_ABYSS_EVENT;
	/**
	 * 欧比斯宝藏活动 Cron 时间。
	 * Cron schedule for Abyss Treasure event.
	 */
	@Property(key = "gameserver.event.abyss.treasure.time", defaultValue = "0 0 15 ? * SUN")
	public static String ABYSS_EVENT_SCHEDULE;
	/**
	 * 欧比斯宝藏活动奖励列表。
	 * Abyss Treasure event rewards list.
	 */
	@Property(key = "gameserver.event.abyss.rewards", defaultValue = "0")
	public static String ABYSS_EVENT_REWARDS;

	/**
	 * 是否启用波比猪活动。
	 * Whether Pig Poppy event is enabled.
	 */
	@Property(key = "gameserver.event.pig.poppy.enable", defaultValue = "false")
	public static boolean ENABLE_PIG_POPPY_EVENT;
	/**
	 * 波比猪活动 Cron 时间。
	 * Cron schedule for Pig Poppy event.
	 */
	@Property(key = "gameserver.event.pig.poppy", defaultValue = "0 0 20 ? * SAT")
	public static String PIG_POPPY_EVENT_SCHEDULE;
	/**
	 * 波比猪活动奖励数量。
	 * Pig Poppy event reward count.
	 */
	@Property(key = "gameserver.event.pig.poppy.reward.count", defaultValue = "5")
	public static int PIG_POPPY_EVENT_COUNT_REWARD;
	/**
	 * 波比猪活动奖励列表。
	 * Pig Poppy event rewards list.
	 */
	@Property(key = "gameserver.pig.poppy.rewards", defaultValue = "0")
	public static String PIG_POPPY_REWARDS;

	/**
	 * 术古扫荡棋盘编号。
	 * Shugo Sweep board ID.
	 */
	@Property(key = "gameserver.event.shugoSweep.board", defaultValue = "1")
	public static int EVENT_SHUGOSWEEP_BOARD;
	/**
	 * 术古扫荡免费骰子次数。
	 * Shugo Sweep free dice count.
	 */
	@Property(key = "gameserver.event.shugoSweep.freeDice", defaultValue = "5")
	public static int EVENT_SHUGOSWEEP_FREEDICE;

	/**
	 * 活动 YouTube 视频嵌入地址。
	 * Event YouTube video embed URL.
	 */
	@Property(key = "gameserver.event.youtube_video", defaultValue = "https://www.youtube.com/embed/zZ7OhMY5mYg")
	public static String EVENT_YOUTUBE_VIDEO;
}
