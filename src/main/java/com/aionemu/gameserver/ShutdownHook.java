package com.aionemu.gameserver;

import com.aionemu.boot.i18n.I18n;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import static com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SERVER_SHUTDOWN;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionProcessExit;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.commons.utils.concurrent.RunnableStatsManager;
import com.aionemu.commons.utils.concurrent.RunnableStatsManager.SortBy;
import com.aionemu.gameserver.configs.main.ShutdownConfig;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.services.player.PlayerLeaveWorldService;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.world.World;

/**
 * JVM 关闭钩子：倒计时公告、踢线、落盘并按模式 halt/重启进程。
 * JVM shutdown hook: countdown announce, disconnect players, persist state and halt/restart by mode.
 *
 * @author lord_rex
 */
@Slf4j
public class ShutdownHook extends Thread {


	/**
	 * 获取关闭钩子单例。
	 * Returns the shutdown-hook singleton.
	 *
	 * @return 单例 / singleton
	 */
	public static ShutdownHook getInstance() {
		return SingletonHolder.INSTANCE;
	}

	/**
	 * 按配置 {@link ShutdownConfig#HOOK_MODE} 触发关服或重启流程。
	 * Triggers shutdown or restart according to {@link ShutdownConfig#HOOK_MODE}.
	 */
	@Override
	public void run() {
		if (ShutdownConfig.HOOK_MODE == 1) {
			doShutdown(ShutdownConfig.HOOK_DELAY, ShutdownConfig.ANNOUNCE_INTERVAL, ShutdownMode.SHUTDOWN);
		} else if (ShutdownConfig.HOOK_MODE == 2) {
			doShutdown(ShutdownConfig.HOOK_DELAY, ShutdownConfig.ANNOUNCE_INTERVAL, ShutdownMode.RESTART);
		}
	}

	/**
	 * 关闭模式：无操作、关服或重启。
	 * Shutdown mode: none, shut down, or restart.
	 */
	public static enum ShutdownMode {
		NONE("terminating"), SHUTDOWN("shutting down"), RESTART("restarting");

		/**
		 * 日志/文案用模式描述。
		 * Human-readable mode text for logs/messages.
		 */
		@Getter
		private final String text;

		private ShutdownMode(String text) {
			this.text = text;
		}
	}

	/**
	 * 向所有在线玩家发送关服倒计时系统消息。
	 * Sends shutdown countdown system message to all online players.
	 *
	 * @param seconds 剩余秒数 / remaining seconds
	 */
	private void sendShutdownMessage(int seconds) {
		try {
			Iterator<Player> onlinePlayers = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
			if (!onlinePlayers.hasNext()) {
				return;
			}
			while (onlinePlayers.hasNext()) {
				Player player = onlinePlayers.next();
				if (player != null && player.getClientConnection() != null) {
					player.getClientConnection().sendPacket(STR_SERVER_SHUTDOWN(String.valueOf(seconds)));
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * 标记玩家关服进行中，并可按配置删除全部 NPC。
	 * Marks players as in-shutdown and optionally despawns all NPCs when configured.
	 *
	 * @param status 是否进入关服流程 / whether shutdown is in progress
	 */
	private void sendShutdownStatus(boolean status) {
		if (ShutdownConfig.DESPAWN_NPCS) {
			if (status) {
				for (Npc npc : new ArrayList<Npc>(com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getNpcs())) {
					npc.getController().onDelete();
				}
			}
		}
		try {
			Iterator<Player> onlinePlayers = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
			if (!onlinePlayers.hasNext()) {
				return;
			}
			while (onlinePlayers.hasNext()) {
				Player player = onlinePlayers.next();
				if (player != null && player.getClientConnection() != null) {
					player.getController().setInShutdownProgress(status);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * 执行完整关闭流程：等待玩家下线后进入嵌入式或独立进程收尾。
	 * Runs full shutdown: wait for players to leave, then embedded or standalone teardown.
	 *
	 * @param delay 倒计时总秒数 / total delay seconds
	 * @param announceInterval 公告间隔秒数 / announce interval seconds
	 * @param mode 关闭模式 / shutdown mode
	 */
	public void doShutdown(int delay, int announceInterval, ShutdownMode mode) {
		if (!waitForPlayersToLeave(delay, announceInterval, mode)) {
			return;
		}

		if (AionRuntimeMode.isBootEmbedded()) {
			if (!AionEmbeddedShutdownHandler.requestShutdown(toEmbeddedMode(mode))) {
				log.warn(I18n.get("shutdown.embedded_handler_missing"));
				if (!GameServer.stop(mode)) {
					completeShutdown(mode, false);
				}
			}
			return;
		}

		completeShutdown(mode, true);
	}

	/**
	 * 倒计时公告并等待在线玩家离开；被中断时返回 false。
	 * Countdown-announce and wait for online players to leave; returns false if interrupted.
	 *
	 * @param delay 倒计时总秒数 / total delay seconds
	 * @param announceInterval 公告间隔秒数 / announce interval seconds
	 * @param mode 关闭模式 / shutdown mode
	 * @return 可继续关服为 true / {@code true} if shutdown may continue
	 */
	public boolean waitForPlayersToLeave(int delay, int announceInterval, ShutdownMode mode) {
		log.info(I18n.get("shutdown.process_start", mode.getText(), delay));

		for (int i = delay; i >= announceInterval; i -= announceInterval) {
			try {
				if (com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator().hasNext()) {
					log.info(I18n.get("shutdown.runtime_in_seconds", mode.getText(), i));
					sendShutdownMessage(i);
					sendShutdownStatus(ShutdownConfig.SAFE_REBOOT);
				} else {
					log.info(I18n.get("shutdown.no_players_proceed"));
					break;
				}

				if (i > announceInterval) {
					Thread.sleep(announceInterval * 1000);
				} else {
					Thread.sleep(i * 1000);
				}
			} catch (InterruptedException e) {
				log.warn(I18n.get("shutdown.announce_interrupted"));
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return true;
	}

	/**
	 * 将钩子模式映射为嵌入式关闭模式。
	 * Maps hook mode to embedded shutdown mode.
	 *
	 * @param mode 钩子模式 / hook mode
	 * @return 嵌入式模式 / embedded mode
	 */
	private AionEmbeddedShutdownMode toEmbeddedMode(ShutdownMode mode) {
		return mode == ShutdownMode.RESTART ? AionEmbeddedShutdownMode.RESTART : AionEmbeddedShutdownMode.SHUTDOWN;
	}

	/**
	 * 断开登录/聊天服、踢线、落盘并可选 halt 进程。
	 * Disconnects login/chat, kicks players, persists state and optionally halts the process.
	 *
	 * @param mode 关闭模式 / shutdown mode
	 * @param haltRuntime 是否强制终止当前进程 / whether to halt the current process
	 */
	public void completeShutdown(ShutdownMode mode, boolean haltRuntime) {
		log.info(I18n.get("shutdown.final_sequence"));

		try {
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().gameServerDisconnected();
			log.info(I18n.get("shutdown.disconnected_login"));
		} catch (Exception e) {
			log.error(I18n.get("shutdown.disconnect_login_error"), e);
		}
		try {
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.chatServer().gameServerDisconnected();
			log.info(I18n.get("shutdown.disconnected_chat"));
		} catch (Exception e) {
			log.error(I18n.get("shutdown.disconnect_chat_error"), e);
		}

		List<Player> playersToDisconnect = new ArrayList<>();
		Iterator<Player> onlinePlayers = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (onlinePlayers.hasNext()) {
			playersToDisconnect.add(onlinePlayers.next());
		}

		if (!playersToDisconnect.isEmpty()) {
			log.info(I18n.get("shutdown.players_found", playersToDisconnect.size()));

			int maxWaitTime = 30000;
			long startTime = System.currentTimeMillis();

			for (Player player : playersToDisconnect) {
				try {
					if (player != null && player.isOnline()) {
						log.info(I18n.get("shutdown.disconnecting_player", player.getName()));
						PlayerLeaveWorldService.startLeaveWorld(player);
					}
				} catch (Exception e) {
					log.error(I18n.get("shutdown.disconnect_player_error", player != null ? player.getName() : "unknown"), e);
				}

				if (System.currentTimeMillis() - startTime > maxWaitTime) {
					log.warn(I18n.get("shutdown.disconnect_timeout"));
					break;
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					log.warn(I18n.get("shutdown.disconnect_interrupted"));
					Thread.currentThread().interrupt();
					break;
				}
			}

			try {
				log.info(I18n.get("shutdown.wait_disconnect"));
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.warn(I18n.get("shutdown.final_wait_interrupted"));
				Thread.currentThread().interrupt();
			}
		} else {
			log.info(I18n.get("shutdown.no_players_disconnect"));
		}

		log.info(I18n.get("shutdown.players_processed"));

		runShutdownStep("dump runnable stats", () -> RunnableStatsManager.dumpClassStats(SortBy.AVG));
		runShutdownStep("save periodic data", () -> GameRuntimeServices.periodicSaveService().onShutdown());
		runShutdownStep("save game time", GameTimeManager::saveTime);
		runShutdownStep("shutdown CronService", GameCronServices::shutdownIfInitialized);
		if (AionRuntimeMode.isBootEmbedded()) {
			log.info(I18n.get("shutdown.threadpool_managed"));
		} else {
			runShutdownStep("shutdown ThreadPoolManager", () -> GameThreadPoolServices.threadPoolManager().shutdown());
		}
		log.info(I18n.get("shutdown.steps_completed"));

		log.info(I18n.get("shutdown.runtime_now", mode.getText()));

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (!haltRuntime) {
			return;
		}
		if (mode == ShutdownMode.RESTART) {
			AionProcessExit.halt(ExitCode.CODE_RESTART);
		} else {
			AionProcessExit.halt(ExitCode.CODE_NORMAL);
		}
	}

	/**
	 * 执行单步关服任务并吞掉异常记录日志。
	 * Runs one shutdown step and logs any thrown exception.
	 *
	 * @param name 步骤名 / step name
	 * @param step 步骤逻辑 / step logic
	 */
	private void runShutdownStep(String name, Runnable step) {
		try {
			step.run();
		} catch (Exception e) {
			log.error(I18n.get("shutdown.step_error", name), e);
		}
	}

	/**
	 * 单例持有者。
	 * Singleton holder.
	 */
	private static final class SingletonHolder {
		private static final ShutdownHook INSTANCE = new ShutdownHook();
	}
}
