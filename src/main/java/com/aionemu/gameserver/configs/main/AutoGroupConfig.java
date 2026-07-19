package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 自动组队与战场/副本入口相关配置。
 * Auto-group and battlefield/instance entry related configuration.
 */
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
	public static long DREDGION_TIMER;

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
	public static long KAMAR_TIMER;
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
	public static long OPHIDAN_TIMER;
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
	public static long BASTION_TIMER;
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
	public static long IDGEL_TIMER;
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
	public static long ASHUNATAL_TIMER;
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
	public static long OPHIDAN_WARPATH_TIMER;
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
	public static long IDGEL_DOME_LANDMARK_TIMER;
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
	 * 大竞技场训练营计时器（分钟，5.6）。
	 * Grand Arena Training Camp timer in minutes (5.6).
	 */
	@Property(key = "gameserver.grand.arena.training.camp.timer", defaultValue = "360")
	public static long GRAND_ARENA_TRAINING_CAMP_TIMER;
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
	public static long IDTM_LOBBY_P02_TIMER;
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
	public static long IDTM_LOBBY_E01_TIMER;
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
	public static long IDRUN_TIMER;
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
}
