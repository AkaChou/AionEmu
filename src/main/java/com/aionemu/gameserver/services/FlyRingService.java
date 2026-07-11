package com.aionemu.gameserver.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;

/**
 * 飞行环服务，启动时加载并生成全部飞行环。
 * Fly-ring service that loads and spawns all fly rings at startup.
 *
 * @author xavier
 */
@Slf4j
public class FlyRingService {

	private static volatile ObjectProvider<FlyRingService> instanceProvider;

	private static class SingletonHolder {

		protected static final FlyRingService instance = new FlyRingService();
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final FlyRingService getInstance() {
		ObjectProvider<FlyRingService> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<FlyRingService> instanceProvider) {
		FlyRingService.instanceProvider = instanceProvider;
	}

	/**
	 * 从静态数据加载飞行环模板并全部生成。
	 * Loads fly-ring templates from static data and spawns them all.
	 */
	public FlyRingService() {
		for (FlyRingTemplate t : DataManager.FLY_RING_DATA.getFlyRingTemplates()) {
			FlyRing f = new FlyRing(t, 0);
			f.spawn();
			log.debug("Added " + f.getName() + " at m=" + f.getWorldId() + ",x=" + f.getX() + ",y=" + f.getY() + ",z="
					+ f.getZ());
		}
	}
}
