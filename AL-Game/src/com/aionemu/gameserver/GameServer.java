/**
 * This file is part of Encom.
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.NioServer;
import com.aionemu.commons.network.ServerCfg;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.commons.services.CronService;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.configs.main.FFAConfig;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.main.PvPModConfig;
import com.aionemu.gameserver.configs.main.RankingConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.configs.main.VeteranRewardConfig;
import com.aionemu.gameserver.configs.main.WeddingsConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.aion.GameConnectionFactoryImpl;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.services.AbyssLandingService;
import com.aionemu.gameserver.services.AbyssLandingSpecialService;
import com.aionemu.gameserver.services.AdminService;
import com.aionemu.gameserver.services.AgentService;
import com.aionemu.gameserver.services.AnnouncementService;
import com.aionemu.gameserver.services.AnohaService;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.BeritraService;
import com.aionemu.gameserver.services.BrokerService;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.ConquestService;
import com.aionemu.gameserver.services.CuringZoneService;
import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.DebugService;
import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.DynamicRiftService;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.ExchangeService;
import com.aionemu.gameserver.services.FlyRingService;
import com.aionemu.gameserver.services.GameTimeService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.IdianDepthsService;
import com.aionemu.gameserver.services.InstanceRiftService;
import com.aionemu.gameserver.services.IuService;
import com.aionemu.gameserver.services.LimitedItemTradeService;
import com.aionemu.gameserver.services.MoltenusService;
import com.aionemu.gameserver.services.NightmareCircusService;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.services.PeriodicSaveService;
import com.aionemu.gameserver.services.PetitionService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.RiftService;
import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.RvrService;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.SpringZoneService;
import com.aionemu.gameserver.services.SvsService;
import com.aionemu.gameserver.services.TowerOfEternityService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.services.VortexService;
import com.aionemu.gameserver.services.WeatherService;
import com.aionemu.gameserver.services.WeddingService;
import com.aionemu.gameserver.services.ZorshivDredgionService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.abysslandingservice.LandingUpdateService;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.BoostEventService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.events.PigPoppyEventService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.events.TreasureAbyssService;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.instance.EngulfedOphidanBridgeService;
import com.aionemu.gameserver.services.instance.GrandArenaTrainingCampService;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;
import com.aionemu.gameserver.services.instance.IDRunService;
import com.aionemu.gameserver.services.instance.IdgelDomeLandmarkService;
import com.aionemu.gameserver.services.instance.IdgelDomeService;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.instance.IronWallWarfrontService;
import com.aionemu.gameserver.services.instance.KamarBattlefieldService;
import com.aionemu.gameserver.services.instance.SuspiciousOphidanBridgeService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.TemporarySpawnEngine;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.utils.AEVersions;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.ThreadUncaughtExceptionHandler;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import com.aionemu.gameserver.utils.cron.ThreadPoolManagerRunnableRunner;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.utils.javaagent.JavaAgentUtils;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import com.aionemu.gameserver.world.zone.ZoneService;

import ch.lambdaj.Lambda;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * GameServer is the main class of the application and represents the whole game
 * server. This class is also an entry point with main() method.
 *
 * @author (Encom)
 */
public class GameServer {

	public static final Logger log = LoggerFactory.getLogger(GameServer.class);
	public static HashSet<String> npcs_count = new HashSet<String>();
	private static int ELYOS_COUNT = 0;
	private static int ASMOS_COUNT = 0;
	private static double ELYOS_RATIO = 0.0;
	private static double ASMOS_RATIO = 0.0;
	private static final ReentrantLock lock = new ReentrantLock(true);

	private static Set<StartupHook> startUpHooks = new HashSet<StartupHook>();

	/**
	 * 初始化日志系统，包括备份旧日志文件和配置新的日志记录器
	 * Initialize the logging system, including backing up old log files and configuring new loggers
	 */
	private static void initalizeLoggger() {
		File backupDir = new File("./log/backup/");
		if (!backupDir.exists() && !backupDir.mkdirs()) {
			System.err.println("Could not create backup directory");
		}
		
		File logDir = new File("./log/");
		File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
		
		if (logFiles != null && logFiles.length > 0) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HHmmss");
			String outFilename = "./log/backup/" + dateFormat.format(new Date()) + ".zip";
			
			try (FileOutputStream fos = new FileOutputStream(outFilename);
				 ZipOutputStream zos = new ZipOutputStream(fos)) {
				
				zos.setLevel(Deflater.BEST_SPEED);
				byte[] buffer = new byte[32768];
				
				for (File logFile : logFiles) {
					try (FileInputStream fis = new FileInputStream(logFile)) {
						ZipEntry entry = new ZipEntry(logFile.getName());
						zos.putNextEntry(entry);
						
						int length;
						while ((length = fis.read(buffer)) > 0) {
							zos.write(buffer, 0, length);
						}
						
						zos.closeEntry();
					} catch (IOException e) {
						System.err.println("Failed to backup log file: " + logFile.getName());
					}
					
					if (!logFile.delete()) {
						System.err.println("Could not delete log file: " + logFile.getName());
					}
				}
				
				System.out.println("Successfully backed up " + logFiles.length + " log files to " + outFilename);
				
			} catch (IOException e) {
				System.err.println("Error during log backup: " + e.getMessage());
			}
		}
		
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(lc);
			lc.reset();
			configurator.doConfigure("config/slf4j-logback.xml");
		} catch (JoranException je) {
			throw new RuntimeException("[LoggerFactory] Failed to configure loggers, shutting down...", je);
		}
	}

	/**
	 * Launching method for GameServer
	 */
	public static void main(String[] args) {
		start(args);
	}

	/**
	 * Starts GameServer from a standalone main method or embedding launcher.
	 */
	public static void start(String[] args) {
		start(args, null);
	}

	/**
	 * Starts GameServer with an optional chat-server connection override.
	 */
	public static void start(String[] args, Boolean chatServerEnabledOverride) {
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.net.preferIPv6Addresses", "false");
		
		long start = System.currentTimeMillis();
		log.info("GameServer starting...");

		enableLambdaJitting();
		final GameEngine[] parallelEngines = { 
			QuestEngine.getInstance(), 
			InstanceEngine.getInstance(),
			AI2Engine.getInstance(), 
			ChatProcessor.getInstance() 
		};

		final CountDownLatch progressLatch = new CountDownLatch(parallelEngines.length);
		initalizeLoggger();
		initUtilityServicesAndConfig();
		if (chatServerEnabledOverride != null) {
			GSConfig.ENABLE_CHAT_SERVER = chatServerEnabledOverride;
			log.info("Chat Server connection overridden by boot configuration: {}", chatServerEnabledOverride);
		}
		
		if (GSConfig.SERVER_YAADMINPANEL_SWITCH_ON) {
			(new ServerCommandProcessor()).startAdminPanel();
		}
		
		DataManager.getInstance();
		Util.printSection(" *** IDFactory *** ");
		IDFactory.getInstance();
		Util.printSection(" *** Zone *** ");
		ZoneService.getInstance().load(null);
		HotspotTeleportService.getInstance();
		RoadService.getInstance();
		World.getInstance();

		/**
		 * Event
		 */
		Util.printSection(" *** Luna Shop System *** ");
		LunaShopService.getInstance().init();
		Util.printSection(" *** Minion System *** ");
		MinionService.getInstance().init();
		Util.printSection(" *** Shugo Sweep System *** ");
		ShugoSweepService.getInstance().initShugoSweep();
		Util.printSection(" *** Atreian Passport System *** ");
		AtreianPassportService.getInstance().onStart();
		Util.printSection(" *** Event Window System *** ");
		EventWindowService.getInstance().initialize();

		/**
		 * GeoData
		 */
		Util.printSection(" *** Geodata *** ");
		GeoService.getInstance().initializeGeo();
		NavService.getInstance().initializeNav();
		DropRegistrationService.getInstance();
		GameServer gs = new GameServer();
		DAOManager.getDAO(PlayerDAO.class).setPlayersOffline(false);

		/**
		 * Engines
		 */
		Util.printSection(" *** Engines *** ");
		for (int i = 0; i < parallelEngines.length; i++) {
			final int index = i;
			ThreadPoolManager.getInstance().execute(new Runnable() {
				public void run() {
					parallelEngines[index].load(progressLatch);
				}
			});
		}
		try {
			progressLatch.await();
		} catch (InterruptedException e1) {
			log.warn("Main thread interrupted while waiting for engines", e1);
			Thread.currentThread().interrupt();
		}

		/**
		 * Location Data
		 */
		Util.printSection(" *** Siege & Battlefield Locations *** ");
		SiegeService.getInstance().initSiegeLocations();
		BaseService.getInstance().initBaseLocations();
		BaseService.getInstance().initBaseReset();
		OutpostService.getInstance().initOutpostLocations();
		OutpostService.getInstance().initOupostReset();
		
		Util.printSection(" *** Vortex & World Boss Locations *** ");
		VortexService.getInstance().initVortex();
		VortexService.getInstance().initVortexLocations();
		BeritraService.getInstance().initBeritra();
		BeritraService.getInstance().initBeritraLocations();
		AgentService.getInstance().initAgent();
		AgentService.getInstance().initAgentLocations();
		AnohaService.getInstance().initAnoha();
		AnohaService.getInstance().initAnohaLocations();
		SvsService.getInstance().initSvs();
		SvsService.getInstance().initSvsLocations();
		
		Util.printSection(" *** PvP & RvR Locations *** ");
		RvrService.getInstance().initRvr();
		RvrService.getInstance().initRvrLocations();
		IuService.getInstance().initConcert();
		IuService.getInstance().initConcertLocations();
		NightmareCircusService.getInstance().initCircus();
		NightmareCircusService.getInstance().initCircusLocations();
		DynamicRiftService.getInstance().initDynamicRift();
		DynamicRiftService.getInstance().initDynamicRiftLocations();
		
		Util.printSection(" *** Instance & Dungeon Locations *** ");
		InstanceRiftService.getInstance().initInstance();
		InstanceRiftService.getInstance().initInstanceLocations();
		ZorshivDredgionService.getInstance().initZorshivDredgion();
		ZorshivDredgionService.getInstance().initZorshivDredgionLocations();
		MoltenusService.getInstance().initMoltenus();
		MoltenusService.getInstance().initMoltenusLocations();
		RiftService.getInstance().initRifts();
		RiftService.getInstance().initRiftLocations();
		ConquestService.getInstance().initOffering();
		ConquestService.getInstance().initConquestLocations();
		IdianDepthsService.getInstance().initIdianDepths();
		IdianDepthsService.getInstance().initIdianDepthsLocations();
		TowerOfEternityService.getInstance().initTowerOfEternity();
		TowerOfEternityService.getInstance().initTowerOfEternityLocation();
		
		Util.printSection(" *** Abyss & Landing Locations *** ");
		AbyssLandingService.getInstance().initLandingLocations();
		LandingUpdateService.getInstance().initResetQuestPoints();
		LandingUpdateService.getInstance().initResetAbyssLandingPoints();
		AbyssLandingSpecialService.getInstance().initLandingSpecialLocations();

		/**
		 * Spawns
		 */
		Util.printSection(" *** Spawns *** ");
		SpawnEngine.spawnAll();
		
		// Events
		Util.printSection(" *** Events *** ");
		if (EventsConfig.ENABLE_EVENT_SERVICE) {
			EventService.getInstance().start();
		}
		if (EventsConfig.EVENT_ENABLED) {
			PlayerEventService.getInstance();
		}
		if (EventsConfig.ENABLE_CRAZY) {
			CrazyDaevaService.getInstance().startTimer();
		}
		if (RankingConfig.TOP_RANKING_UPDATE_SETTING) {
			AbyssRankUpdateService.getInstance().scheduleUpdateHour();
		} else {
			AbyssRankUpdateService.getInstance().scheduleUpdateMinute();
		}

		AbyssRankUpdateService.getInstance().initRewardWeeklyManager();
		
		PacketBroadcaster.getInstance();

		TemporarySpawnEngine.spawnAll();

		/**
		 * Cleaning
		 */
		DatabaseCleaningService.getInstance();
		AbyssRankCleaningService.getInstance();

		/**
		 * Scheduled Services
		 */
		Util.printSection(" *** Scheduled Services *** ");
		if (EventsConfig.ENABLE_PIG_POPPY_EVENT) {
			PigPoppyEventService.ScheduleCron();
		}
		if (EventsConfig.ENABLE_ABYSS_EVENT) {
			TreasureAbyssService.ScheduleCron();
		}
		if (EventsConfig.IMPERIAL_TOMB_ENABLE) {
			ShugoImperialTombSpawnManager.getInstance().start();
		}

		/**
		 * Custom Events
		 */
		Util.printSection(" *** Custom Events *** ");
		// FFA
		if (FFAConfig.FFA_ENABLED) {
			FFAService.getInstance();
		}
		if (PvPModConfig.BG_ENABLED) {
			LadderService.getInstance();
			BGService.getInstance();
		}
		BanditService.getInstance().onInit();

		/**
		 * Siege Schedule Initialization
		 */
		Util.printSection(" *** Sieges *** ");
		SiegeService.getInstance().initSieges();
		BaseService.getInstance().initBases();

		/**
		 * Dredgion
		 */
		Util.printSection(" *** Dredgion *** ");
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			DredgionService2.getInstance().initDredgion();
		}
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			AsyunatarService.getInstance().initAsyunatar();
		}

		/**
		 * Battlefield
		 */
		Util.printSection(" *** Battlefield *** ");
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			KamarBattlefieldService.getInstance().initKamarBattlefield();
		}
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			EngulfedOphidanBridgeService.getInstance().initEngulfedOphidan();
		}
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			SuspiciousOphidanBridgeService.getInstance().initSuspiciousOphidan();
		}
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			IronWallWarfrontService.getInstance().initIronWallWarfront();
		}
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			IdgelDomeService.getInstance().initIdgelDome();
		}
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			IdgelDomeLandmarkService.getInstance().initLandmark();
		}
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			HallOfTenacityService.getInstance().initHallOfTenacity();
		}
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			GrandArenaTrainingCampService.getInstance().initGrandArenaTrainingCamp();
		}
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			IDRunService.getInstance().initIDRun();
		}

		/**
		 * Protector/Conqueror
		 */
		Util.printSection(" *** Protector/Conqueror initialization *** ");
		ProtectorConquerorService.getInstance().initSystem();

		/**
		 * Dispute Land
		 */
		Util.printSection(" *** Dispute Land initialization *** ");
		DisputeLandService.getInstance().initDisputeLand();
		OutpostService.getInstance().initOutposts();

		/**
		 * HTML
		 */
		Util.printSection(" *** HTML *** ");
		HTMLCache.getInstance();

		if (CustomConfig.ENABLE_REWARD_SERVICE) {
			RewardService.getInstance();
		}
		if (WeddingsConfig.WEDDINGS_ENABLE) {
			WeddingService.getInstance();
		}
		if (VeteranRewardConfig.VETERANREWARDS_ENABLED) {
			VeteranRewardsService.getInstance();
		}
		/**
		 * Services
		 */
		Util.printSection(" *** Services *** ");
		PeriodicSaveService.getInstance();
		AdminService.getInstance();
		PlayerTransferService.getInstance();
		TerritoryService.getInstance().initTerritory();
		GameTimeService.getInstance();
		AnnouncementService.getInstance();
		DebugService.getInstance();
		WeatherService.getInstance();
		BrokerService.getInstance();
		Influence.getInstance();
		ExchangeService.getInstance();
		PetitionService.getInstance();
		InstanceService.load();
		FlyRingService.getInstance();
		CuringZoneService.getInstance();
		SpringZoneService.getInstance();
		BoostEventService.getInstance().onStart();
		TaskManagerFromDB.getInstance();
		LimitedItemTradeService.getInstance().start();
		GameTimeManager.startClock();

		if (CustomConfig.LIMITS_ENABLED) {
			PlayerLimitService.getInstance().scheduleUpdate();
		}
		if (AIConfig.SHOUTS_ENABLE) {
			NpcShoutsService.getInstance();
		}
		if (SiegeConfig.SIEGE_SHIELD_ENABLED) {
			ShieldService.getInstance().spawnAll();
		}

		/**
		 * Season Ranking Update
		 */
		Util.printSection(" *** Season Ranking *** ");
		SeasonRankingUpdateService.getInstance().onStart();

		/**
		 * Housing
		 */
		Util.printSection(" *** Housing *** ");
		HousingBidService.getInstance().start();
		MaintenanceTask.getInstance();
		TownService.getInstance();
		ChallengeTaskService.getInstance();

        /**
         * 系统初始化最终阶段
         * System initialization final phase
         */
        Util.printSection(" *** System *** ");
        AEVersions.printFullVersionInfo();
        AEInfos.printAllInfos();
        Util.printSection("GameServer");
        log.info("Power by Encom / Aion 5.8 Community Project");
        log.info("══════════════════════════════════════════════════════════");
        log.info(" █████  ██  ██████  ███    ██ ███████ ███    ███ ██    ██ ███████     █████");
        log.info("██   ██ ██ ██    ██ ████   ██ ██      ████  ████ ██    ██ ██         ██   ██");
        log.info("███████ ██ ██    ██ ██ ██  ██ █████   ██ ████ ██ ██    ██ ███████     █████");
        log.info("██   ██ ██ ██    ██ ██  ██ ██ ██      ██  ██  ██ ██    ██      ██    ██   ██");
        log.info("██   ██ ██  ██████  ██   ████ ███████ ██      ██  ██████  ███████ ██  █████");
        log.info("══════════════════════════════════════════════════════════");


        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        log.info("Memory Status After GC: Allocated={} MB, Free={} MB, Used={} MB, Max={} MB", totalMemory, freeMemory, usedMemory, maxMemory);
        
        long startupTime = (System.currentTimeMillis() - start) / 1000;
        log.info("Server startup completed in {} Seconds", startupTime);

        gs.startServers();
        Runtime.getRuntime().addShutdownHook(ShutdownHook.getInstance());
        
        if (GSConfig.ENABLE_RATIO_LIMITATION) {
            addStartupHook(new StartupHook() {
                @Override
                public void onStartup() {
                    lock.lock();
                    try {
                        long dbStart = System.currentTimeMillis();
                        ASMOS_COUNT = DAOManager.getDAO(PlayerDAO.class).getCharacterCountForRace(Race.ASMODIANS);
                        ELYOS_COUNT = DAOManager.getDAO(PlayerDAO.class).getCharacterCountForRace(Race.ELYOS);
                        long dbTime = System.currentTimeMillis() - dbStart;
                        log.debug("Database faction query took {} ms", dbTime);
                        computeRatios();
                    } catch (Exception e) {
                        log.error("Error loading faction ratios", e);
                    } finally {
                        lock.unlock();
                    }
                    displayRatios(false);
                }
            });
        }
        
        onStartup();
        
        log.info("=== Server initialization COMPLETE ===");
        log.info("Total initialization time: {} seconds", startupTime);
        log.info("Server is now ready to accept connections");
	}

	/**
	 * Starts servers for connection with aion client and login\chat server.
	 */
	private void startServers() {
		Util.printSection(" *** Network *** ");
		
		log.info("Network Config - Bind: {}, Port: {}, Threads: {}", NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, NetworkConfig.NIO_READ_WRITE_THREADS);
		
		boolean nettyTransportEnabled = Boolean.getBoolean("aion.transport.netty");
		NioServer nioServer = nettyTransportEnabled
			? new NioServer(NetworkConfig.NIO_READ_WRITE_THREADS)
			: new NioServer(NetworkConfig.NIO_READ_WRITE_THREADS, new ServerCfg(NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, "Game Connections", new GameConnectionFactoryImpl()));
		ServerTransport gameClientTransport = nettyTransportEnabled
			? new com.aionemu.commons.network.NettyServer(new NettyServerCfg(NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, "Game Connections", new GameConnectionFactoryImpl()))
			: nioServer;
		BannedMacManager.getInstance();

		LoginServer ls = LoginServer.getInstance();
		ChatServer cs = ChatServer.getInstance();

		ls.setNioServer(nioServer);
		cs.setNioServer(nioServer);

		long transportStart = System.currentTimeMillis();
		nioServer.connect();
		if (nettyTransportEnabled) {
			gameClientTransport.connect();
		}
		long transportTime = System.currentTimeMillis() - transportStart;
		log.info("{} server transport started in {} ms", nettyTransportEnabled ? "Netty" : "NIO", transportTime);
		
		System.out.println("");
		
		long lsStart = System.currentTimeMillis();
		ls.connect();
		long lsTime = System.currentTimeMillis() - lsStart;
		log.info("Login Server connected in {} ms", lsTime);

		if (GSConfig.ENABLE_CHAT_SERVER) {
			long csStart = System.currentTimeMillis();
			cs.connect();
			long csTime = System.currentTimeMillis() - csStart;
			log.info("Chat Server connected in {} ms", csTime);
		} else {
			log.info("Chat Server is disabled by configuration");
		}
		
		Util.printSection(" *** Misc *** ");
		log.info("All network servers started successfully");
	}

	/**
	 * Initialize all helper services, that are not directly related to aion gs,
	 * which includes:
	 */
	private static void initUtilityServicesAndConfig() {
		Thread.setDefaultUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler());
		
		if (JavaAgentUtils.isConfigured()) {
			log.info("JavaAgent [Callback Support] is configured.");
		} else {
			log.warn("JavaAgent [Callback Support] is NOT configured. Performance may be affected.");
		}
		
		CronService.initSingleton(ThreadPoolManagerRunnableRunner.class);
		Util.printSection(" *** Config *** ");
		
		long configStart = System.currentTimeMillis();
		Config.load();
		long configTime = System.currentTimeMillis() - configStart;
		log.info("Configuration loaded in {} ms", configTime);
		
		DateTimeUtil.init();
		
		Util.printSection(" *** DataBase *** ");
		long dbStart = System.currentTimeMillis();
		DatabaseFactory.init();
		long dbInitTime = System.currentTimeMillis() - dbStart;
		log.info("Database factory initialized in {} ms", dbInitTime);
		
		long daoStart = System.currentTimeMillis();
		DAOManager.init();
		long daoTime = System.currentTimeMillis() - daoStart;
		log.info("DAO Manager initialized in {} ms", daoTime);
		
		ThreadConfig.load();
		ThreadPoolManager.getInstance();
	}

	private static void enableLambdaJitting() {
		try {
			Method enableJitting = Lambda.class.getMethod("enableJitting", boolean.class);
			enableJitting.invoke(null, true);
		} catch (NoSuchMethodException e) {
			log.debug("lambdaj runtime does not expose enableJitting(boolean); skipping JIT configuration.");
		} catch (Exception e) {
			log.warn("Unable to configure lambdaj JIT support.", e);
		}
	}

	public synchronized static void addStartupHook(StartupHook hook) {
		if (startUpHooks != null) {
			startUpHooks.add(hook);
		} else {
			hook.onStartup();
		}
	}

	private synchronized static void onStartup() {
		final Set<StartupHook> startupHooks = startUpHooks;

		startUpHooks = null;

		if (startupHooks != null && !startupHooks.isEmpty()) {
			log.info("Executing {} startup hooks", startupHooks.size());
			long hooksStart = System.currentTimeMillis();
			
			for (StartupHook hook : startupHooks) {
				try {
					long hookStart = System.currentTimeMillis();
					hook.onStartup();
					long hookTime = System.currentTimeMillis() - hookStart;
					log.debug("Startup hook executed in {} ms", hookTime);
				} catch (Exception e) {
					log.error("Startup hook failed", e);
				}
			}
			
			long hooksTime = System.currentTimeMillis() - hooksStart;
			log.info("All startup hooks executed in {} ms", hooksTime);
		} else {
			log.info("No startup hooks to execute");
		}
	}

	public static void updateRatio(Race race, int i) {
		lock.lock();
		try {
			switch (race) {
			case ASMODIANS:
				ASMOS_COUNT += i;
				break;
			case ELYOS:
				ELYOS_COUNT += i;
				break;
			default:
				break;
			}
			computeRatios();

		} catch (Exception e) {
			log.error("[Error] Cant update ratio limits", e);
		} finally {
			lock.unlock();
		}

		displayRatios(true);
	}

	private static void computeRatios() {
		if ((ASMOS_COUNT <= GSConfig.RATIO_MIN_CHARACTERS_COUNT) && (ELYOS_COUNT <= GSConfig.RATIO_MIN_CHARACTERS_COUNT)) {
			ASMOS_RATIO = ELYOS_RATIO = 50.0;
		} else {
			ASMOS_RATIO = ASMOS_COUNT * 100.0 / (ASMOS_COUNT + ELYOS_COUNT);
			ELYOS_RATIO = ELYOS_COUNT * 100.0 / (ASMOS_COUNT + ELYOS_COUNT);
		}
	}

    private static void displayRatios(boolean updated) {
        String status = updated ? "updated" : "initialized";
        String totalPlayers = String.valueOf(ASMOS_COUNT + ELYOS_COUNT);
        
        if (log.isInfoEnabled()) {
            log.info("[Faction Balance] {} - Total Players: {}, Elyos: {}% ({}), Asmodians: {}% ({})", status, totalPlayers, String.format("%.2f", ELYOS_RATIO), ELYOS_COUNT, String.format("%.2f", ASMOS_RATIO), ASMOS_COUNT);
            
            double imbalance = Math.abs(ELYOS_RATIO - ASMOS_RATIO);
            if (imbalance > 20.0) {
                log.warn("Faction imbalance detected: {}% difference", String.format("%.1f", imbalance));
            }
        }
    }

	public static double getRatiosFor(Race race) {
		switch (race) {
		case ASMODIANS:
			return ASMOS_RATIO;
		case ELYOS:
			return ELYOS_RATIO;
		default:
			return 0.0;
		}
	}

	public static int getCountFor(Race race) {
		switch (race) {
		case ASMODIANS:
			return ASMOS_COUNT;
		case ELYOS:
			return ELYOS_COUNT;
		default:
			return 0;
		}
	}

	public static abstract interface StartupHook {
		public abstract void onStartup();
	}
}
