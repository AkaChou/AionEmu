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
import com.aionemu.gameserver.configs.schedule.CircusSchedule;
import com.aionemu.gameserver.configs.schedule.CircusSchedule.Circus;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusLocation;
import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.nightmarecircusspawns.NightmareCircusSpawnTemplate;
import com.aionemu.gameserver.services.nightmarecircusservice.CircusInstance;
import com.aionemu.gameserver.services.nightmarecircusservice.CircusStartRunnable;
import com.aionemu.gameserver.services.nightmarecircusservice.Nightmare;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 梦魇马戏团活动服务，管理地点初始化、定时开关与刷怪。
 * Nightmare Circus event service managing location init, scheduled open/close, and spawns.
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class NightmareCircusService {
	private static volatile ObjectProvider<NightmareCircusService> instanceProvider;
	private CircusSchedule circusSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, NightmareCircusLocation> nightmareCircus;
	private final ConcurrentMap<Integer, CircusInstance<?>> activeNightmareCircus = new ConcurrentHashMap<Integer, CircusInstance<?>>();

	/**
	 * 初始化马戏团地点并按关闭状态刷怪。
	 * Initializes circus locations and spawns them in the closed state.
	 */
	public void initCircusLocations() {
		if (CustomConfig.NIGHTMARE_CIRCUS_ENABLE) {
			nightmareCircus = DataManager.NIGHTMARE_CIRCUS_DATA.getNightmareCircusLocations();
			for (NightmareCircusLocation loc : getNightmareCircusLocations().values()) {
				spawn(loc, NightmareCircusStateType.CLOSED);
			}
			log.info(I18n.get("log.64f9e92187e6", nightmareCircus.size()));
		} else {
			log.info(I18n.get("log.643edc83114c"));
			nightmareCircus = Collections.emptyMap();
		}
	}

	/**
	 * 初始化马戏团并加载定时计划。
	 * Initializes the circus and loads its schedule.
	 */
	public void initCircus() {
		if (CustomConfig.NIGHTMARE_CIRCUS_ENABLE) {
			log.info(I18n.get("log.7985c458fc25"));
		}
		reloadSchedule();
	}

	/**
	 * 重载马戏团 Cron 计划（先取消旧任务）。
	 * Reloads the circus cron schedule (cancels previous tasks first).
	 */
	public synchronized void reloadSchedule() {
		CircusSchedule newSchedule = CustomConfig.NIGHTMARE_CIRCUS_ENABLE ? CircusSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		circusSchedule = newSchedule;
		if (circusSchedule != null) {
			for (Circus circus : circusSchedule.getCircussList()) {
				for (String circusTime : circus.getCircusTimes()) {
					Runnable task = new CircusStartRunnable(circus.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, circusTime);
				}
			}
		}
	}

	/**
	 * 启动指定 ID 的梦魇马戏团活动。
	 * Starts the Nightmare Circus event for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void startNightmareCircus(final int id) {
		final CircusInstance<?> nightmare = new Nightmare(nightmareCircus.get(id));
		if (activeNightmareCircus.putIfAbsent(id, nightmare) != null) {
			return;
		}
		nightmare.start();
		dreamFaerieMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopNightmareCircus(id);
			}
		}, CustomConfig.NIGHTMARE_CIRCUS_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定 ID 的梦魇马戏团活动。
	 * Stops the Nightmare Circus event for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void stopNightmareCircus(int id) {
		CircusInstance<?> nightmare = activeNightmareCircus.remove(id);
		if (nightmare == null || nightmare.isClosed()) {
			return;
		}
		nightmare.stop();
	}

	/**
	 * 按状态在地点刷出对应 NPC。
	 * Spawns NPCs for the location according to the given state.
	 *
	 * location
	 * state type
	 */
	public void spawn(NightmareCircusLocation loc, NightmareCircusStateType nstate) {
		if (nstate.equals(NightmareCircusStateType.OPEN)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getNightmareCircusSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				NightmareCircusSpawnTemplate nightmareCircustemplate = (NightmareCircusSpawnTemplate) st;
				if (nightmareCircustemplate.getNStateType().equals(nstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(nightmareCircustemplate, 1));
				}
			}
		}
	}

	/**
	 * 向全服广播马戏团开启消息。
	 * Broadcasts the circus-open system message to all players.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean dreamFaerieMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendSys3Message(player, "\uE09B", "<Nightmare Circus> is now open !!!");
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 清除地点已刷出的 NPC。
	 * Despawns NPCs previously spawned at the location.
	 *
	 * location
	 */
	public void despawn(NightmareCircusLocation loc) {
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
	 * 判断指定马戏团是否进行中。
	 * Checks whether the circus with the given id is in progress.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否进行中 / whether in progress
	 */
	public boolean isNightmareCircusInProgress(int id) {
		return activeNightmareCircus.containsKey(id);
	}

	/**
	 * 获取进行中的马戏团实例映射。
	 * Returns the map of active circus instances.
	 *
	 * @return 活动实例映射 / active instances map
	 */
	public Map<Integer, CircusInstance<?>> getActiveNightmareCircus() {
		return activeNightmareCircus;
	}

	/**
	 * 获取活动持续时长（小时）。
	 * Returns the event duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.NIGHTMARE_CIRCUS_DURATION;
	}

	/**
	 * 按 ID 获取马戏团地点。
	 * Returns the circus location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public NightmareCircusLocation getNightmareCircusLocation(int id) {
		return nightmareCircus.get(id);
	}

	/**
	 * 获取全部马戏团地点。
	 * Returns all circus locations.
	 *
	 * locations map
	 */
	public Map<Integer, NightmareCircusLocation> getNightmareCircusLocations() {
		return nightmareCircus;
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static NightmareCircusService getInstance() {
		ObjectProvider<NightmareCircusService> provider = instanceProvider;
		if (provider == null) {
			return NightmareCircusServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> NightmareCircusServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<NightmareCircusService> instanceProvider) {
		NightmareCircusService.instanceProvider = instanceProvider;
	}

	private static class NightmareCircusServiceHolder {
		private static final NightmareCircusService INSTANCE = new NightmareCircusService();
	}
}
