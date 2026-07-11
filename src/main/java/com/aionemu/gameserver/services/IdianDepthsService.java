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
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.idiandepths.IdianDepthsLocation;
import com.aionemu.gameserver.model.idiandepths.IdianDepthsStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.idiandepthsspawns.IdianDepthsSpawnTemplate;
import com.aionemu.gameserver.services.idiandepthsservice.Idian;
import com.aionemu.gameserver.services.idiandepthsservice.IdianDepths;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 伊迪安深渊服务：按计划开启各地点，管理刷怪与活动生命周期。
 * Idian Depths service: schedule-opens locations and manages spawn/event lifecycle.
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class IdianDepthsService {
	private static volatile ObjectProvider<IdianDepthsService> instanceProvider;
	private Map<Integer, IdianDepthsLocation> idianDepths;
	private final ConcurrentMap<Integer, IdianDepths<?>> activeIdianDepths = new ConcurrentHashMap<Integer, IdianDepths<?>>();

	/**
	 * 加载伊迪安深渊地点、刷关闭态 NPC，并注册开启 cron。
	 * Loads Idian Depths locations, spawns closed-state NPCs, and registers open cron.
	 */
	public void initIdianDepthsLocations() {
		if (CustomConfig.IDIAN_DEPTHS_ENABLED) {
			idianDepths = DataManager.IDIAN_DEPTHS_DATA.getIdianDepthsLocations();
			for (IdianDepthsLocation loc : getIdianDepthsLocations().values()) {
				spawn(loc, IdianDepthsStateType.CLOSED);
			}
			log.info(I18n.get("log.35480b35b73c", idianDepths.size()));

			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					for (IdianDepthsLocation loc : getIdianDepthsLocations().values()) {
						startIdianDepths(loc.getId());
					}
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendSys3Message(player, "\uE0AA", "<Idian Depths> open !!!");
						}
					});
				}
			}, () -> CustomConfig.IDIAN_DEPTHS_SCHEDULE);
		} else {
			log.info(I18n.get("log.b09d3ab19771"));
			idianDepths = Collections.emptyMap();
		}
	}

	/**
	 * 记录伊迪安深渊功能启用日志。
	 * Logs that Idian Depths is enabled (when configured).
	 */
	public void initIdianDepths() {
		if (CustomConfig.IDIAN_DEPTHS_ENABLED) {
			log.info(I18n.get("log.5227a2ba2e5f"));
		}
	}

	/**
	 * 启动指定 ID 的伊迪安深渊，并在持续时长后自动关闭。
	 * Starts Idian Depths for the given id and auto-stops after configured duration.
	 *
	 * @param id 地点 ID / location id
	 */
	public void startIdianDepths(final int id) {
		final IdianDepths<?> idian = new Idian(idianDepths.get(id));
		if (activeIdianDepths.putIfAbsent(id, idian) != null) {
			return;
		}
		idian.start();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopIdianDepths(id);
			}
		}, CustomConfig.IDIAN_DEPTHS_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定 ID 的伊迪安深渊。
	 * Stops Idian Depths for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void stopIdianDepths(int id) {
		IdianDepths<?> idian = activeIdianDepths.remove(id);
		if (idian == null || idian.isClosed()) {
			return;
		}
		idian.stop();
	}

	/**
	 * 按状态类型在地点刷出对应模板 NPC。
	 * Spawns NPCs for the location matching the given state type.
	 *
	 * location
	 * state type
	 */
	public void spawn(IdianDepthsLocation loc, IdianDepthsStateType istate) {
		if (istate.equals(IdianDepthsStateType.OPEN)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getIdianDepthsSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				IdianDepthsSpawnTemplate idianDepthsttemplate = (IdianDepthsSpawnTemplate) st;
				if (idianDepthsttemplate.getIStateType().equals(istate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(idianDepthsttemplate, 1));
				}
			}
		}
	}

	/**
	 * 清除地点上已刷出的对象（无仇恨时立即删除）。
	 * Clears spawned objects at the location (deletes immediately when no aggro).
	 *
	 * location
	 */
	public void despawn(IdianDepthsLocation loc) {
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
	 * 指定地点是否正在进行中。
	 * Whether Idian Depths is in progress for the given id.
	 *
	 * @param id 地点 ID / location id
	 * @return 若 active 则为 true / true if active
	 */
	public boolean isIdianDepthsInProgress(int id) {
		return activeIdianDepths.containsKey(id);
	}

	/**
	 * 获取当前激活的伊迪安深渊映射。
	 * Returns the map of active Idian Depths instances.
	 *
	 * active instances
	 */
	public Map<Integer, IdianDepths<?>> getActiveIdianDepths() {
		return activeIdianDepths;
	}

	/**
	 * 返回配置的持续时长（小时）。
	 * Returns configured duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.IDIAN_DEPTHS_DURATION;
	}

	/**
	 * 按 ID 获取地点。
	 * Returns the location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public IdianDepthsLocation getIdianDepthsLocation(int id) {
		return idianDepths.get(id);
	}

	/**
	 * 获取全部地点。
	 * Returns all locations.
	 *
	 * location map
	 */
	public Map<Integer, IdianDepthsLocation> getIdianDepthsLocations() {
		return idianDepths;
	}

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则 holder）。
	 * Returns the service singleton (Spring ObjectProvider if set, else holder).
	 *
	 * service instance
	 */
	public static IdianDepthsService getInstance() {
		ObjectProvider<IdianDepthsService> provider = instanceProvider;
		if (provider == null) {
			return IdianDepthsServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> IdianDepthsServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<IdianDepthsService> instanceProvider) {
		IdianDepthsService.instanceProvider = instanceProvider;
	}

	private static class IdianDepthsServiceHolder {
		private static final IdianDepthsService INSTANCE = new IdianDepthsService();
	}
}
