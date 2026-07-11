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
	private List<ScheduledFuture<?>> futures = new ArrayList<ScheduledFuture<?>>();

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

	private static class SingletonHolder {
		protected static final BGService instance = new BGService();
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static final BGService getInstance() {
		ObjectProvider<BGService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
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
