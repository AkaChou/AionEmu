package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.curingzone.CuringObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.curingzones.CuringTemplate;
import com.aionemu.gameserver.skillengine.SkillEngine;
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
	private List<CuringObject> curingObjects = new ArrayList<CuringObject>();

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
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {

			public void run() {
				for (final CuringObject obj : curingObjects)
					obj.getKnownList().doOnAllPlayers(new Visitor<Player>() {
						public void visit(Player player) {
							if ((MathUtil.isIn3dRange(obj, player, obj.getRange()))
									&& (!player.getEffectController().hasAbnormalEffect(8751))) {
								GameEngineServices.skillEngine().getSkill(player, 8751, 1, player).useNoAnimationSkill();
							}
						}
					});
			}
		}, 1000, 1000);
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final CuringZoneService getInstance() {
		ObjectProvider<CuringZoneService> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<CuringZoneService> instanceProvider) {
		CuringZoneService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {

		protected static final CuringZoneService instance = new CuringZoneService();
	}
}
