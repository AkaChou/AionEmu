package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.schedule.ConquestSchedule;
import com.aionemu.gameserver.configs.schedule.ConquestSchedule.Conquest;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.conquest.ConquestLocation;
import com.aionemu.gameserver.model.conquest.ConquestStateType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.conquestspawns.ConquestSpawnTemplate;
import com.aionemu.gameserver.services.conquestservice.ConquestOffering;
import com.aionemu.gameserver.services.conquestservice.ConquestStartRunnable;
import com.aionemu.gameserver.services.conquestservice.Offering;
import com.aionemu.gameserver.spawnengine.SpawnEngine;

/**
 * 征服/供奉（Conquest/Offering）世界活动服务：稀有怪与限时副本开启通知。
 * Service for Conquest/Offering world events: rare spawns and timed-instance open notices.
 *
 * @author Rinzler (Encom)
 */
@Slf4j(topic = "com.aionemu.gameserver.services.ZorshivDredgionService")
public class ConquestService {
	private static volatile ObjectProvider<ConquestService> instanceProvider;
	private ConquestSchedule conquestSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, ConquestLocation> conquest;
	private final ConcurrentMap<Integer, ConquestOffering<?>> activeConquest = new ConcurrentHashMap<Integer, ConquestOffering<?>>();

	/**
	 * 初始化征服活动地点：按配置加载并在和平状态刷怪。
	 * Initialize conquest locations: load data and spawn peace-state NPCs when enabled.
	 */
	public void initConquestLocations() {
		if (CustomConfig.CONQUEST_ENABLED) {
			conquest = DataManager.CONQUEST_DATA.getConquestLocations();
			for (ConquestLocation loc : getConquestLocations().values()) {
				spawn(loc, ConquestStateType.PEACE);
			}
			log.info(I18n.get("log.cdddb02cbd65", conquest.size()));
		} else {
			log.info(I18n.get("log.0364874809b0"));
			conquest = Collections.emptyMap();
		}
	}

	/**
	 * 初始化供奉活动并装载 cron 调度。
	 * Initialize the offering event and load its cron schedule.
	 */
	public void initOffering() {
		if (CustomConfig.CONQUEST_ENABLED) {
			log.info(I18n.get("log.ba7e51410ef5"));
		}
		reloadSchedule();
	}

	/**
	 * 重新加载征服/供奉时间表：取消旧任务并按新 cron 注册。
	 * Reload the conquest/offering schedule: cancel old tasks and re-register from cron.
	 */
	public synchronized void reloadSchedule() {
		ConquestSchedule newSchedule = CustomConfig.CONQUEST_ENABLED ? ConquestSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		conquestSchedule = newSchedule;
		if (conquestSchedule != null) {
			for (Conquest conquest : conquestSchedule.getConquestsList()) {
				for (String offeringTime : conquest.getOfferingTimes()) {
					Runnable task = new ConquestStartRunnable(conquest.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, offeringTime);
				}
			}
		}
	}

	/**
	 * 启动指定地点的征服/供奉活动，并在持续时长结束后自动停止。
	 * Start the conquest/offering at the given location and auto-stop after the configured duration.
	 *
	 * @param id 活动地点 ID / conquest location id
	 */
	public void startConquest(final int id) {
		ConquestOffering<?> offering = new Offering(conquest.get(id));
		if (activeConquest.putIfAbsent(id, offering) != null) {
			return;
		}
		offering.start();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopConquest(id);
			}
		}, CustomConfig.CONQUEST_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定地点的征服/供奉活动。
	 * Stop the conquest/offering at the given location.
	 *
	 * @param id 活动地点 ID / conquest location id
	 */
	public void stopConquest(int id) {
		ConquestOffering<?> offering = activeConquest.remove(id);
		if (offering == null || offering.isFinished()) {
			return;
		}
		offering.stop();
	}

	/**
	 * 按状态刷出征服活动相关 NPC。
	 * Spawn conquest-event NPCs for the given location and state.
	 *
	 * @param loc 活动地点 / conquest location
	 * spawn state
	 */
	public void spawn(ConquestLocation loc, ConquestStateType ostate) {
		if (ostate.equals(ConquestStateType.CONQUEST)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getConquestSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				ConquestSpawnTemplate conquesttemplate = (ConquestSpawnTemplate) st;
				if (conquesttemplate.getOStateType().equals(ostate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(conquesttemplate, 1));
				}
			}
		}
	}

	/**
	 * 清除指定地点已刷出的征服活动 NPC。
	 * Despawn conquest-event NPCs at the given location.
	 *
	 * @param loc 活动地点 / conquest location
	 */
	public void despawn(ConquestLocation loc) {
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
	 * 判断指定地点是否正在进行征服/供奉。
	 * Whether a conquest/offering is in progress at the given location.
	 *
	 * @param id 活动地点 ID / conquest location id
	 * 若 in progress 则为 true / true if in progress
	 */
	public boolean isConquestInProgress(int id) {
		return activeConquest.containsKey(id);
	}

	/**
	 * 返回当前活跃的征服/供奉映射。
	 * Return the map of currently active conquest/offering events.
	 *
	 * @return 地点 ID → 活动实例 / location id to event instance
	 */
	public Map<Integer, ConquestOffering<?>> getActiveConquest() {
		return activeConquest;
	}

	/**
	 * 返回征服/供奉持续时长（小时，来自配置）。
	 * Return conquest/offering duration in hours (from config).
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.CONQUEST_DURATION;
	}

	/**
	 * 按 ID 获取征服活动地点。
	 * Get a conquest location by id.
	 *
	 * @param id 活动地点 ID / conquest location id
	 * conquest location
	 */
	public ConquestLocation getConquestLocation(int id) {
		return conquest.get(id);
	}

	/**
	 * 返回全部征服活动地点。
	 * Return all conquest locations.
	 *
	 * location map
	 */
	public Map<Integer, ConquestLocation> getConquestLocations() {
		return conquest;
	}

	/**
	 * 获取 ConquestService 单例（Spring 提供者优先，否则 holder）。
	 * Return the ConquestService singleton (Spring provider first, else holder).
	 *
	 * service instance
	 */
	public static ConquestService getInstance() {
		ObjectProvider<ConquestService> provider = instanceProvider;
		if (provider == null) {
			return ConquestServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> ConquestServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring ObjectProvider，供 getInstance 使用。
	 * Inject the Spring ObjectProvider used by getInstance().
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<ConquestService> instanceProvider) {
		ConquestService.instanceProvider = instanceProvider;
	}

	private static class ConquestServiceHolder {
		private static final ConquestService INSTANCE = new ConquestService();
	}
}
