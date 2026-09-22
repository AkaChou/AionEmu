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

/**
 * 游戏时间同步服务，定期向在线玩家推送游戏时间并持久化。
 * Game-time sync service that periodically pushes game time to online players and persists it.
 */
@Slf4j
public class GameTimeService {
	private static volatile ObjectProvider<GameTimeService> instanceProvider;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static final GameTimeService getInstance() {
		ObjectProvider<GameTimeService> provider = instanceProvider;
		GameTimeService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("GameTimeService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 * @param instanceProvider Spring provider
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
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
			Iterator<Player> iterator = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
			while (iterator.hasNext()) {
				Player next = iterator.next();
				PacketSendUtility.sendPacket(next, new SM_GAME_TIME());
			}
			GameTimeManager.saveTime();
		}, GAMETIME_UPDATE, GAMETIME_UPDATE);
		log.info(I18n.get("log.c03f3afa17b3", GAMETIME_UPDATE));
	}
}
