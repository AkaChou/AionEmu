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
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.lifecycle.GameAdminPanelLifecycle;
import com.aionemu.gameserver.lifecycle.GameBattlefieldGateway;
import com.aionemu.gameserver.lifecycle.GameBattlefieldLifecycle;
import com.aionemu.gameserver.lifecycle.GameChatServerOverrideLifecycle;
import com.aionemu.gameserver.lifecycle.GameCleaningGateway;
import com.aionemu.gameserver.lifecycle.GameCleaningLifecycle;
import com.aionemu.gameserver.lifecycle.GameCustomEventsGateway;
import com.aionemu.gameserver.lifecycle.GameCustomEventsLifecycle;
import com.aionemu.gameserver.lifecycle.GameDisputeLandGateway;
import com.aionemu.gameserver.lifecycle.GameDisputeLandLifecycle;
import com.aionemu.gameserver.lifecycle.GameDredgionGateway;
import com.aionemu.gameserver.lifecycle.GameDredgionLifecycle;
import com.aionemu.gameserver.lifecycle.GameEnginesGateway;
import com.aionemu.gameserver.lifecycle.GameEnginesLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventBootstrapGateway;
import com.aionemu.gameserver.lifecycle.GameEventBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventRuntimeGateway;
import com.aionemu.gameserver.lifecycle.GameEventRuntimeLifecycle;
import com.aionemu.gameserver.lifecycle.GameGeoNavGateway;
import com.aionemu.gameserver.lifecycle.GameGeoNavLifecycle;
import com.aionemu.gameserver.lifecycle.GameHtmlGateway;
import com.aionemu.gameserver.lifecycle.GameHtmlLifecycle;
import com.aionemu.gameserver.lifecycle.GameHousingLifecycle;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapGateway;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameLoggingLifecycle;
import com.aionemu.gameserver.lifecycle.GameNetworkStartupLifecycle;
import com.aionemu.gameserver.lifecycle.GameOptionalServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameProtectorConquerorGateway;
import com.aionemu.gameserver.lifecycle.GameProtectorConquerorLifecycle;
import com.aionemu.gameserver.lifecycle.GameRatioLimitLifecycle;
import com.aionemu.gameserver.lifecycle.GameRewardServicesGateway;
import com.aionemu.gameserver.lifecycle.GameRewardServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameRuntimeServicesGateway;
import com.aionemu.gameserver.lifecycle.GameRuntimeServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSeasonRankingLifecycle;
import com.aionemu.gameserver.lifecycle.GameServerNetworkLifecycle;
import com.aionemu.gameserver.lifecycle.GameScheduledServicesGateway;
import com.aionemu.gameserver.lifecycle.GameScheduledServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSiegeScheduleGateway;
import com.aionemu.gameserver.lifecycle.GameSiegeScheduleLifecycle;
import com.aionemu.gameserver.lifecycle.GameSpawnGateway;
import com.aionemu.gameserver.lifecycle.GameSpawnLifecycle;
import com.aionemu.gameserver.lifecycle.GameStaticDataGateway;
import com.aionemu.gameserver.lifecycle.GameStaticDataLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupCompletionLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupHooksLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupLogLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupSequenceLifecycle;
import com.aionemu.gameserver.lifecycle.GameSystemLifecycle;
import com.aionemu.gameserver.lifecycle.GameSystemPropertiesLifecycle;
import com.aionemu.gameserver.lifecycle.GameThreadPoolLifecycle;
import com.aionemu.gameserver.lifecycle.GameUtilityServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldActivationGateway;
import com.aionemu.gameserver.lifecycle.GameWorldActivationLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapGateway;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapLifecycle;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.Util;

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
	private static volatile GameServer activeServer;

	private GameServerNetworkLifecycle networkLifecycle;

	public static void activateServer(GameServer server) {
		activeServer = server;
	}

	public void attachNetworkLifecycle(GameServerNetworkLifecycle networkLifecycle) {
		this.networkLifecycle = networkLifecycle;
	}

	private static String configDir() {
		return System.getProperty("aion.game.config.dir", "./config");
	}

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
			configurator.doConfigure(configDir() + "/logback-spring.xml");
		} catch (JoranException je) {
			throw new RuntimeException("[LoggerFactory] Failed to configure loggers, shutting down...", je);
		}
	}

	public static void initializeLogger() {
		initalizeLoggger();
	}

	/**
	 * Starts GameServer from the boot-managed service lifecycle.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(String[] args) {
		start(args, null);
	}

	/**
	 * Starts GameServer with an optional chat-server connection override.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(String[] args, Boolean chatServerEnabledOverride) {
		start(args, chatServerEnabledOverride, new GameThreadPoolLifecycle());
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle
	) {
		start(args, chatServerEnabledOverride, threadPoolLifecycle, new GameStaticDataLifecycle(new GameStaticDataGateway()));
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle
	) {
		start(args, chatServerEnabledOverride, threadPoolLifecycle, staticDataLifecycle, new GameWorldBootstrapLifecycle(new GameWorldBootstrapGateway()));
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle
	) {
		start(args, chatServerEnabledOverride, threadPoolLifecycle, staticDataLifecycle, worldBootstrapLifecycle,
			new GameEventBootstrapLifecycle(new GameEventBootstrapGateway()));
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			new GameGeoNavLifecycle(new GameGeoNavGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			new GameWorldActivationLifecycle(new GameWorldActivationGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			new GameEnginesLifecycle(new GameEnginesGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			new GameLocationBootstrapLifecycle(new GameLocationBootstrapGateway()),
			new GameSpawnLifecycle(new GameSpawnGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			new GameEventRuntimeLifecycle(new GameEventRuntimeGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			new GameCleaningLifecycle(new GameCleaningGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			new GameScheduledServicesLifecycle(new GameScheduledServicesGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			new GameCustomEventsLifecycle(new GameCustomEventsGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			new GameSiegeScheduleLifecycle(new GameSiegeScheduleGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			new GameDredgionLifecycle(new GameDredgionGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			new GameBattlefieldLifecycle(new GameBattlefieldGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			new GameProtectorConquerorLifecycle(new GameProtectorConquerorGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle,
		GameProtectorConquerorLifecycle protectorConquerorLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			protectorConquerorLifecycle,
			new GameDisputeLandLifecycle(new GameDisputeLandGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle,
		GameProtectorConquerorLifecycle protectorConquerorLifecycle,
		GameDisputeLandLifecycle disputeLandLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			protectorConquerorLifecycle,
			disputeLandLifecycle,
			new GameHtmlLifecycle(new GameHtmlGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle,
		GameProtectorConquerorLifecycle protectorConquerorLifecycle,
		GameDisputeLandLifecycle disputeLandLifecycle,
		GameHtmlLifecycle htmlLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			protectorConquerorLifecycle,
			disputeLandLifecycle,
			htmlLifecycle,
			new GameRewardServicesLifecycle(new GameRewardServicesGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle,
		GameProtectorConquerorLifecycle protectorConquerorLifecycle,
		GameDisputeLandLifecycle disputeLandLifecycle,
		GameHtmlLifecycle htmlLifecycle,
		GameRewardServicesLifecycle rewardServicesLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			protectorConquerorLifecycle,
			disputeLandLifecycle,
			htmlLifecycle,
			rewardServicesLifecycle,
			new GameRuntimeServicesLifecycle(new GameRuntimeServicesGateway())
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle,
		GameProtectorConquerorLifecycle protectorConquerorLifecycle,
		GameDisputeLandLifecycle disputeLandLifecycle,
		GameHtmlLifecycle htmlLifecycle,
		GameRewardServicesLifecycle rewardServicesLifecycle,
		GameRuntimeServicesLifecycle runtimeServicesLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			protectorConquerorLifecycle,
			disputeLandLifecycle,
			htmlLifecycle,
			rewardServicesLifecycle,
			runtimeServicesLifecycle,
			new GameOptionalServicesLifecycle()
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle,
		GameProtectorConquerorLifecycle protectorConquerorLifecycle,
		GameDisputeLandLifecycle disputeLandLifecycle,
		GameHtmlLifecycle htmlLifecycle,
		GameRewardServicesLifecycle rewardServicesLifecycle,
		GameRuntimeServicesLifecycle runtimeServicesLifecycle,
		GameOptionalServicesLifecycle optionalServicesLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			protectorConquerorLifecycle,
			disputeLandLifecycle,
			htmlLifecycle,
			rewardServicesLifecycle,
			runtimeServicesLifecycle,
			optionalServicesLifecycle,
			new GameSeasonRankingLifecycle()
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle,
		GameProtectorConquerorLifecycle protectorConquerorLifecycle,
		GameDisputeLandLifecycle disputeLandLifecycle,
		GameHtmlLifecycle htmlLifecycle,
		GameRewardServicesLifecycle rewardServicesLifecycle,
		GameRuntimeServicesLifecycle runtimeServicesLifecycle,
		GameOptionalServicesLifecycle optionalServicesLifecycle,
		GameSeasonRankingLifecycle seasonRankingLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			protectorConquerorLifecycle,
			disputeLandLifecycle,
			htmlLifecycle,
			rewardServicesLifecycle,
			runtimeServicesLifecycle,
			optionalServicesLifecycle,
			seasonRankingLifecycle,
			new GameHousingLifecycle()
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle,
		GameProtectorConquerorLifecycle protectorConquerorLifecycle,
		GameDisputeLandLifecycle disputeLandLifecycle,
		GameHtmlLifecycle htmlLifecycle,
		GameRewardServicesLifecycle rewardServicesLifecycle,
		GameRuntimeServicesLifecycle runtimeServicesLifecycle,
		GameOptionalServicesLifecycle optionalServicesLifecycle,
		GameSeasonRankingLifecycle seasonRankingLifecycle,
		GameHousingLifecycle housingLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			protectorConquerorLifecycle,
			disputeLandLifecycle,
			htmlLifecycle,
			rewardServicesLifecycle,
			runtimeServicesLifecycle,
			optionalServicesLifecycle,
			seasonRankingLifecycle,
			housingLifecycle,
			new GameSystemLifecycle()
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle,
		GameProtectorConquerorLifecycle protectorConquerorLifecycle,
		GameDisputeLandLifecycle disputeLandLifecycle,
		GameHtmlLifecycle htmlLifecycle,
		GameRewardServicesLifecycle rewardServicesLifecycle,
		GameRuntimeServicesLifecycle runtimeServicesLifecycle,
		GameOptionalServicesLifecycle optionalServicesLifecycle,
		GameSeasonRankingLifecycle seasonRankingLifecycle,
		GameHousingLifecycle housingLifecycle,
		GameSystemLifecycle systemLifecycle
	) {
		start(
			args,
			chatServerEnabledOverride,
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			protectorConquerorLifecycle,
			disputeLandLifecycle,
			htmlLifecycle,
			rewardServicesLifecycle,
			runtimeServicesLifecycle,
			optionalServicesLifecycle,
			seasonRankingLifecycle,
			housingLifecycle,
			systemLifecycle,
			new GameServerNetworkLifecycle(),
			new GameNetworkStartupLifecycle(),
			new GameRatioLimitLifecycle(),
			new GameStartupHooksLifecycle(),
			new GameStartupCompletionLifecycle(),
			new GameLoggingLifecycle(),
			new GameUtilityServicesLifecycle(),
			new GameAdminPanelLifecycle(),
			new GameSystemPropertiesLifecycle(),
			new GameStartupLogLifecycle(),
			new GameChatServerOverrideLifecycle()
		);
	}

	/**
	 * Starts GameServer with Spring-managed game runtime resources when boot embedded.
	 */
	@Deprecated(since = "1.0", forRemoval = false)
	public static void start(
		String[] args,
		Boolean chatServerEnabledOverride,
		GameThreadPoolLifecycle threadPoolLifecycle,
		GameStaticDataLifecycle staticDataLifecycle,
		GameWorldBootstrapLifecycle worldBootstrapLifecycle,
		GameEventBootstrapLifecycle eventBootstrapLifecycle,
		GameGeoNavLifecycle geoNavLifecycle,
		GameWorldActivationLifecycle worldActivationLifecycle,
		GameEnginesLifecycle enginesLifecycle,
		GameLocationBootstrapLifecycle locationBootstrapLifecycle,
		GameSpawnLifecycle spawnLifecycle,
		GameEventRuntimeLifecycle eventRuntimeLifecycle,
		GameCleaningLifecycle cleaningLifecycle,
		GameScheduledServicesLifecycle scheduledServicesLifecycle,
		GameCustomEventsLifecycle customEventsLifecycle,
		GameSiegeScheduleLifecycle siegeScheduleLifecycle,
		GameDredgionLifecycle dredgionLifecycle,
		GameBattlefieldLifecycle battlefieldLifecycle,
		GameProtectorConquerorLifecycle protectorConquerorLifecycle,
		GameDisputeLandLifecycle disputeLandLifecycle,
		GameHtmlLifecycle htmlLifecycle,
		GameRewardServicesLifecycle rewardServicesLifecycle,
		GameRuntimeServicesLifecycle runtimeServicesLifecycle,
		GameOptionalServicesLifecycle optionalServicesLifecycle,
		GameSeasonRankingLifecycle seasonRankingLifecycle,
		GameHousingLifecycle housingLifecycle,
		GameSystemLifecycle systemLifecycle,
		GameServerNetworkLifecycle serverNetworkLifecycle,
		GameNetworkStartupLifecycle networkStartupLifecycle,
		GameRatioLimitLifecycle ratioLimitLifecycle,
		GameStartupHooksLifecycle startupHooksLifecycle,
		GameStartupCompletionLifecycle startupCompletionLifecycle,
		GameLoggingLifecycle loggingLifecycle,
		GameUtilityServicesLifecycle utilityServicesLifecycle,
		GameAdminPanelLifecycle adminPanelLifecycle,
		GameSystemPropertiesLifecycle systemPropertiesLifecycle,
		GameStartupLogLifecycle startupLogLifecycle,
		GameChatServerOverrideLifecycle chatServerOverrideLifecycle
	) {
		new GameStartupSequenceLifecycle(
			threadPoolLifecycle,
			staticDataLifecycle,
			worldBootstrapLifecycle,
			eventBootstrapLifecycle,
			geoNavLifecycle,
			worldActivationLifecycle,
			enginesLifecycle,
			locationBootstrapLifecycle,
			spawnLifecycle,
			eventRuntimeLifecycle,
			cleaningLifecycle,
			scheduledServicesLifecycle,
			customEventsLifecycle,
			siegeScheduleLifecycle,
			dredgionLifecycle,
			battlefieldLifecycle,
			protectorConquerorLifecycle,
			disputeLandLifecycle,
			htmlLifecycle,
			rewardServicesLifecycle,
			runtimeServicesLifecycle,
			optionalServicesLifecycle,
			seasonRankingLifecycle,
			housingLifecycle,
			systemLifecycle,
			serverNetworkLifecycle,
			networkStartupLifecycle,
			ratioLimitLifecycle,
			startupHooksLifecycle,
			startupCompletionLifecycle,
			loggingLifecycle,
			utilityServicesLifecycle,
			adminPanelLifecycle,
			systemPropertiesLifecycle,
			startupLogLifecycle,
			chatServerOverrideLifecycle
		).start(chatServerEnabledOverride);
	}

	/**
	 * Starts servers for connection with aion client and login\chat server.
	 */
	public void startServers() {
		networkLifecycle = new GameServerNetworkLifecycle();
		networkLifecycle.start(this);
	}

	public static boolean stop() {
		return stop(ShutdownHook.ShutdownMode.SHUTDOWN);
	}

	public static boolean stop(ShutdownHook.ShutdownMode mode) {
		GameServer server = activeServer;
		if (server != null) {
			try {
				server.stopServers();
				ShutdownHook.getInstance().completeShutdown(mode, false);
			} finally {
				activeServer = null;
			}
			return true;
		}
		return false;
	}

	private void stopServers() {
		GameServerNetworkLifecycle lifecycle = networkLifecycle;
		if (lifecycle != null) {
			lifecycle.stop();
			networkLifecycle = null;
		}
	}

	/**
	 * Initialize all helper services, that are not directly related to aion gs,
	 * which includes:
	 */
	private static void initUtilityServicesAndConfig(GameThreadPoolLifecycle threadPoolLifecycle) {
		new GameUtilityServicesLifecycle().start(threadPoolLifecycle);
	}

	public static void initializeUtilityServicesAndConfig(GameThreadPoolLifecycle threadPoolLifecycle) {
		initUtilityServicesAndConfig(threadPoolLifecycle);
	}

	public synchronized static void addStartupHook(StartupHook hook) {
		if (startUpHooks != null) {
			startUpHooks.add(hook);
		} else {
			hook.onStartup();
		}
	}

	public static void registerRatioLimitStartupHook() {
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

	public static void runStartupHooks() {
		onStartup();
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
