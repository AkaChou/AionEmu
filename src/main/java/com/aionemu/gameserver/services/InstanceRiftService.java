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
import com.aionemu.gameserver.configs.schedule.InstanceSchedule;
import com.aionemu.gameserver.configs.schedule.InstanceSchedule.Instance;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instancerift.InstanceRiftLocation;
import com.aionemu.gameserver.model.instancerift.InstanceRiftStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.instanceriftspawns.InstanceRiftSpawnTemplate;
import com.aionemu.gameserver.services.instanceriftservice.InstanceStartRunnable;
import com.aionemu.gameserver.services.instanceriftservice.Rift;
import com.aionemu.gameserver.services.instanceriftservice.RiftInstance;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 副本裂隙服务：按 InstanceSchedule 调度开启，管理刷怪与生命周期。
 * Instance Rift service: opens by InstanceSchedule and manages spawn lifecycle.
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class InstanceRiftService {
	private static volatile ObjectProvider<InstanceRiftService> instanceProvider;
	private InstanceSchedule instanceSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, InstanceRiftLocation> instanceRift;
	private final ConcurrentMap<Integer, RiftInstance<?>> activeInstanceRift = new ConcurrentHashMap<Integer, RiftInstance<?>>();

	/**
	 * 加载副本裂隙地点并刷关闭态 NPC。
	 * Loads instance-rift locations and spawns closed-state NPCs.
	 */
	public void initInstanceLocations() {
		if (CustomConfig.INSTANCE_RIFT_ENABLED) {
			instanceRift = DataManager.INSTANCE_RIFT_DATA.getInstanceRiftLocations();
			for (InstanceRiftLocation loc : getInstanceRiftLocations().values()) {
				spawn(loc, InstanceRiftStateType.CLOSED);
			}
			log.info(I18n.get("log.da34bb44d09e", instanceRift.size()));
		} else {
			log.info(I18n.get("log.a881539ad74c"));
			instanceRift = Collections.emptyMap();
		}
	}

	/**
	 * 初始化并（重）加载 cron 调度。
	 * Initializes and (re)loads cron schedules.
	 */
	public void initInstance() {
		if (CustomConfig.INSTANCE_RIFT_ENABLED) {
			log.info(I18n.get("log.9c13f28080ec"));
		}
		reloadSchedule();
	}

	/**
	 * 重新加载 InstanceSchedule：取消旧任务并注册新 cron。
	 * Reloads InstanceSchedule: cancels old tasks and registers new crons.
	 */
	public synchronized void reloadSchedule() {
		InstanceSchedule newSchedule = CustomConfig.INSTANCE_RIFT_ENABLED ? InstanceSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		instanceSchedule = newSchedule;
		if (instanceSchedule != null) {
			for (Instance instance : instanceSchedule.getInstancesList()) {
				for (String instanceTime : instance.getInstanceTimes()) {
					Runnable task = new InstanceStartRunnable(instance.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, instanceTime);
				}
			}
		}
	}

	/**
	 * 启动指定 ID 的副本裂隙，广播消息并在持续时长后自动关闭。
	 * Starts the instance rift for the given id, broadcasts, and auto-stops after duration.
	 *
	 * @param id 地点 ID / location id
	 */
	public void startInstanceRift(final int id) {
		RiftInstance<?> rift = new Rift(instanceRift.get(id));
		if (activeInstanceRift.putIfAbsent(id, rift) != null) {
			return;
		}
		rift.start();
		instanceRiftMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopInstanceRift(id);
			}
		}, CustomConfig.INSTANCE_RIFT_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定 ID 的副本裂隙。
	 * Stops the instance rift for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void stopInstanceRift(int id) {
		RiftInstance<?> rift = activeInstanceRift.remove(id);
		if (rift == null || rift.isClosed()) {
			return;
		}
		rift.stop();
	}

	/**
	 * 按状态类型在地点刷出对应模板 NPC。
	 * Spawns NPCs for the location matching the given state type.
	 *
	 * location
	 * state type
	 */
	public void spawn(InstanceRiftLocation loc, InstanceRiftStateType estate) {
		if (estate.equals(InstanceRiftStateType.OPEN)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getInstanceRiftSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				InstanceRiftSpawnTemplate instanceRifttemplate = (InstanceRiftSpawnTemplate) st;
				if (instanceRifttemplate.getEStateType().equals(estate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(instanceRifttemplate, 1));
				}
			}
		}
	}

	/**
	 * 向全体玩家广播副本裂隙开启消息（按 ID）。
	 * Broadcasts instance-rift open message to all players (by id).
	 *
	 * @param id 地点 ID / location id
	 * @return 若 handled 则为 true / true if handled
	 */
	public boolean instanceRiftMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendSys3Message(player, "\uE04C", "<Instance Rift> is now open !!!");
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 清除地点上已刷出的对象（无仇恨时立即删除）。
	 * Clears spawned objects at the location (deletes immediately when no aggro).
	 *
	 * location
	 */
	public void despawn(InstanceRiftLocation loc) {
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
	 * 指定副本裂隙是否正在进行中。
	 * Whether the given instance rift is in progress.
	 *
	 * @param id 地点 ID / location id
	 * @return 若 active 则为 true / true if active
	 */
	public boolean isInstanceRiftInProgress(int id) {
		return activeInstanceRift.containsKey(id);
	}

	/**
	 * 获取当前激活的副本裂隙映射。
	 * Returns the map of active instance rifts.
	 *
	 * active rifts
	 */
	public Map<Integer, RiftInstance<?>> getActiveInstanceRift() {
		return activeInstanceRift;
	}

	/**
	 * 返回配置的持续时长（小时）。
	 * Returns configured duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.INSTANCE_RIFT_DURATION;
	}

	/**
	 * 按 ID 获取地点。
	 * Returns the location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public InstanceRiftLocation getInstanceRiftLocation(int id) {
		return instanceRift.get(id);
	}

	/**
	 * 获取全部地点。
	 * Returns all locations.
	 *
	 * location map
	 */
	public Map<Integer, InstanceRiftLocation> getInstanceRiftLocations() {
		return instanceRift;
	}

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则 holder）。
	 * Returns the service singleton (Spring ObjectProvider if set, else holder).
	 *
	 * service instance
	 */
	public static InstanceRiftService getInstance() {
		ObjectProvider<InstanceRiftService> provider = instanceProvider;
		if (provider == null) {
			return InstanceRiftServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> InstanceRiftServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<InstanceRiftService> instanceProvider) {
		InstanceRiftService.instanceProvider = instanceProvider;
	}

	private static class InstanceRiftServiceHolder {
		private static final InstanceRiftService INSTANCE = new InstanceRiftService();
	}
}
