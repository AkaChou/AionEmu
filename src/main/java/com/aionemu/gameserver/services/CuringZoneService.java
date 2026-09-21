package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.curingzone.CuringObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.curingzones.CuringTemplate;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 治愈区域服务，生成治愈物并周期性对范围内玩家施加治愈效果。
 * Curing-zone service that spawns curing objects and periodically applies the heal effect to nearby players.
 */
@Slf4j
public class CuringZoneService {

	private static volatile ObjectProvider<CuringZoneService> instanceProvider;
	/** 已生成的治愈物列表。 / Spawned curing objects. */
	private final List<CuringObject> curingObjects = new ArrayList<>();

	/**
	 * 加载治愈模板、生成治愈物并启动周期任务。
	 * Loads curing templates, spawns objects, and starts the periodic task.
	 */
	public CuringZoneService() {
		for (CuringTemplate t : DataManager.CURING_OBJECTS_DATA.getCuringObject()) {
			CuringObject obj = new CuringObject(t, 0);
			obj.spawn();
			curingObjects.add(obj);
		}
		log.info(I18n.get("log.d429a803f8d3"));
		startTask();
	}

	/**
	 * 启动每秒扫描任务，对范围内未带效果的玩家施放治愈技能。
	 * Starts the per-second scan that casts the curing skill on in-range players without the effect.
	 */
	private void startTask() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
			for (final CuringObject obj : curingObjects)
				obj.getKnownList().doOnAllPlayers(player -> {
					if ((MathUtil.isIn3dRange(obj, player, obj.getRange()))
							&& (!player.getEffectController().hasAbnormalEffect(8751))) {
						GameEngineServices.skillEngine().getSkill(player, 8751, 1, player).useNoAnimationSkill();
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
	public static final CuringZoneService getInstance() {
		ObjectProvider<CuringZoneService> provider = instanceProvider;
		CuringZoneService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("CuringZoneService 未由 Spring 提供："
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
	public static void setInstanceProvider(ObjectProvider<CuringZoneService> instanceProvider) {
		CuringZoneService.instanceProvider = instanceProvider;
	}
}
