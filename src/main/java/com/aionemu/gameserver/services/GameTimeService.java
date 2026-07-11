package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Iterator;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GAME_TIME;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.world.World;

/**
 * 游戏时间同步服务，定期向在线玩家推送游戏时间并持久化。
 * Game-time sync service that periodically pushes game time to online players and persists it.
 */
@Slf4j
public class GameTimeService {
	private static volatile ObjectProvider<GameTimeService> instanceProvider;

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final GameTimeService getInstance() {
		ObjectProvider<GameTimeService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<GameTimeService> instanceProvider) {
		GameTimeService.instanceProvider = instanceProvider;
	}

	/** 游戏时间广播间隔（毫秒）。 / Game-time broadcast interval in milliseconds. */
	private final static int GAMETIME_UPDATE = 3 * 60000;

	/**
	 * 启动定时任务：向所有在线玩家发送游戏时间并保存。
	 * Starts the scheduled task that sends game time to all online players and saves it.
	 */
	public GameTimeService() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				Iterator<Player> iterator = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
				while (iterator.hasNext()) {
					Player next = iterator.next();
					PacketSendUtility.sendPacket(next, new SM_GAME_TIME());
				}
				GameTimeManager.saveTime();
			}
		}, GAMETIME_UPDATE, GAMETIME_UPDATE);
		log.info(I18n.get("log.c03f3afa17b3", GAMETIME_UPDATE));
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final GameTimeService instance = new GameTimeService();
	}
}
