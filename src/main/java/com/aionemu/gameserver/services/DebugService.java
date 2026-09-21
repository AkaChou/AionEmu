package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Iterator;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import lombok.extern.slf4j.Slf4j;

/**
 * 调试诊断服务，周期性检查在线玩家连接与心跳异常。
 * Debug diagnostics service that periodically checks online players for connection and ping anomalies.
 *
 * @author ATracer
 */
@Slf4j
public class DebugService {

	private static volatile ObjectProvider<DebugService> instanceProvider;

	/** 玩家分析间隔（毫秒）。 / Player analysis interval in milliseconds. */
	private static final int ANALYZE_PLAYERS_INTERVAL = 30 * 60 * 1000;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static final DebugService getInstance() {
		ObjectProvider<DebugService> provider = instanceProvider;
		DebugService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("DebugService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<DebugService> instanceProvider) {
		DebugService.instanceProvider = instanceProvider;
	}

	/**
	 * 启动周期性玩家健康检查任务。
	 * Starts the periodic player-health check task.
	 */
	public DebugService() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				analyzeWorldPlayers();
			}

		}, ANALYZE_PLAYERS_INTERVAL, ANALYZE_PLAYERS_INTERVAL);
		log.info(I18n.get("log.c0807adf5cd7", ANALYZE_PLAYERS_INTERVAL));
	}

	/**
	 * 扫描在线玩家，记录无连接或心跳超时的异常。
	 * Scans online players and logs missing connections or oversized ping intervals.
	 */
	private void analyzeWorldPlayers() {
		log.info(I18n.get("log.25e39c719d4a", System.currentTimeMillis()));

		Iterator<Player> playersIterator = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (playersIterator.hasNext()) {
			Player player = playersIterator.next();

			/**
	 * 检查连接。 / Check connection
	 */
			AionConnection connection = player.getClientConnection();
			if (connection == null) {
				log.warn(I18n.get("log.882f2866065c", player.getObjectId(), player.getName(), player.isSpawned()));
				continue;
			}

			/**
	 * 检查 CM_PING 数据包。 / Check CM_PING packet
	 */
			long lastPingTimeMS = connection.getLastPingTimeMS();
			long pingInterval = System.currentTimeMillis() - lastPingTimeMS;
			if (lastPingTimeMS > 0 && pingInterval > 300000) {
				log.warn(I18n.get("log.af56f8905635", player.getObjectId(), player.getName(), player.isSpawned(), pingInterval));
			}
		}
	}
}
