package com.aionemu.gameserver;


import com.aionemu.boot.i18n.I18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.aionemu.gameserver.lifecycle.GameServerNetworkGateway;
import com.aionemu.gameserver.lifecycle.GameServerNetworkLifecycle;
import com.aionemu.gameserver.lifecycle.GameShutdownRequest;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.logging.slf4j.LogbackConfiguration;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.Race;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * 游戏服务器主入口与启动编排：日志、生命周期服务、阵营比例与关停。
 * Main GameServer entry and startup orchestration: logging, lifecycle services, faction ratios and stop.
 *
 * @author Encom
 */
@Slf4j
public class GameServer {

	/**
	 * 魔族角色计数 / Asmodian character count
	 */
	private static int ASMOS_COUNT = 0;
	/**
	 * 天族角色计数 / Elyos character count
	 */
	private static int ELYOS_COUNT = 0;
	/**
	 * 天族角色比例 / Elyos character ratio
	 */
	private static double ELYOS_RATIO = 0.0;
	/**
	 * 魔族角色比例 / Asmodian character ratio
	 */
	private static double ASMOS_RATIO = 0.0;
	private static final ReentrantLock lock = new ReentrantLock(true);

	private static Set<StartupHook> startUpHooks = new HashSet<>();
	private static volatile GameServer activeServer;

	private GameServerNetworkLifecycle networkLifecycle;

	/**
	 * 注册当前活动的 GameServer 实例。
	 * Registers the currently active GameServer instance.
	 *
	 * @param server 活动实例 / active server
	 */
	public static void activateServer(GameServer server) {
		activeServer = server;
	}

	/**
	 * 绑定网络生命周期，供启停网络使用。
	 * Attaches network lifecycle used to start/stop network services.
	 *
	 * @param networkLifecycle 网络生命周期 / network lifecycle
	 */
	public void attachNetworkLifecycle(GameServerNetworkLifecycle networkLifecycle) {
		this.networkLifecycle = networkLifecycle;
	}

	/**
	 * 初始化日志系统，包括备份旧日志文件和配置新的日志记录器
	 * Initialize the logging system, including backing up old log files and configuring new loggers
	 */
	private static void initalizeLoggger() {
		if (AionRuntimeMode.isBootEmbedded()) {
			return;
		}
		File backupDir = new File("./log/backup/");
		if (!backupDir.exists() && !backupDir.mkdirs()) {
			log.error(I18n.get("log.77147bf4cff7", backupDir.getAbsolutePath()));
		}

		File logDir = new File("./log/");
		File[] logFiles = logDir.listFiles((_, name) -> name.endsWith(".log"));

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
						log.error(I18n.get("log.c1a01a282f44", logFile.getName()), e);
					}

					if (!logFile.delete()) {
						log.error(I18n.get("log.f20ba663444a", logFile.getName()));
					}
				}

				log.info(I18n.get("log.038919cb0a3e", logFiles.length, outFilename));

			} catch (IOException e) {
				log.error(I18n.get("log.6f01b0cf500e", outFilename), e);
			}
		}

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
			LogbackConfiguration.configure(lc);
		} catch (JoranException je) {
			throw new RuntimeException("[LoggerFactory] Failed to configure loggers, shutting down...", je);
		}
	}

	/**
	 * 初始化日志（备份旧日志并配置 Logback）。
	 * Initializes logging (backs up old logs and configures Logback).
	 */
	public static void initializeLogger() {
		initalizeLoggger();
	}

	/**
	 * 启动与客户端、登录/聊天服相关的网络服务。
	 * Starts network services for client, login and chat servers.
	 */
	public void startServers() {
		networkLifecycle = new GameServerNetworkLifecycle(new GameServerNetworkGateway());
		networkLifecycle.start(this);
	}

	/**
	 * 以默认 {@link ShutdownHook.ShutdownMode#SHUTDOWN} 模式停止活动实例。
	 * Stops the active instance with default {@link ShutdownHook.ShutdownMode#SHUTDOWN}.
	 *
	 * @return 若存在活动实例并已处理返回 true / {@code true} if an active instance was stopped
	 */
	public static boolean stop() {
		return stop(ShutdownHook.ShutdownMode.SHUTDOWN);
	}

	/**
	 * 按指定模式停止活动实例网络并完成关服收尾（不 halt 进程）。
	 * Stops the active instance network and completes shutdown for the given mode (does not halt process).
	 *
	 * @param mode 关闭模式 / shutdown mode
	 * @return 若存在活动实例并已处理返回 true / {@code true} if an active instance was stopped
	 */
	public static boolean stop(ShutdownHook.ShutdownMode mode) {
		GameServer server = activeServer;
		if (server != null) {
			try {
				server.stopServers();
				GameShutdownRequest.completeShutdown(mode, false);
			} finally {
				activeServer = null;
			}
			return true;
		}
		return false;
	}

	/**
	 * 停止已绑定的网络生命周期。
	 * Stops the attached network lifecycle.
	 */
	private void stopServers() {
		GameServerNetworkLifecycle lifecycle = networkLifecycle;
		if (lifecycle != null) {
			lifecycle.stop();
			networkLifecycle = null;
		}
	}

	/**
	 * 注册启动钩子；若钩子阶段已结束则立即执行。
	 * Registers a startup hook; runs immediately if the hook phase already finished.
	 *
	 * @param hook 启动钩子 / startup hook
	 */
	public synchronized static void addStartupHook(StartupHook hook) {
		if (startUpHooks != null) {
			startUpHooks.add(hook);
		} else {
			hook.onStartup();
		}
	}

	/**
	 * 注册阵营角色数比例限制的启动钩子（从 DB 加载计数）。
	 * Registers the faction character-ratio startup hook (loads counts from DB).
	 */
	public static void registerRatioLimitStartupHook() {
		addStartupHook(() -> {
			lock.lock();
			try {
				long dbStart = System.currentTimeMillis();
				ASMOS_COUNT = DAOManager.getDAO(PlayerDAO.class).getCharacterCountForRace(Race.ASMODIANS);
				ELYOS_COUNT = DAOManager.getDAO(PlayerDAO.class).getCharacterCountForRace(Race.ELYOS);
				long dbTime = System.currentTimeMillis() - dbStart;
				log.debug("Database faction query took {} ms", dbTime);
				computeRatios();
			} catch (Exception e) {
				log.error(I18n.get("log.a690a349a611"), e);
			} finally {
				lock.unlock();
			}
			displayRatios(false);
		});
	}

	/**
	 * 执行并清空已注册的启动钩子。
	 * Runs and clears registered startup hooks.
	 */
	public static void runStartupHooks() {
		onStartup();
	}

	private synchronized static void onStartup() {
		final Set<StartupHook> startupHooks = startUpHooks;

		startUpHooks = null;

		if (startupHooks != null && !startupHooks.isEmpty()) {
			log.info(I18n.get("log.a5ddca4aaca5", startupHooks.size()));
			long hooksStart = System.currentTimeMillis();

			for (StartupHook hook : startupHooks) {
				try {
					long hookStart = System.currentTimeMillis();
					hook.onStartup();
					long hookTime = System.currentTimeMillis() - hookStart;
					log.debug("Startup hook executed in {} ms", hookTime);
				} catch (Exception e) {
					log.error(I18n.get("log.00fadfdcf59f"), e);
				}
			}

			long hooksTime = System.currentTimeMillis() - hooksStart;
			log.info(I18n.get("log.ca08b9810fb8", hooksTime));
		} else {
			log.info(I18n.get("log.01e9f647463e"));
		}
	}

	/**
	 * 按增量更新指定阵营角色计数并重算比例。
	 * Updates the character count for a race by delta and recomputes ratios.
	 *
	 * @param race 阵营 / race
	 * @param i    计数增量（可负） / count delta (may be negative)
	 */
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
			log.error(I18n.get("log.d8cc88ecc91c"), e);
		} finally {
			lock.unlock();
		}

		displayRatios(true);
	}

	/**
	 * 根据当前阵营角色数计算 Elyos/Asmodians 比例。
	 * Computes Elyos/Asmodians ratios from current race character counts.
	 */
	private static void computeRatios() {
		if ((ASMOS_COUNT <= GSConfig.RATIO_MIN_CHARACTERS_COUNT) && (ELYOS_COUNT <= GSConfig.RATIO_MIN_CHARACTERS_COUNT)) {
			ASMOS_RATIO = ELYOS_RATIO = 50.0;
		} else {
			ASMOS_RATIO = ASMOS_COUNT * 100.0 / (ASMOS_COUNT + ELYOS_COUNT);
			ELYOS_RATIO = ELYOS_COUNT * 100.0 / (ASMOS_COUNT + ELYOS_COUNT);
		}
	}

	/**
	 * 记录阵营比例状态；失衡超过 20% 时告警。
	 * Logs faction ratio status; warns when imbalance exceeds 20%.
	 *
	 * @param updated 是否为更新后日志 / whether this is a post-update log
	 */
	private static void displayRatios(boolean updated) {
		String status = updated ? "updated" : "initialized";
		String totalPlayers = String.valueOf(ASMOS_COUNT + ELYOS_COUNT);

		if (log.isInfoEnabled()) {
			log.info(I18n.get("log.c6bf79499f11", status, totalPlayers, String.format("%.2f", ELYOS_RATIO), ELYOS_COUNT, String.format("%.2f", ASMOS_RATIO), ASMOS_COUNT));

			double imbalance = Math.abs(ELYOS_RATIO - ASMOS_RATIO);
			if (imbalance > 20.0) {
				log.warn(I18n.get("log.dd2d045a1c76", String.format("%.1f", imbalance)));
			}
		}
	}

	/**
	 * 获取指定阵营的人数比例。
	 * Gets the population ratio for the specified race.
	 *
	 * @param race 种族 / race
	 * @return 阵营比例 / race ratio
	 */
	public static double getRatiosFor(Race race) {
		return race == Race.ASMODIANS ? ASMOS_RATIO : ELYOS_RATIO;
	}

	/**
	 * 获取指定阵营的角色数量。
	 * Gets the character count for the specified race.
	 *
	 * @param race 种族 / race
	 * @return 角色数量 / character count
	 */
	public static int getCountFor(Race race) {
		return race == Race.ASMODIANS ? ASMOS_COUNT : ELYOS_COUNT;
	}

	/**
	 * 启动完成后回调钩子。
	 * Callback hook invoked after startup completes.
	 */
	public interface StartupHook {
		/**
		 * 启动钩子回调。
		 * Startup hook callback.
		 */
		void onStartup();
	}
}
