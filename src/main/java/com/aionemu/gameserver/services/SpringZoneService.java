package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.springzone.SpringObject;
import com.aionemu.gameserver.model.templates.springzones.SpringTemplate;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 泉水区域服务，刷出泉水对象并为范围内玩家施加守护祝福。
 * Spring zone service that spawns spring objects and applies Bless of Guardian Spring to nearby players.
 *
 * @author Rinzler (Encom)
 */
@Slf4j
public class SpringZoneService {
	private static volatile ObjectProvider<SpringZoneService> instanceProvider;
	private final List<SpringObject> springObjects = new ArrayList<>();

	/**
	 * 构造服务：刷出泉水对象并启动定时效果任务。
	 * Constructs the service: spawns spring objects and starts the periodic effect task.
	 */
	public SpringZoneService() {
		for (SpringTemplate t : DataManager.SPRING_OBJECTS_DATA.getSpringObject()) {
			SpringObject obj = new SpringObject(t, 0);
			obj.spawn();
			springObjects.add(obj);
		}
		startSpring();
	}

	/**
	 * 定时为泉水范围内且尚未持有效果的玩家施加技能 17560。
	 * Periodically applies skill 17560 to players in spring range without the effect.
	 */
	private void startSpring() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
			for (final SpringObject obj : springObjects)
				obj.getKnownList().doOnAllPlayers(player -> {
					if ((MathUtil.isIn3dRange(obj, player, obj.getRange()))
							&& (!player.getEffectController().hasAbnormalEffect(17560))) { // Bless Of Guardian
																							// 泉。 / Spring.
						GameEngineServices.skillEngine().getSkill(player, 17560, 1, player).useNoAnimationSkill();
					}
				});
		}, 1000, 1000);
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
	public static final SpringZoneService getInstance() {
		ObjectProvider<SpringZoneService> provider = instanceProvider;
		SpringZoneService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("SpringZoneService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<SpringZoneService> instanceProvider) {
		SpringZoneService.instanceProvider = instanceProvider;
	}
}
