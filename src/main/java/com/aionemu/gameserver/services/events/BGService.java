package com.aionemu.gameserver.services.events;


import com.aionemu.boot.i18n.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.lifecycle.GameEventServices;
import com.aionemu.gameserver.eventEngine.events.BattlegroundEvent;

/**
 * 战场定时注册服务，按计划开启战场匹配入口。
 * Battleground schedule service that opens matchmaking windows on schedule.
 *
 * @author Rinzler (Encom)
 */

@Slf4j(topic = "com.aionemu.gameserver.services.EventService")
public class BGService {
	private static volatile ObjectProvider<BGService> instanceProvider;
	private static final int DELAY = 60 * 100;
	private final List<ScheduledFuture<?>> futures = new ArrayList<ScheduledFuture<?>>();

	public BGService() {
		register(DELAY);
		log.info(I18n.get("log.e0322d9a5480"));
	}

	/**
	 * 注册调度任务。
	 * Registers scheduled tasks.
	 *
	 * @param delay 延迟毫秒 / delay
	 */
	public void register(int delay) {
		if (futures.isEmpty()) {
			BattlegroundEvent bgEvent = new BattlegroundEvent();
			bgEvent.setPriority(1);
			futures.add(GameEventServices.eventScheduler().scheduleAtFixedRate(bgEvent, delay, 6 * 60 * 1000));
		}
	}

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
	public static final BGService getInstance() {
		ObjectProvider<BGService> provider = instanceProvider;
		BGService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("BGService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * @param instanceProvider 副本提供者 / instanceProvider
	 */
	public static void setInstanceProvider(ObjectProvider<BGService> instanceProvider) {
		BGService.instanceProvider = instanceProvider;
	}
}
