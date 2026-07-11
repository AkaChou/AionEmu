package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.dynamicrift.DynamicRiftLocation;
import com.aionemu.gameserver.model.dynamicrift.DynamicRiftStateType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.dynamicriftspawns.DynamicRiftSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.dynamicriftservice.DynamicRift;
import com.aionemu.gameserver.services.dynamicriftservice.Portal;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 动态裂隙服务：按 cron 开启龙/因德拉图/术古商队等动态裂隙，管理刷怪与生命周期。
 * Dynamic Rift service: cron-opens Dragon/Indratoo/Shugo portals and manages spawn lifecycle.
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class DynamicRiftService {
	private static volatile ObjectProvider<DynamicRiftService> instanceProvider;
	private Map<Integer, DynamicRiftLocation> dynamicRift;
	private final ConcurrentMap<Integer, DynamicRift<?>> activeDynamicRift = new ConcurrentHashMap<Integer, DynamicRift<?>>();

	/**
	 * 加载动态裂隙地点、刷关闭态 NPC，并注册各类型开启 cron。
	 * Loads dynamic-rift locations, spawns closed-state NPCs, and registers open crons.
	 */
	public void initDynamicRiftLocations() {
		if (CustomConfig.DYNAMIC_RIFT_ENABLED) {
			dynamicRift = DataManager.DYNAMIC_RIFT_DATA.getDynamicRiftLocations();
			for (DynamicRiftLocation loc : getDynamicRiftLocations().values()) {
				spawn(loc, DynamicRiftStateType.CLOSED);
			}
			log.info(I18n.get("log.69fd598b3b65", dynamicRift.size()));
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					startDynamicRift(1);
					startDynamicRift(3);
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendPacket(player,
									SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_PORTAL_OPEN_IDDF3_Dragon);
						}
					});
				}
			}, () -> CustomConfig.DYNAMIC_RIFT_DRAGON_SCHEDULE);
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					startDynamicRift(2);
					startDynamicRift(4);
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendPacket(player,
									SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_PORTAL_OPEN_IDLF3_Castle_Indratoo);
						}
					});
				}
			}, () -> CustomConfig.DYNAMIC_RIFT_INDRATOO_SCHEDULE);
			// 术古商人联盟 / Shugo Merchant League
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					startDynamicRift(5);
					startDynamicRift(6);
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							// 术古商人联盟已到达。 / The Shugo Merchant League has arrived.
							PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HF_ShugoCaravanAppear);
						}
					});
				}
			}, () -> CustomConfig.SHUGO_MERCHANT_LEAGUE_SCHEDULE);
		} else {
			dynamicRift = Collections.emptyMap();
		}
	}

	/**
	 * 记录动态裂隙功能启用/禁用日志。
	 * Logs whether Dynamic Rift is enabled or disabled.
	 */
	public void initDynamicRift() {
		if (CustomConfig.DYNAMIC_RIFT_ENABLED) {
			log.info(I18n.get("log.8d47511e47ac"));
		} else {
			log.info(I18n.get("log.f7a5bb06c320"));
		}
	}

	/**
	 * 启动指定 ID 的动态裂隙，并在持续时长后自动关闭。
	 * Starts the dynamic rift for the given id and auto-stops after configured duration.
	 *
	 * @param id 裂隙地点 ID / rift location id
	 */
	public void startDynamicRift(final int id) {
		DynamicRift<?> portal = new Portal(dynamicRift.get(id));
		if (activeDynamicRift.putIfAbsent(id, portal) != null) {
			return;
		}
		portal.start();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopDynamicRift(id);
			}
		}, CustomConfig.DYNAMIC_RIFT_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定 ID 的动态裂隙。
	 * Stops the dynamic rift for the given id.
	 *
	 * @param id 裂隙地点 ID / rift location id
	 */
	public void stopDynamicRift(int id) {
		DynamicRift<?> portal = activeDynamicRift.remove(id);
		if (portal == null || portal.isClosed()) {
			return;
		}
		portal.stop();
	}

	/**
	 * 按状态类型在地点刷出对应模板 NPC。
	 * Spawns NPCs for the location matching the given state type.
	 *
	 * @param loc 裂隙地点 / rift location
	 * state type
	 */
	public void spawn(DynamicRiftLocation loc, DynamicRiftStateType dstate) {
		if (dstate.equals(DynamicRiftStateType.OPEN)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getDynamicRiftSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				DynamicRiftSpawnTemplate dynamicRifttemplate = (DynamicRiftSpawnTemplate) st;
				if (dynamicRifttemplate.getDStateType().equals(dstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(dynamicRifttemplate, 1));
				}
			}
		}
	}

	/**
	 * 清除地点上已刷出的对象（无仇恨时立即删除）。
	 * Clears spawned objects at the location (deletes immediately when no aggro).
	 *
	 * @param loc 裂隙地点 / rift location
	 */
	public void despawn(DynamicRiftLocation loc) {
		if (loc.getSpawned() == null) {
			return;
		}
		for (VisibleObject obj : new ArrayList<VisibleObject>(loc.getSpawned())) {
			Npc spawned = (Npc) obj;
			spawned.setDespawnDelayed(true);
			if (spawned.getAggroList().getList().isEmpty()) {
				spawned.getController().cancelTask(TaskId.RESPAWN);
				obj.getController().onDelete();
			}
		}
		loc.getSpawned().clear();
	}

	/**
	 * 指定裂隙是否正在进行中。
	 * Whether the given dynamic rift is in progress.
	 *
	 * @param id 裂隙地点 ID / rift location id
	 * @return 若 active 则为 true / true if active
	 */
	public boolean isDynamicRiftInProgress(int id) {
		return activeDynamicRift.containsKey(id);
	}

	/**
	 * 获取当前激活的动态裂隙映射。
	 * Returns the map of active dynamic rifts.
	 *
	 * active rifts
	 */
	public Map<Integer, DynamicRift<?>> getActiveDynamicRift() {
		return activeDynamicRift;
	}

	/**
	 * 返回配置的动态裂隙持续时长（小时）。
	 * Returns configured dynamic-rift duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.DYNAMIC_RIFT_DURATION;
	}

	/**
	 * 按 ID 获取动态裂隙地点。
	 * Returns the dynamic-rift location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public DynamicRiftLocation getDynamicRiftLocation(int id) {
		return dynamicRift.get(id);
	}

	/**
	 * 获取全部动态裂隙地点。
	 * Returns all dynamic-rift locations.
	 *
	 * location map
	 */
	public Map<Integer, DynamicRiftLocation> getDynamicRiftLocations() {
		return dynamicRift;
	}

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则 holder）。
	 * Returns the service singleton (Spring ObjectProvider if set, else holder).
	 *
	 * service instance
	 */
	public static DynamicRiftService getInstance() {
		ObjectProvider<DynamicRiftService> provider = instanceProvider;
		if (provider == null) {
			return DynamicRiftServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> DynamicRiftServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<DynamicRiftService> instanceProvider) {
		DynamicRiftService.instanceProvider = instanceProvider;
	}

	private static class DynamicRiftServiceHolder {
		private static final DynamicRiftService INSTANCE = new DynamicRiftService();
	}
}
