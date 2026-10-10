package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 自动组队与战场/副本入口相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Auto-group and battlefield/instance entry related configuration supporting Spring Boot
 * ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class AutoGroupConfig {
	/**
	 * 是否启用自动组队。
	 * Whether auto-group is enabled.
	 */
	@Property(key = "gameserver.autogroup.enable", defaultValue = "true")
	public static boolean AUTO_GROUP_ENABLED;

	/**
	 * 掘航舰计时器（分钟）。
	 * Dredgion timer in minutes.
	 */
	@Property(key = "gameserver.dredgion.timer", defaultValue = "60")
	public static long DREDGION_TIMER = 60L;

	/**
	 * 是否启用掘航舰。
	 * Whether Dredgion is enabled.
	 */
	@Property(key = "gameserver.dredgion.enable", defaultValue = "true")
	public static boolean DREDGION_ENABLED;
	/**
	 * 掘航舰中午开放 Cron。
	 * Dredgion midday open Cron schedule.
	 */
	@Property(key = "gameserver.dredgion.schedule.midday", defaultValue = "0 0 12 ? * MON,TUE,WED,THU,FRI,SAT,SUN *")
	public static String DREDGION_SCHEDULE_MIDDAY;
	/**
	 * 掘航舰晚间开放 Cron。
	 * Dredgion evening open Cron schedule.
	 */
	@Property(key = "gameserver.dredgion.schedule.evening", defaultValue = "0 0 20 ? * MON,TUE,WED,THU,FRI,SAT,SUN *")
	public static String DREDGION_SCHEDULE_EVENING;
	/**
	 * 掘航舰午夜开放 Cron。
	 * Dredgion midnight open Cron schedule.
	 */
	@Property(key = "gameserver.dredgion.schedule.midnight", defaultValue = "0 0 23 ? * MON,TUE,WED,THU,FRI,SAT,SUN *")
	public static String DREDGION_SCHEDULE_MIDNIGHT;

	/**
	 * 卡玛战场计时器（分钟，4.3）。
	 * Kamar Battlefield timer in minutes (4.3).
	 */
	@Property(key = "gameserver.kamar.timer", defaultValue = "60")
	public static long KAMAR_TIMER = 60L;
	/**
	 * 是否启用卡玛战场。
	 * Whether Kamar Battlefield is enabled.
	 */
	@Property(key = "gameserver.kamar.enable", defaultValue = "true")
	public static boolean KAMAR_ENABLED;
	/**
	 * 卡玛战场午夜开放 Cron。
	 * Kamar Battlefield midnight open Cron schedule.
	 */
	@Property(key = "gameserver.kamar.schedule.midnight", defaultValue = "0 0 23 ? * FRI *")
	public static String KAMAR_SCHEDULE_MIDNIGHT;

	/**
	 * 淹没的奥菲丹桥计时器（分钟，4.5）。
	 * Engulfed Ophidan Bridge timer in minutes (4.5).
	 */
	@Property(key = "gameserver.ophidan.timer", defaultValue = "60")
	public static long OPHIDAN_TIMER = 60L;
	/**
	 * 是否启用淹没的奥菲丹桥。
	 * Whether Engulfed Ophidan Bridge is enabled.
	 */
	@Property(key = "gameserver.ophidan.enable", defaultValue = "true")
	public static boolean OPHIDAN_ENABLED;
	/**
	 * 淹没的奥菲丹桥中午开放 Cron。
	 * Engulfed Ophidan Bridge midday open Cron schedule.
	 */
	@Property(key = "gameserver.ophidan.schedule.midday", defaultValue = "0 0 12 ? * TUE,THU,SAT *")
	public static String OPHIDAN_SCHEDULE_MIDDAY;
	/**
	 * 淹没的奥菲丹桥午夜开放 Cron。
	 * Engulfed Ophidan Bridge midnight open Cron schedule.
	 */
	@Property(key = "gameserver.ophidan.schedule.midnight", defaultValue = "0 0 23 ? * TUE,THU,SAT *")
	public static String OPHIDAN_SCHEDULE_MIDNIGHT;

	/**
	 * 铁壁战线计时器（分钟，4.5）。
	 * Iron Wall Warfront (Bastion) timer in minutes (4.5).
	 */
	@Property(key = "gameserver.bastion.timer", defaultValue = "60")
	public static long BASTION_TIMER = 60L;
	/**
	 * 是否启用铁壁战线。
	 * Whether Iron Wall Warfront (Bastion) is enabled.
	 */
	@Property(key = "gameserver.bastion.enable", defaultValue = "true")
	public static boolean BASTION_ENABLED;
	/**
	 * 铁壁战线午夜开放 Cron。
	 * Iron Wall Warfront midnight open Cron schedule.
	 */
	@Property(key = "gameserver.bastion.schedule.midnight", defaultValue = "0 0 23 ? * FRI *")
	public static String BASTION_SCHEDULE_MIDNIGHT;

	/**
	 * 伊吉尔穹顶计时器（分钟，4.7）。
	 * Idgel Dome timer in minutes (4.7).
	 */
	@Property(key = "gameserver.idgel.dome.timer", defaultValue = "60")
	public static long IDGEL_TIMER = 60L;
	/**
	 * 是否启用伊吉尔穹顶。
	 * Whether Idgel Dome is enabled.
	 */
	@Property(key = "gameserver.idgel.dome.enable", defaultValue = "true")
	public static boolean IDGEL_ENABLED;
	/**
	 * 伊吉尔穹顶中午开放 Cron。
	 * Idgel Dome midday open Cron schedule.
	 */
	@Property(key = "gameserver.idgel.dome.schedule.midday", defaultValue = "0 0 12 ? * MON,WED,FRI *")
	public static String IDGEL_SCHEDULE_MIDDAY;
	/**
	 * 伊吉尔穹顶午夜开放 Cron。
	 * Idgel Dome midnight open Cron schedule.
	 */
	@Property(key = "gameserver.idgel.dome.schedule.midnight", defaultValue = "0 0 23 ? * MON,WED,FRI *")
	public static String IDGEL_SCHEDULE_MIDNIGHT;

	/**
	 * 阿修纳塔尔掘航舰计时器（分钟，5.1）。
	 * Ashunatal Dredgion timer in minutes (5.1).
	 */
	@Property(key = "gameserver.ashunatal.dredgion.timer", defaultValue = "60")
	public static long ASHUNATAL_TIMER = 60L;
	/**
	 * 是否启用阿修纳塔尔掘航舰。
	 * Whether Ashunatal Dredgion is enabled.
	 */
	@Property(key = "gameserver.ashunatal.dredgion.enable", defaultValue = "true")
	public static boolean ASHUNATAL_ENABLED;
	/**
	 * 阿修纳塔尔掘航舰中午开放 Cron。
	 * Ashunatal Dredgion midday open Cron schedule.
	 */
	@Property(key = "gameserver.ashunatal.schedule.midday", defaultValue = "0 0 12 ? * MON,TUE,WED,THU,FRI,SAT,SUN *")
	public static String ASHUNATAL_SCHEDULE_MIDDAY;
	/**
	 * 阿修纳塔尔掘航舰晚间开放 Cron。
	 * Ashunatal Dredgion evening open Cron schedule.
	 */
	@Property(key = "gameserver.ashunatal.schedule.evening", defaultValue = "0 0 20 ? * MON,TUE,WED,THU,FRI *")
	public static String ASHUNATAL_SCHEDULE_EVENING;
	/**
	 * 阿修纳塔尔掘航舰午夜开放 Cron。
	 * Ashunatal Dredgion midnight open Cron schedule.
	 */
	@Property(key = "gameserver.ashunatal.schedule.midnight", defaultValue = "0 0 23 ? * SAT,SUN *")
	public static String ASHUNATAL_SCHEDULE_MIDNIGHT;

	/**
	 * 奥菲丹战径计时器（分钟，5.1）。
	 * Ophidan Warpath timer in minutes (5.1).
	 */
	@Property(key = "gameserver.ophidan.warpath.timer", defaultValue = "60")
	public static long OPHIDAN_WARPATH_TIMER = 60L;
	/**
	 * 是否启用奥菲丹战径。
	 * Whether Ophidan Warpath is enabled.
	 */
	@Property(key = "gameserver.ophidan.warpath.enable", defaultValue = "true")
	public static boolean OPHIDAN_WARPATH_ENABLED;
	/**
	 * 奥菲丹战径午夜开放 Cron。
	 * Ophidan Warpath midnight open Cron schedule.
	 */
	@Property(key = "gameserver.ophidan.warpath.schedule.midnight", defaultValue = "0 0 23 ? * TUE,THU *")
	public static String OPHIDAN_WARPATH_SCHEDULE_MIDNIGHT;

	/**
	 * 伊吉尔穹顶地标计时器（分钟，5.1）。
	 * Idgel Dome Landmark timer in minutes (5.1).
	 */
	@Property(key = "gameserver.idgel.dome.landmark.timer", defaultValue = "60")
	public static long IDGEL_DOME_LANDMARK_TIMER = 60L;
	/**
	 * 是否启用伊吉尔穹顶地标。
	 * Whether Idgel Dome Landmark is enabled.
	 */
	@Property(key = "gameserver.idgel.dome.landmark.enable", defaultValue = "true")
	public static boolean IDGEL_DOME_LANDMARK_ENABLED;
	/**
	 * 伊吉尔穹顶地标午夜开放 Cron。
	 * Idgel Dome Landmark midnight open Cron schedule.
	 */
	@Property(key = "gameserver.idgel.dome.landmark.schedule.midnight", defaultValue = "0 0 23 ? * MON,WED *")
	public static String IDGEL_DOME_LANDMARK_SCHEDULE_MIDNIGHT;

	/**
	 * 坚韧殿堂计时器（分钟，5.3）。
	 * Hall of Tenacity timer in minutes (5.3).
	 */
	@Property(key = "gameserver.hall.of.tenacity.timer", defaultValue = "360")
	public static long HALL_OF_TENACITY_TIMER = 360L;
	/**
	 * 是否启用坚韧殿堂。
	 * Whether Hall of Tenacity is enabled.
	 */
	@Property(key = "gameserver.hall.of.tenacity.enable", defaultValue = "true")
	public static boolean HALL_OF_TENACITY_ENABLED;
	/**
	 * 坚韧殿堂上午开放 Cron。
	 * Hall of Tenacity morning open Cron schedule.
	 */
	@Property(key = "gameserver.hall.of.tenacity.schedule.morning", defaultValue = "0 0 9 ? * SAT,SUN *")
	public static String HALL_OF_TENACITY_SCHEDULE_MORNING;
	/**
	 * 坚韧殿堂晚间开放 Cron。
	 * Hall of Tenacity evening open Cron schedule.
	 */
	@Property(key = "gameserver.hall.of.tenacity.schedule.evening", defaultValue = "0 0 18 ? * MON,WED,TUE,THU,FRI, *")
	public static String HALL_OF_TENACITY_SCHEDULE_EVENING;

	/**
	 * 大竞技场训练营计时器（分钟，5.6）。
	 * Grand Arena Training Camp timer in minutes (5.6).
	 */
	@Property(key = "gameserver.grand.arena.training.camp.timer", defaultValue = "360")
	public static long GRAND_ARENA_TRAINING_CAMP_TIMER = 360L;
	/**
	 * 是否启用大竞技场训练营。
	 * Whether Grand Arena Training Camp is enabled.
	 */
	@Property(key = "gameserver.grand.arena.training.camp.enable", defaultValue = "true")
	public static boolean GRAND_ARENA_TRAINING_CAMP_ENABLED;
	/**
	 * 大竞技场训练营晚间开放 Cron。
	 * Grand Arena Training Camp evening open Cron schedule.
	 */
	@Property(key = "gameserver.grand.arena.training.camp.schedule.evening", defaultValue = "0 0 18 ? * SAT,SUN *")
	public static String GRAND_ARENA_TRAINING_CAMP_SCHEDULE_EVENING;

	/**
	 * IDTM_Lobby_P02 计时器（分钟，5.6）。
	 * IDTM_Lobby_P02 timer in minutes (5.6).
	 */
	@Property(key = "gameserver.IDTM_Lobby_P02.timer", defaultValue = "360")
	public static long IDTM_LOBBY_P02_TIMER = 360L;
	/**
	 * 是否启用 IDTM_Lobby_P02。
	 * Whether IDTM_Lobby_P02 is enabled.
	 */
	@Property(key = "gameserver.IDTM_Lobby_P02.enable", defaultValue = "true")
	public static boolean IDTM_LOBBY_P02_ENABLED;
	/**
	 * IDTM_Lobby_P02 晚间开放 Cron。
	 * IDTM_Lobby_P02 evening open Cron schedule.
	 */
	@Property(key = "gameserver.IDTM_Lobby_P02.schedule.evening", defaultValue = "0 0 18 ? * SAT,SUN *")
	public static String IDTM_LOBBY_P02_SCHEDULE_EVENING;

	/**
	 * IDTM_Lobby_E01 计时器（分钟，5.6）。
	 * IDTM_Lobby_E01 timer in minutes (5.6).
	 */
	@Property(key = "gameserver.IDTM_Lobby_E01.timer", defaultValue = "360")
	public static long IDTM_LOBBY_E01_TIMER = 360L;
	/**
	 * 是否启用 IDTM_Lobby_E01。
	 * Whether IDTM_Lobby_E01 is enabled.
	 */
	@Property(key = "gameserver.IDTM_Lobby_E01.enable", defaultValue = "true")
	public static boolean IDTM_LOBBY_E01_ENABLED;
	/**
	 * IDTM_Lobby_E01 晚间开放 Cron。
	 * IDTM_Lobby_E01 evening open Cron schedule.
	 */
	@Property(key = "gameserver.IDTM_Lobby_E01.schedule.evening", defaultValue = "0 0 18 ? * SAT,SUN *")
	public static String IDTM_LOBBY_E01_SCHEDULE_EVENING;

	/**
	 * IDRun 计时器（分钟，5.8）。
	 * IDRun timer in minutes (5.8).
	 */
	@Property(key = "gameserver.IDRun.timer", defaultValue = "60")
	public static long IDRUN_TIMER = 60L;
	/**
	 * 是否启用 IDRun。
	 * Whether IDRun is enabled.
	 */
	@Property(key = "gameserver.IDRun.enable", defaultValue = "true")
	public static boolean IDRUN_ENABLED;
	/**
	 * IDRun 开放 Cron。
	 * IDRun open Cron schedule.
	 */
	@Property(key = "gameserver.IDRun.schedule", defaultValue = "0 0 0/2 1/1 * ? *")
	public static String IDRUN_SCHEDULE;

	/** 自动组队绑定树 / auto-group binding tree. */
	@Getter
	private final AutoGroupProperties autoGroup = new AutoGroupProperties();
	/** 掘航舰绑定树 / dredgion binding tree. */
	@Getter
	private final DredgionProperties dredgion = new DredgionProperties();
	/** 卡玛战场绑定树 / kamar binding tree. */
	@Getter
	private final KamarProperties kamar = new KamarProperties();
	/** 淹没的奥菲丹桥绑定树 / ophidan binding tree. */
	@Getter
	private final OphidanProperties ophidan = new OphidanProperties();
	/** 铁壁战线绑定树 / bastion binding tree. */
	@Getter
	private final BastionProperties bastion = new BastionProperties();
	/** 伊吉尔穹顶绑定树 / idgel binding tree. */
	@Getter
	private final IdgelProperties idgel = new IdgelProperties();
	/** 阿修纳塔尔绑定树 / ashunatal binding tree. */
	@Getter
	private final AshunatalProperties ashunatal = new AshunatalProperties();
	/** 坚韧殿堂绑定树 / hall binding tree. */
	@Getter
	private final HallProperties hall = new HallProperties();
	/** 大竞技场绑定树：对应 gameserver.grand.* 四级元素 / grand binding tree: four elements for gameserver.grand.*. */
	@Getter
	private final GrandProperties grand = new GrandProperties();
	/** IDTM_Lobby_P02 绑定树 / IDTM_Lobby_P02 binding tree. */
	@Getter
	private final IdtmLobbyP02Properties IDTM_Lobby_P02 = new IdtmLobbyP02Properties();
	/** IDTM_Lobby_E01 绑定树 / IDTM_Lobby_E01 binding tree. */
	@Getter
	private final IdtmLobbyE01Properties IDTM_Lobby_E01 = new IdtmLobbyE01Properties();
	/** IDRun 绑定树 / IDRun binding tree. */
	@Getter
	private final IdRunProperties IDRun = new IdRunProperties();

	/**
	 * 自动组队节点：绑定 gameserver.autogroup.*。
	 * Auto-group node binding gameserver.autogroup.*.
	 */
	public static class AutoGroupProperties {
		public boolean isEnable() {
			return AUTO_GROUP_ENABLED;
		}

		public void setEnable(boolean enable) {
			AUTO_GROUP_ENABLED = enable;
		}
	}

	/**
	 * 掘航舰开放时刻节点：绑定 gameserver.dredgion.schedule.*。
	 * Dredgion schedule node binding gameserver.dredgion.schedule.*.
	 */
	public static class DredgionScheduleProperties {
		public String getMidday() {
			return DREDGION_SCHEDULE_MIDDAY;
		}

		public void setMidday(String midday) {
			DREDGION_SCHEDULE_MIDDAY = midday;
		}

		public String getEvening() {
			return DREDGION_SCHEDULE_EVENING;
		}

		public void setEvening(String evening) {
			DREDGION_SCHEDULE_EVENING = evening;
		}

		public String getMidnight() {
			return DREDGION_SCHEDULE_MIDNIGHT;
		}

		public void setMidnight(String midnight) {
			DREDGION_SCHEDULE_MIDNIGHT = midnight;
		}
	}

	/**
	 * 掘航舰节点：绑定 gameserver.dredgion.*。
	 * Dredgion node binding gameserver.dredgion.*.
	 */
	public static class DredgionProperties {
		private final DredgionScheduleProperties schedule = new DredgionScheduleProperties();

		public boolean isEnable() {
			return DREDGION_ENABLED;
		}

		public void setEnable(boolean enable) {
			DREDGION_ENABLED = enable;
		}

		public long getTimer() {
			return DREDGION_TIMER;
		}

		public void setTimer(long timer) {
			DREDGION_TIMER = timer;
		}

		public DredgionScheduleProperties getSchedule() {
			return schedule;
		}
	}

	/**
	 * 卡玛战场开放时刻节点：绑定 gameserver.kamar.schedule.*。
	 * Kamar schedule node binding gameserver.kamar.schedule.*.
	 */
	public static class KamarScheduleProperties {
		public String getMidnight() {
			return KAMAR_SCHEDULE_MIDNIGHT;
		}

		public void setMidnight(String midnight) {
			KAMAR_SCHEDULE_MIDNIGHT = midnight;
		}
	}

	/**
	 * 卡玛战场节点：绑定 gameserver.kamar.*。
	 * Kamar node binding gameserver.kamar.*.
	 */
	public static class KamarProperties {
		private final KamarScheduleProperties schedule = new KamarScheduleProperties();

		public boolean isEnable() {
			return KAMAR_ENABLED;
		}

		public void setEnable(boolean enable) {
			KAMAR_ENABLED = enable;
		}

		public long getTimer() {
			return KAMAR_TIMER;
		}

		public void setTimer(long timer) {
			KAMAR_TIMER = timer;
		}

		public KamarScheduleProperties getSchedule() {
			return schedule;
		}
	}

	/**
	 * 奥菲丹战径开放时刻节点：绑定 gameserver.ophidan.warpath.schedule.*。
	 * Ophidan Warpath schedule node binding gameserver.ophidan.warpath.schedule.*.
	 */
	public static class WarpathScheduleProperties {
		public String getMidnight() {
			return OPHIDAN_WARPATH_SCHEDULE_MIDNIGHT;
		}

		public void setMidnight(String midnight) {
			OPHIDAN_WARPATH_SCHEDULE_MIDNIGHT = midnight;
		}
	}

	/**
	 * 奥菲丹战径节点：绑定 gameserver.ophidan.warpath.*。
	 * Ophidan Warpath node binding gameserver.ophidan.warpath.*.
	 */
	public static class WarpathProperties {
		private final WarpathScheduleProperties schedule = new WarpathScheduleProperties();

		public boolean isEnable() {
			return OPHIDAN_WARPATH_ENABLED;
		}

		public void setEnable(boolean enable) {
			OPHIDAN_WARPATH_ENABLED = enable;
		}

		public long getTimer() {
			return OPHIDAN_WARPATH_TIMER;
		}

		public void setTimer(long timer) {
			OPHIDAN_WARPATH_TIMER = timer;
		}

		public WarpathScheduleProperties getSchedule() {
			return schedule;
		}
	}

	/**
	 * 淹没的奥菲丹桥开放时刻节点：绑定 gameserver.ophidan.schedule.*。
	 * Ophidan schedule node binding gameserver.ophidan.schedule.*.
	 */
	public static class OphidanScheduleProperties {
		public String getMidday() {
			return OPHIDAN_SCHEDULE_MIDDAY;
		}

		public void setMidday(String midday) {
			OPHIDAN_SCHEDULE_MIDDAY = midday;
		}

		public String getMidnight() {
			return OPHIDAN_SCHEDULE_MIDNIGHT;
		}

		public void setMidnight(String midnight) {
			OPHIDAN_SCHEDULE_MIDNIGHT = midnight;
		}
	}

	/**
	 * 淹没的奥菲丹桥节点：绑定 gameserver.ophidan.*（含战径子节点）。
	 * Ophidan node binding gameserver.ophidan.* (including the warpath child node).
	 */
	public static class OphidanProperties {
		private final OphidanScheduleProperties schedule = new OphidanScheduleProperties();
		private final WarpathProperties warpath = new WarpathProperties();

		public boolean isEnable() {
			return OPHIDAN_ENABLED;
		}

		public void setEnable(boolean enable) {
			OPHIDAN_ENABLED = enable;
		}

		public long getTimer() {
			return OPHIDAN_TIMER;
		}

		public void setTimer(long timer) {
			OPHIDAN_TIMER = timer;
		}

		public OphidanScheduleProperties getSchedule() {
			return schedule;
		}

		public WarpathProperties getWarpath() {
			return warpath;
		}
	}

	/**
	 * 铁壁战线开放时刻节点：绑定 gameserver.bastion.schedule.*。
	 * Bastion schedule node binding gameserver.bastion.schedule.*.
	 */
	public static class BastionScheduleProperties {
		public String getMidnight() {
			return BASTION_SCHEDULE_MIDNIGHT;
		}

		public void setMidnight(String midnight) {
			BASTION_SCHEDULE_MIDNIGHT = midnight;
		}
	}

	/**
	 * 铁壁战线节点：绑定 gameserver.bastion.*。
	 * Bastion node binding gameserver.bastion.*.
	 */
	public static class BastionProperties {
		private final BastionScheduleProperties schedule = new BastionScheduleProperties();

		public boolean isEnable() {
			return BASTION_ENABLED;
		}

		public void setEnable(boolean enable) {
			BASTION_ENABLED = enable;
		}

		public long getTimer() {
			return BASTION_TIMER;
		}

		public void setTimer(long timer) {
			BASTION_TIMER = timer;
		}

		public BastionScheduleProperties getSchedule() {
			return schedule;
		}
	}

	/**
	 * 伊吉尔穹顶地标开放时刻节点：绑定 gameserver.idgel.dome.landmark.schedule.*。
	 * Idgel Dome Landmark schedule node binding gameserver.idgel.dome.landmark.schedule.*.
	 */
	public static class LandmarkScheduleProperties {
		public String getMidnight() {
			return IDGEL_DOME_LANDMARK_SCHEDULE_MIDNIGHT;
		}

		public void setMidnight(String midnight) {
			IDGEL_DOME_LANDMARK_SCHEDULE_MIDNIGHT = midnight;
		}
	}

	/**
	 * 伊吉尔穹顶地标节点：绑定 gameserver.idgel.dome.landmark.*。
	 * Idgel Dome Landmark node binding gameserver.idgel.dome.landmark.*.
	 */
	public static class LandmarkProperties {
		private final LandmarkScheduleProperties schedule = new LandmarkScheduleProperties();

		public boolean isEnable() {
			return IDGEL_DOME_LANDMARK_ENABLED;
		}

		public void setEnable(boolean enable) {
			IDGEL_DOME_LANDMARK_ENABLED = enable;
		}

		public long getTimer() {
			return IDGEL_DOME_LANDMARK_TIMER;
		}

		public void setTimer(long timer) {
			IDGEL_DOME_LANDMARK_TIMER = timer;
		}

		public LandmarkScheduleProperties getSchedule() {
			return schedule;
		}
	}

	/**
	 * 伊吉尔穹顶开放时刻节点：绑定 gameserver.idgel.dome.schedule.*。
	 * Idgel Dome schedule node binding gameserver.idgel.dome.schedule.*.
	 */
	public static class DomeScheduleProperties {
		public String getMidday() {
			return IDGEL_SCHEDULE_MIDDAY;
		}

		public void setMidday(String midday) {
			IDGEL_SCHEDULE_MIDDAY = midday;
		}

		public String getMidnight() {
			return IDGEL_SCHEDULE_MIDNIGHT;
		}

		public void setMidnight(String midnight) {
			IDGEL_SCHEDULE_MIDNIGHT = midnight;
		}
	}

	/**
	 * 伊吉尔穹顶节点：绑定 gameserver.idgel.dome.*（含地标子节点）。
	 * Idgel Dome node binding gameserver.idgel.dome.* (including the landmark child node).
	 */
	public static class DomeProperties {
		private final DomeScheduleProperties schedule = new DomeScheduleProperties();
		private final LandmarkProperties landmark = new LandmarkProperties();

		public boolean isEnable() {
			return IDGEL_ENABLED;
		}

		public void setEnable(boolean enable) {
			IDGEL_ENABLED = enable;
		}

		public long getTimer() {
			return IDGEL_TIMER;
		}

		public void setTimer(long timer) {
			IDGEL_TIMER = timer;
		}

		public DomeScheduleProperties getSchedule() {
			return schedule;
		}

		public LandmarkProperties getLandmark() {
			return landmark;
		}
	}

	/**
	 * 伊吉尔域节点：绑定 gameserver.idgel.*。
	 * Idgel domain node binding gameserver.idgel.*.
	 */
	public static class IdgelProperties {
		private final DomeProperties dome = new DomeProperties();

		public DomeProperties getDome() {
			return dome;
		}
	}

	/**
	 * 阿修纳塔尔掘航舰子节点：绑定 gameserver.ashunatal.dredgion.*。
	 * Ashunatal dredgion child node binding gameserver.ashunatal.dredgion.*.
	 */
	public static class AshunatalDredgionProperties {
		public boolean isEnable() {
			return ASHUNATAL_ENABLED;
		}

		public void setEnable(boolean enable) {
			ASHUNATAL_ENABLED = enable;
		}

		public long getTimer() {
			return ASHUNATAL_TIMER;
		}

		public void setTimer(long timer) {
			ASHUNATAL_TIMER = timer;
		}
	}

	/**
	 * 阿修纳塔尔开放时刻节点：绑定 gameserver.ashunatal.schedule.*。
	 * Ashunatal schedule node binding gameserver.ashunatal.schedule.*.
	 */
	public static class AshunatalScheduleProperties {
		public String getMidday() {
			return ASHUNATAL_SCHEDULE_MIDDAY;
		}

		public void setMidday(String midday) {
			ASHUNATAL_SCHEDULE_MIDDAY = midday;
		}

		public String getEvening() {
			return ASHUNATAL_SCHEDULE_EVENING;
		}

		public void setEvening(String evening) {
			ASHUNATAL_SCHEDULE_EVENING = evening;
		}

		public String getMidnight() {
			return ASHUNATAL_SCHEDULE_MIDNIGHT;
		}

		public void setMidnight(String midnight) {
			ASHUNATAL_SCHEDULE_MIDNIGHT = midnight;
		}
	}

	/**
	 * 阿修纳塔尔节点：绑定 gameserver.ashunatal.*。
	 * Ashunatal node binding gameserver.ashunatal.*.
	 */
	public static class AshunatalProperties {
		private final AshunatalDredgionProperties dredgion = new AshunatalDredgionProperties();
		private final AshunatalScheduleProperties schedule = new AshunatalScheduleProperties();

		public AshunatalDredgionProperties getDredgion() {
			return dredgion;
		}

		public AshunatalScheduleProperties getSchedule() {
			return schedule;
		}
	}

	/**
	 * 坚韧殿堂开放时刻节点：绑定 gameserver.hall.of.tenacity.schedule.*。
	 * Hall of Tenacity schedule node binding gameserver.hall.of.tenacity.schedule.*.
	 */
	public static class TenacityScheduleProperties {
		public String getMorning() {
			return HALL_OF_TENACITY_SCHEDULE_MORNING;
		}

		public void setMorning(String morning) {
			HALL_OF_TENACITY_SCHEDULE_MORNING = morning;
		}

		public String getEvening() {
			return HALL_OF_TENACITY_SCHEDULE_EVENING;
		}

		public void setEvening(String evening) {
			HALL_OF_TENACITY_SCHEDULE_EVENING = evening;
		}
	}

	/**
	 * 坚韧殿堂节点：绑定 gameserver.hall.of.tenacity.*。
	 * Hall of Tenacity node binding gameserver.hall.of.tenacity.*.
	 */
	public static class TenacityProperties {
		private final TenacityScheduleProperties schedule = new TenacityScheduleProperties();

		public boolean isEnable() {
			return HALL_OF_TENACITY_ENABLED;
		}

		public void setEnable(boolean enable) {
			HALL_OF_TENACITY_ENABLED = enable;
		}

		public long getTimer() {
			return HALL_OF_TENACITY_TIMER;
		}

		public void setTimer(long timer) {
			HALL_OF_TENACITY_TIMER = timer;
		}

		public TenacityScheduleProperties getSchedule() {
			return schedule;
		}
	}

	/**
	 * 坚韧殿堂路径节点：绑定 gameserver.hall.of.*。
	 * Hall path node binding gameserver.hall.of.*.
	 */
	public static class OfProperties {
		private final TenacityProperties tenacity = new TenacityProperties();

		public TenacityProperties getTenacity() {
			return tenacity;
		}
	}

	/**
	 * 坚韧殿堂域节点：绑定 gameserver.hall.*。
	 * Hall domain node binding gameserver.hall.*.
	 */
	public static class HallProperties {
		private final OfProperties of = new OfProperties();

		public OfProperties getOf() {
			return of;
		}
	}

	/**
	 * 大竞技场训练营开放时刻节点：绑定 gameserver.grand.arena.training.camp.schedule.*。
	 * Grand Arena Training Camp schedule node binding gameserver.grand.arena.training.camp.schedule.*.
	 */
	public static class CampScheduleProperties {
		public String getEvening() {
			return GRAND_ARENA_TRAINING_CAMP_SCHEDULE_EVENING;
		}

		public void setEvening(String evening) {
			GRAND_ARENA_TRAINING_CAMP_SCHEDULE_EVENING = evening;
		}
	}

	/**
	 * 大竞技场训练营节点：绑定 gameserver.grand.arena.training.camp.*。
	 * Grand Arena Training Camp node binding gameserver.grand.arena.training.camp.*.
	 */
	public static class CampProperties {
		private final CampScheduleProperties schedule = new CampScheduleProperties();

		public boolean isEnable() {
			return GRAND_ARENA_TRAINING_CAMP_ENABLED;
		}

		public void setEnable(boolean enable) {
			GRAND_ARENA_TRAINING_CAMP_ENABLED = enable;
		}

		public long getTimer() {
			return GRAND_ARENA_TRAINING_CAMP_TIMER;
		}

		public void setTimer(long timer) {
			GRAND_ARENA_TRAINING_CAMP_TIMER = timer;
		}

		public CampScheduleProperties getSchedule() {
			return schedule;
		}
	}

	/**
	 * 大竞技场训练路径节点：绑定 gameserver.grand.arena.training.*。
	 * Grand Arena training path node binding gameserver.grand.arena.training.*.
	 */
	public static class TrainingProperties {
		private final CampProperties camp = new CampProperties();

		public CampProperties getCamp() {
			return camp;
		}
	}

	/**
	 * 大竞技场节点：绑定 gameserver.grand.arena.*。
	 * Grand Arena node binding gameserver.grand.arena.*.
	 */
	public static class GrandArenaProperties {
		private final TrainingProperties training = new TrainingProperties();

		public TrainingProperties getTraining() {
			return training;
		}
	}

	/**
	 * 大竞技场域节点：绑定 gameserver.grand.*。
	 * Grand domain node binding gameserver.grand.*.
	 */
	public static class GrandProperties {
		private final GrandArenaProperties arena = new GrandArenaProperties();

		public GrandArenaProperties getArena() {
			return arena;
		}
	}

	/**
	 * IDTM_Lobby_P02 开放时刻节点：绑定 gameserver.IDTM_Lobby_P02.schedule.*。
	 * IDTM_Lobby_P02 schedule node binding gameserver.IDTM_Lobby_P02.schedule.*.
	 */
	public static class IdtmLobbyP02ScheduleProperties {
		public String getEvening() {
			return IDTM_LOBBY_P02_SCHEDULE_EVENING;
		}

		public void setEvening(String evening) {
			IDTM_LOBBY_P02_SCHEDULE_EVENING = evening;
		}
	}

	/**
	 * IDTM_Lobby_P02 节点：绑定 gameserver.IDTM_Lobby_P02.*。
	 * IDTM_Lobby_P02 node binding gameserver.IDTM_Lobby_P02.*.
	 */
	public static class IdtmLobbyP02Properties {
		private final IdtmLobbyP02ScheduleProperties schedule = new IdtmLobbyP02ScheduleProperties();

		public boolean isEnable() {
			return IDTM_LOBBY_P02_ENABLED;
		}

		public void setEnable(boolean enable) {
			IDTM_LOBBY_P02_ENABLED = enable;
		}

		public long getTimer() {
			return IDTM_LOBBY_P02_TIMER;
		}

		public void setTimer(long timer) {
			IDTM_LOBBY_P02_TIMER = timer;
		}

		public IdtmLobbyP02ScheduleProperties getSchedule() {
			return schedule;
		}
	}

	/**
	 * IDTM_Lobby_E01 开放时刻节点：绑定 gameserver.IDTM_Lobby_E01.schedule.*。
	 * IDTM_Lobby_E01 schedule node binding gameserver.IDTM_Lobby_E01.schedule.*.
	 */
	public static class IdtmLobbyE01ScheduleProperties {
		public String getEvening() {
			return IDTM_LOBBY_E01_SCHEDULE_EVENING;
		}

		public void setEvening(String evening) {
			IDTM_LOBBY_E01_SCHEDULE_EVENING = evening;
		}
	}

	/**
	 * IDTM_Lobby_E01 节点：绑定 gameserver.IDTM_Lobby_E01.*。
	 * IDTM_Lobby_E01 node binding gameserver.IDTM_Lobby_E01.*.
	 */
	public static class IdtmLobbyE01Properties {
		private final IdtmLobbyE01ScheduleProperties schedule = new IdtmLobbyE01ScheduleProperties();

		public boolean isEnable() {
			return IDTM_LOBBY_E01_ENABLED;
		}

		public void setEnable(boolean enable) {
			IDTM_LOBBY_E01_ENABLED = enable;
		}

		public long getTimer() {
			return IDTM_LOBBY_E01_TIMER;
		}

		public void setTimer(long timer) {
			IDTM_LOBBY_E01_TIMER = timer;
		}

		public IdtmLobbyE01ScheduleProperties getSchedule() {
			return schedule;
		}
	}

	/**
	 * IDRun 节点：绑定 gameserver.IDRun.*。
	 * IDRun node binding gameserver.IDRun.*.
	 */
	public static class IdRunProperties {
		public boolean isEnable() {
			return IDRUN_ENABLED;
		}

		public void setEnable(boolean enable) {
			IDRUN_ENABLED = enable;
		}

		public long getTimer() {
			return IDRUN_TIMER;
		}

		public void setTimer(long timer) {
			IDRUN_TIMER = timer;
		}

		public String getSchedule() {
			return IDRUN_SCHEDULE;
		}

		public void setSchedule(String schedule) {
			IDRUN_SCHEDULE = schedule;
		}
	}
}
