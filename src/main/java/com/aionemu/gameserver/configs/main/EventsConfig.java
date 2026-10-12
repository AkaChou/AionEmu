package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 运营活动相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * In-game events related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
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

	/** 活动绑定树 / event binding tree. */
	@Getter
	private final EventProperties event = new EventProperties();
	/** 装饰绑定树 / decoration binding tree. */
	@Getter
	private final EnableDecorProperties enable = new EnableDecorProperties();
	/** 活动赠礼绑定树 / event-give binding tree. */
	@Getter
	private final EventsGiveProperties events = new EventsGiveProperties();
	/** VIP 票据绑定树 / VIP-ticket binding tree. */
	@Getter
	private final VipProperties vip = new VipProperties();
	/** 术古绑定树 / shugo binding tree. */
	@Getter
	private final ShugoProperties shugo = new ShugoProperties();
	/** 疯狂大天使绑定树 / crazy-daeva binding tree. */
	@Getter
	private final CrazyProperties crazy = new CrazyProperties();
	/** 升级街机绑定树 / upgrade-arcade binding tree. */
	@Getter
	private final UpgradeProperties upgrade = new UpgradeProperties();
	/** 波比猪奖励绑定树 / pig-poppy rewards binding tree. */
	@Getter
	private final PigRewardsProperties pig = new PigRewardsProperties();

	/**
	 * 活动域节点：绑定 gameserver.event.*。
	 * Event domain node binding gameserver.event.*.
	 */
	@Getter
	public static class EventProperties {
		private final ServiceProperties service = new ServiceProperties();
		private final AwakeProperties awake = new AwakeProperties();
		private final SeedProperties seed = new SeedProperties();
		private final ArcadeProperties arcade = new ArcadeProperties();
		private final AbyssProperties abyss = new AbyssProperties();
		private final EventPigProperties pig = new EventPigProperties();
		private final ShugoSweepProperties shugoSweep = new ShugoSweepProperties();

		public boolean isEnable() {
			return EVENT_ENABLED;
		}

		public void setEnable(boolean enable) {
			EVENT_ENABLED = enable;
		}

		public String getYoutubeVideo() {
			return EVENT_YOUTUBE_VIDEO;
		}

		public void setYoutubeVideo(String youtubeVideo) {
			EVENT_YOUTUBE_VIDEO = youtubeVideo;
		}
	}

	/** 活动服务节点 / Event-service node. */
	public static class ServiceProperties {
		public boolean isEnable() {
			return ENABLE_EVENT_SERVICE;
		}

		public void setEnable(boolean enable) {
			ENABLE_EVENT_SERVICE = enable;
		}
	}

	/** 觉醒活动节点 / Awake-event node. */
	public static class AwakeProperties {
		public boolean isEnable() {
			return ENABLE_AWAKE_EVENT;
		}

		public void setEnable(boolean enable) {
			ENABLE_AWAKE_EVENT = enable;
		}
	}

	/** 种子变身父节点 / Seed-transformation parent node. */
	@Getter
	public static class SeedProperties {
		private final SeedTransformationProperties transformation = new SeedTransformationProperties();
	}

	/** 种子变身节点 / Seed-transformation node. */
	public static class SeedTransformationProperties {
		public int getTime() {
			return SEED_TRANSFORMATION_PERIOD;
		}

		public void setTime(int time) {
			SEED_TRANSFORMATION_PERIOD = time;
		}
	}

	/** 升级街机节点 / Upgrade-arcade node. */
	public static class ArcadeProperties {
		public boolean isEnable() {
			return ENABLE_EVENT_ARCADE;
		}

		public void setEnable(boolean enable) {
			ENABLE_EVENT_ARCADE = enable;
		}
	}

	/** 欧比斯宝藏父节点 / Abyss-treasure parent node. */
	@Getter
	public static class AbyssProperties {
		private final AbyssTreasureProperties treasure = new AbyssTreasureProperties();

		public String getRewards() {
			return ABYSS_EVENT_REWARDS;
		}

		public void setRewards(String rewards) {
			ABYSS_EVENT_REWARDS = rewards;
		}
	}

	/** 欧比斯宝藏节点 / Abyss-treasure node. */
	@Getter
	public static class AbyssTreasureProperties {
		public boolean isEnable() {
			return ENABLE_ABYSS_EVENT;
		}

		public void setEnable(boolean enable) {
			ENABLE_ABYSS_EVENT = enable;
		}

		public String getTime() {
			return ABYSS_EVENT_SCHEDULE;
		}

		public void setTime(String time) {
			ABYSS_EVENT_SCHEDULE = time;
		}
	}

	/**
	 * 活动波比猪父节点：对应配置元素 gameserver.event.pig。
	 * Event pig-poppy parent node matching the gameserver.event.pig element.
	 */
	@Getter
	public static class EventPigProperties {
		private final EventPigPoppyProperties poppy = new EventPigPoppyProperties();

		/**
		 * 兼容裸标量 gameserver.event.pig.poppy 经 valueOf 转换后的赋值（no-op）。
		 * No-op setter for the valueOf-converted legacy scalar gameserver.event.pig.poppy.
		 */
		public void setPoppy(EventPigPoppyProperties poppy) {
		}
	}

	/**
	 * 活动波比猪父节点：对应配置元素 gameserver.event.pig.poppy（valueOf 命中裸标量）。
	 * Event pig-poppy parent node matching the gameserver.event.pig.poppy element (valueOf catches the bare scalar).
	 */
	@Getter
	public static class EventPigPoppyProperties {
		private final PigPoppyRewardProperties reward = new PigPoppyRewardProperties();

		public boolean isEnable() {
			return ENABLE_PIG_POPPY_EVENT;
		}

		public void setEnable(boolean enable) {
			ENABLE_PIG_POPPY_EVENT = enable;
		}

		/**
		 * 兼容旧版裸标量 gameserver.event.pig.poppy=0 0 22 ? * SAT 的绑定入口。
		 * Binding entry for the legacy bare scalar gameserver.event.pig.poppy cron expression.
		 */
		public static EventPigPoppyProperties valueOf(String value) {
			try {
				PIG_POPPY_EVENT_SCHEDULE = value;
			} catch (RuntimeException ignored) {
			}
			return new EventPigPoppyProperties();
		}

		public String getValue() {
			return PIG_POPPY_EVENT_SCHEDULE;
		}

		public void setValue(String value) {
			PIG_POPPY_EVENT_SCHEDULE = value;
		}
	}

	/** 波比猪奖励数量节点：绑定 gameserver.event.pig.poppy.reward.count。 / Pig-poppy reward-count node binding gameserver.event.pig.poppy.reward.count. */
	public static class PigPoppyRewardProperties {
		public int getCount() {
			return PIG_POPPY_EVENT_COUNT_REWARD;
		}

		public void setCount(int count) {
			PIG_POPPY_EVENT_COUNT_REWARD = count;
		}
	}

	/** 术古扫荡节点 / Shugo-sweep node. */
	public static class ShugoSweepProperties {
		public int getBoard() {
			return EVENT_SHUGOSWEEP_BOARD;
		}

		public void setBoard(int board) {
			EVENT_SHUGOSWEEP_BOARD = board;
		}

		public int getFreeDice() {
			return EVENT_SHUGOSWEEP_FREEDICE;
		}

		public void setFreeDice(int freeDice) {
			EVENT_SHUGOSWEEP_FREEDICE = freeDice;
		}
	}

	/**
	 * 装饰节点：绑定 gameserver.enable.decor。
	 * Decoration node binding gameserver.enable.decor.
	 */
	public static class EnableDecorProperties {
		public int getDecor() {
			return ENABLE_DECOR;
		}

		public void setDecor(int decor) {
			ENABLE_DECOR = decor;
		}
	}

	/** 活动赠礼父节点：对应配置元素 gameserver.events。 / Event-give parent node matching gameserver.events. */
	@Getter
	public static class EventsGiveProperties {
		private final GiveProperties give = new GiveProperties();
	}

	/** 活动赠礼节点 / Event-give node. */
	public static class GiveProperties {
		public int getJuice() {
			return EVENT_GIVE_JUICE;
		}

		public void setJuice(int juice) {
			EVENT_GIVE_JUICE = juice;
		}

		public int getCake() {
			return EVENT_GIVE_CAKE;
		}

		public void setCake(int cake) {
			EVENT_GIVE_CAKE = cake;
		}
	}

	/** VIP 票据父节点 / VIP-ticket parent node. */
	@Getter
	public static class VipProperties {
		private final TicketsProperties tickets = new TicketsProperties();
	}

	/** VIP 票据节点 / VIP-ticket node. */
	public static class TicketsProperties {
		public boolean isEnable() {
			return ENABLE_VIP_TICKETS;
		}

		public void setEnable(boolean enable) {
			ENABLE_VIP_TICKETS = enable;
		}

		public int getTime() {
			return VIP_TICKETS_PERIOD;
		}

		public void setTime(int time) {
			VIP_TICKETS_PERIOD = time;
		}
	}

	/**
	 * 术古域节点：绑定 gameserver.shugo.*。
	 * Shugo domain node binding gameserver.shugo.*.
	 */
	@Getter
	public static class ShugoProperties {
		private final ImperialProperties imperial = new ImperialProperties();
	}

	/** 术古帝国陵墓父节点 / Shugo Imperial Tomb parent node. */
	@Getter
	public static class ImperialProperties {
		private final TombProperties tomb = new TombProperties();
	}

	/**
	 * 术古帝国陵墓节点：绑定 gameserver.shugo.imperial.tomb.*。
	 * Shugo Imperial Tomb node binding gameserver.shugo.imperial.tomb.*.
	 */
	@Getter
	public static class TombProperties {
		private final TombTimerProperties timer = new TombTimerProperties();
		private final TombTimeProperties time = new TombTimeProperties();

		public boolean isEnable() {
			return IMPERIAL_TOMB_ENABLE;
		}

		public void setEnable(boolean enable) {
			IMPERIAL_TOMB_ENABLE = enable;
		}
	}

	/**
	 * 陵墓时长父节点：对应配置元素 gameserver.shugo.imperial.tomb.timer。
	 * Tomb-timer parent node matching the gameserver.shugo.imperial.tomb.timer element.
	 */
	@Getter
	public static class TombTimerProperties {
		private final TombTimerFromProperties from = new TombTimerFromProperties();
	}

	/** 陵墓时长起始父节点 / Tomb-timer from parent node. */
	@Getter
	public static class TombTimerFromProperties {
		private final TombTimerFromStartProperties start = new TombTimerFromStartProperties();
	}

	/** 陵墓时长起始父节点 / Tomb-timer from-start parent node. */
	@Getter
	public static class TombTimerFromStartProperties {
		private final TombTimerFromStartToProperties to = new TombTimerFromStartToProperties();
	}

	/**
	 * 陵墓时长结束节点：绑定 gameserver.shugo.imperial.tomb.timer.from.start.to.end。
	 * Tomb-timer end node binding gameserver.shugo.imperial.tomb.timer.from.start.to.end.
	 */
	public static class TombTimerFromStartToProperties {
		public long getEnd() {
			return IMPERIAL_TOMB_TIMER;
		}

		public void setEnd(long end) {
			IMPERIAL_TOMB_TIMER = end;
		}
	}

	/**
	 * 陵墓开始时间父节点：对应配置元素 gameserver.shugo.imperial.tomb.time。
	 * Tomb start-time parent node matching the gameserver.shugo.imperial.tomb.time element.
	 */
	@Getter
	public static class TombTimeProperties {
		private final TombTimeToProperties to = new TombTimeToProperties();
	}

	/**
	 * 陵墓开始时间节点：绑定 gameserver.shugo.imperial.tomb.time.to.start。
	 * Tomb start-time node binding gameserver.shugo.imperial.tomb.time.to.start.
	 */
	public static class TombTimeToProperties {
		public String getStart() {
			return IMPERIAL_TOMB_TIMES;
		}

		public void setStart(String start) {
			IMPERIAL_TOMB_TIMES = start;
		}
	}

	/**
	 * 疯狂大天使域节点：绑定 gameserver.crazy.*。
	 * Crazy-daeva domain node binding gameserver.crazy.*.
	 */
	@Getter
	public static class CrazyProperties {
		private final CrazyDaevaProperties daeva = new CrazyDaevaProperties();
	}

	/** 疯狂大天使节点 / Crazy-daeva node. */
	@Getter
	public static class CrazyDaevaProperties {
		private final CrazyLowestProperties lowest = new CrazyLowestProperties();
		private final CrazyTimeProperties time = new CrazyTimeProperties();

		public boolean isEnable() {
			return ENABLE_CRAZY;
		}

		public void setEnable(boolean enable) {
			ENABLE_CRAZY = enable;
		}

		public int getEndtime() {
			return CRAZY_ENDTIME;
		}

		public void setEndtime(int endtime) {
			CRAZY_ENDTIME = endtime;
		}
	}

	/** 疯狂大天使最低随机父节点 / Crazy-daeva lowest-random parent node. */
	@Getter
	public static class CrazyLowestProperties {
		public int getRnd() {
			return CRAZY_LOWEST_RND;
		}

		public void setRnd(int rnd) {
			CRAZY_LOWEST_RND = rnd;
		}
	}

	/** 疯狂大天使时间父节点 / Crazy-daeva time parent node. */
	@Getter
	public static class CrazyTimeProperties {
		private final CrazyTimeToProperties to = new CrazyTimeToProperties();
	}

	/**
	 * 疯狂大天使开始时间节点：绑定 gameserver.crazy.daeva.time.to.start。
	 * Crazy-daeva start-time node binding gameserver.crazy.daeva.time.to.start.
	 */
	public static class CrazyTimeToProperties {
		public String getStart() {
			return CRAZY_TIMES;
		}

		public void setStart(String start) {
			CRAZY_TIMES = start;
		}
	}

	/**
	 * 升级街机域节点：绑定 gameserver.upgrade.*。
	 * Upgrade-arcade domain node binding gameserver.upgrade.*.
	 */
	@Getter
	public static class UpgradeProperties {
		private final UpgradeArcadeProperties arcade = new UpgradeArcadeProperties();
	}

	/**
	 * 升级街机概率节点：绑定 gameserver.upgrade.arcade.chance。
	 * Upgrade-arcade chance node binding gameserver.upgrade.arcade.chance.
	 */
	public static class UpgradeArcadeProperties {
		public int getChance() {
			return EVENT_ARCADE_CHANCE;
		}

		public void setChance(int chance) {
			EVENT_ARCADE_CHANCE = chance;
		}
	}

	/**
	 * 波比猪奖励域节点：绑定 gameserver.pig.poppy.*。
	 * Pig-poppy rewards domain node binding gameserver.pig.poppy.*.
	 */
	@Getter
	public static class PigRewardsProperties {
		private final PigPoppyRewardsProperties poppy = new PigPoppyRewardsProperties();
	}

	/**
	 * 波比猪奖励节点：绑定 gameserver.pig.poppy.rewards。
	 * Pig-poppy rewards node binding gameserver.pig.poppy.rewards.
	 */
	public static class PigPoppyRewardsProperties {
		public String getRewards() {
			return PIG_POPPY_REWARDS;
		}

		public void setRewards(String rewards) {
			PIG_POPPY_REWARDS = rewards;
		}
	}
}
