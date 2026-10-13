package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.services.InstanceRiftService;
import lombok.extern.slf4j.Slf4j;

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
import com.aionemu.gameserver.model.instancerift.InstanceRiftLocation;
import com.aionemu.gameserver.model.instancerift.InstanceRiftStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.instanceriftspawns.InstanceRiftSpawnTemplate;
import com.aionemu.gameserver.services.instanceriftservice.Rift;
import com.aionemu.gameserver.services.instanceriftservice.RiftInstance;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;

/**
 * 副本裂隙服务：按 InstanceSchedule 调度开启，管理刷怪与生命周期。
 * Instance Rift service: opens by InstanceSchedule and manages spawn lifecycle.
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class InstanceRiftService {
	private static volatile ObjectProvider<InstanceRiftService> instanceProvider;
	private static volatile InstanceRiftService resolvedInstance;
	private InstanceSchedule instanceSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, InstanceRiftLocation> instanceRift;
	private final ConcurrentMap<Integer, RiftInstance<?>> activeInstanceRift = new ConcurrentHashMap<>();

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
		scheduledTasks.forEach(CronService.requireCurrent()::cancel);
		scheduledTasks.clear();
		instanceSchedule = newSchedule;
		if (instanceSchedule != null) {
			for (Instance instance : instanceSchedule.getInstancesList()) {
				for (String instanceTime : instance.getInstanceTimes()) {
					Runnable task = new InstanceStartRunnable(this, instance.getId());
					scheduledTasks.add(task);
					CronService.requireCurrent().schedule(task, instanceTime);
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
		ThreadPoolManager.getInstance().schedule(() -> stopInstanceRift(id), (long) CustomConfig.INSTANCE_RIFT_DURATION * 3600 * 1000);
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
	 * @param loc    location
	 * @param estate state type
	 */
	public void spawn(InstanceRiftLocation loc, InstanceRiftStateType estate) {
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
	public void instanceRiftMsg(int id) {
		switch (id) {
			case 1 -> {
				World.getInstance().doOnAllPlayers(player -> PacketSendUtility.sendSys3Message(player, "\uE04C", "<Instance Rift> is now open !!!"));
			}
			default -> {
			}
		};
	}

	/**
	 * 清除地点上已刷出的对象（无仇恨时立即删除）。
	 * Clears spawned objects at the location (deletes immediately when no aggro).
	 *
	 * @param loc location
	 */
	public void despawn(InstanceRiftLocation loc) {
		if (loc.getSpawned() == null) {
			return;
		}
		for (VisibleObject obj : new ArrayList<>(loc.getSpawned())) {
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
	 *           location
	 */
	public InstanceRiftLocation getInstanceRiftLocation(int id) {
		return instanceRift.get(id);
	}

	/**
	 * 获取全部地点。
	 * Returns all locations.
	 * location map
	 */
	public Map<Integer, InstanceRiftLocation> getInstanceRiftLocations() {
		return instanceRift;
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *                               when no provider or bean is available
	 */
	public static InstanceRiftService getInstance() {
		InstanceRiftService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<InstanceRiftService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("InstanceRiftService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<InstanceRiftService> instanceProvider) {
		InstanceRiftService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

	/**
	 * 副本裂隙启动定时任务。
	 * Start runnable for instance-rift events.
	 */
	private record InstanceStartRunnable(InstanceRiftService service, int id) implements Runnable {
		/**
		 * 查找匹配地点并启动裂隙。
		 * Finds the matching location and starts the rift.
		 */
		@Override
		public void run() {
			Map<Integer, InstanceRiftLocation> locations = service.getInstanceRiftLocations();
			for (InstanceRiftLocation loc : locations.values()) {
				if (loc.getId() == id) {
					service.startInstanceRift(loc.getId());
				}
			}
		}
	}
}
