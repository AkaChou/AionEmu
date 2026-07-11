package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.springzone.SpringObject;
import com.aionemu.gameserver.model.templates.springzones.SpringTemplate;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 泉水区域服务，刷出泉水对象并为范围内玩家施加守护祝福。
 * Spring zone service that spawns spring objects and applies Bless of Guardian Spring to nearby players.
 *
 * @author Rinzler (Encom)
 */
@Slf4j
public class SpringZoneService {
	private static volatile ObjectProvider<SpringZoneService> instanceProvider;
	private List<SpringObject> springObjects = new ArrayList<SpringObject>();

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
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			public void run() {
				for (final SpringObject obj : springObjects)
					obj.getKnownList().doOnAllPlayers(new Visitor<Player>() {
						public void visit(Player player) {
							if ((MathUtil.isIn3dRange(obj, player, obj.getRange()))
									&& (!player.getEffectController().hasAbnormalEffect(17560))) { // Bless Of Guardian
																									// 泉。 / Spring.
								GameEngineServices.skillEngine().getSkill(player, 17560, 1, player).useNoAnimationSkill();
							}
						}
					});
			}
		}, 1000, 1000);
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 */
	public static final SpringZoneService getInstance() {
		ObjectProvider<SpringZoneService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
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

	private static class SingletonHolder {
		protected static final SpringZoneService instance = new SpringZoneService();
	}
}
