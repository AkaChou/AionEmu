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
	public static final FlyRingService getInstance() {
		ObjectProvider<FlyRingService> provider = instanceProvider;
		FlyRingService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("FlyRingService 未由 Spring 提供："
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
