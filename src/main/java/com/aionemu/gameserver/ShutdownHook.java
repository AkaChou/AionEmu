package com.aionemu.gameserver;

import static com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SERVER_SHUTDOWN;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.services.CronService;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.commons.utils.concurrent.RunnableStatsManager;
import com.aionemu.commons.utils.concurrent.RunnableStatsManager.SortBy;
import com.aionemu.gameserver.configs.main.ShutdownConfig;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.services.PeriodicSaveService;
import com.aionemu.gameserver.services.player.PlayerLeaveWorldService;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.world.World;

/**
 * @author lord_rex
 */
public class ShutdownHook extends Thread {

	private static final Logger log = LoggerFactory.getLogger(ShutdownHook.class);

	public static ShutdownHook getInstance() {
		return SingletonHolder.INSTANCE;
	}

	@Override
	public void run() {
		if (ShutdownConfig.HOOK_MODE == 1) {
			doShutdown(ShutdownConfig.HOOK_DELAY, ShutdownConfig.ANNOUNCE_INTERVAL, ShutdownMode.SHUTDOWN);
		} else if (ShutdownConfig.HOOK_MODE == 2) {
			doShutdown(ShutdownConfig.HOOK_DELAY, ShutdownConfig.ANNOUNCE_INTERVAL, ShutdownMode.RESTART);
		}
	}

	public static enum ShutdownMode {
		NONE("terminating"), SHUTDOWN("shutting down"), RESTART("restarting");

		private final String text;

		private ShutdownMode(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}

	private void sendShutdownMessage(int seconds) {
		try {
			Iterator<Player> onlinePlayers = World.getInstance().getPlayersIterator();
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

	private void sendShutdownStatus(boolean status) {
		if (ShutdownConfig.DESPAWN_NPCS) {
			if (status) {
				for (Npc npc : World.getInstance().getNpcs()) {
					npc.getController().onDelete();
				}
			}
		}
		try {
			Iterator<Player> onlinePlayers = World.getInstance().getPlayersIterator();
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
	 * @param delay
	 * @param announceInterval
	 * @param mode
	 */
	public void doShutdown(int delay, int announceInterval, ShutdownMode mode) {
		log.info("Starting shutdown process with mode: {}, delay: {} seconds", mode.getText(), delay);
		
		for (int i = delay; i >= announceInterval; i -= announceInterval) {
			try {
				if (World.getInstance().getPlayersIterator().hasNext()) {
					log.info("Runtime is " + mode.getText() + " in " + i + " seconds.");
					sendShutdownMessage(i);
					sendShutdownStatus(ShutdownConfig.SAFE_REBOOT);
				} else {
					log.info("No players online, proceeding with shutdown...");
					break;
				}

				if (i > announceInterval) {
					Thread.sleep(announceInterval * 1000);
				} else {
					Thread.sleep(i * 1000);
				}
			} catch (InterruptedException e) {
				log.warn("Shutdown interrupted during announcement phase");
				Thread.currentThread().interrupt();
				return;
			}
		}

		if (AionRuntimeMode.isBootEmbedded()) {
			if (!AionEmbeddedShutdownHandler.requestShutdown(toEmbeddedMode(mode))) {
				log.warn("Embedded shutdown handler is not registered; stopping GameServer directly.");
				if (!GameServer.stop(mode)) {
					completeShutdown(mode, false);
				}
			}
			return;
		}

		completeShutdown(mode, true);
	}

	private AionEmbeddedShutdownMode toEmbeddedMode(ShutdownMode mode) {
		return mode == ShutdownMode.RESTART ? AionEmbeddedShutdownMode.RESTART : AionEmbeddedShutdownMode.SHUTDOWN;
	}

	public void completeShutdown(ShutdownMode mode, boolean haltRuntime) {
		log.info("Starting final shutdown sequence...");

		try {
			LoginServer.getInstance().gameServerDisconnected();
			log.info("Disconnected from Login Server");
		} catch (Exception e) {
			log.error("Error disconnecting from Login Server", e);
		}
		try {
			ChatServer.getInstance().gameServerDisconnected();
			log.info("Disconnected from Chat Server");
		} catch (Exception e) {
			log.error("Error disconnecting from Chat Server", e);
		}

		List<Player> playersToDisconnect = new ArrayList<>();
		Iterator<Player> onlinePlayers = World.getInstance().getPlayersIterator();
		while (onlinePlayers.hasNext()) {
			playersToDisconnect.add(onlinePlayers.next());
		}

		if (!playersToDisconnect.isEmpty()) {
			log.info("Found {} players online, starting disconnect process...", playersToDisconnect.size());
			
			int maxWaitTime = 30000;
			long startTime = System.currentTimeMillis();
			
			for (Player player : playersToDisconnect) {
				try {
					if (player != null && player.isOnline()) {
						log.info("Disconnecting player: {}", player.getName());
						PlayerLeaveWorldService.startLeaveWorld(player);
					}
				} catch (Exception e) {
					log.error("Error while disconnecting player " + (player != null ? player.getName() : "unknown"), e);
				}
				
				if (System.currentTimeMillis() - startTime > maxWaitTime) {
					log.warn("Player disconnect taking too long, forcing shutdown...");
					break;
				}
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					log.warn("Interrupted during player disconnect");
					Thread.currentThread().interrupt();
					break;
				}
			}
			
			try {
				log.info("Waiting for disconnect operations to complete...");
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.warn("Final wait interrupted");
				Thread.currentThread().interrupt();
			}
		} else {
			log.info("No players online to disconnect");
		}

		log.info("All players processed, continuing shutdown...");

		runShutdownStep("dump runnable stats", () -> RunnableStatsManager.dumpClassStats(SortBy.AVG));
		runShutdownStep("save periodic data", () -> PeriodicSaveService.getInstance().onShutdown());
		runShutdownStep("save game time", GameTimeManager::saveTime);
		runShutdownStep("shutdown CronService", () -> CronService.getInstance().shutdown());
		runShutdownStep("shutdown ThreadPoolManager", () -> ThreadPoolManager.getInstance().shutdown());
		log.info("All service shutdown steps completed");

		log.info("Runtime is " + mode.getText() + " now...");
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (!haltRuntime) {
			return;
		}
		if (mode == ShutdownMode.RESTART) {
			Runtime.getRuntime().halt(ExitCode.CODE_RESTART);
		} else {
			Runtime.getRuntime().halt(ExitCode.CODE_NORMAL);
		}
	}

	private void runShutdownStep(String name, Runnable step) {
		try {
			step.run();
		} catch (Exception e) {
			log.error("Error during shutdown step: {}", name, e);
		}
	}

	private static final class SingletonHolder {
		private static final ShutdownHook INSTANCE = new ShutdownHook();
	}
}
