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

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.towerofeternityspawns.TowerOfEternitySpawnTemplate;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityLocation;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityStateType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FLAG_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FLAG_UPDATE;
import com.aionemu.gameserver.services.towerofeternityservice.Tower;
import com.aionemu.gameserver.services.towerofeternityservice.TowerOfEternity;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 永恒之塔服务，管理塔地点开关、刷怪与旗帜同步。
 * Tower of Eternity service managing tower open/close, spawns, and flag sync.
 *
 * @author Wnkrz
 */
@Slf4j

public class TowerOfEternityService {
	private static volatile ObjectProvider<TowerOfEternityService> instanceProvider;
	private Map<Integer, TowerOfEternityLocation> towerOfEternity;
	private final ConcurrentMap<Integer, TowerOfEternity<?>> activeTowerOfEternity = new ConcurrentHashMap<Integer, TowerOfEternity<?>>();

	/**
	 * 初始化永恒之塔地点并按关闭状态刷怪，注册定时开启。
	 * Initializes tower locations in closed state and registers scheduled opens.
	 */
	public void initTowerOfEternityLocation() {
		if (CustomConfig.TOWER_OF_ETERNITY_ENABLED) {
			towerOfEternity = DataManager.TOWER_OF_ETERNITY_DATA.getTowerOfEternityLocations();
			for (TowerOfEternityLocation loc : getTowerOfEternityLocations().values()) {
				spawn(loc, TowerOfEternityStateType.CLOSED);
			}
			log.info(I18n.get("log.42697e23a860", towerOfEternity.size()));

			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					startTowerOfEternity(Rnd.get(1, 5));
					startTowerOfEternity(Rnd.get(6, 10));
				}
			}, () -> CustomConfig.TOWER_OF_ETERNITY_SCHEDULE);
		} else {
			log.info(I18n.get("log.1f021afa9f81"));
			towerOfEternity = Collections.emptyMap();
		}
	}

	/**
	 * 初始化永恒之塔服务日志入口。
	 * Initializes Tower of Eternity service logging entry.
	 */
	public void initTowerOfEternity() {
		if (CustomConfig.TOWER_OF_ETERNITY_ENABLED) {
			log.info(I18n.get("log.8cba40ef1e05"));
		}
	}

	/**
	 * 按状态在地点刷出对应 NPC 并广播旗帜更新。
	 * Spawns NPCs for the location by state and broadcasts flag updates.
	 *
	 * location
	 * state type
	 */
	public void spawn(TowerOfEternityLocation loc, TowerOfEternityStateType tstate) {
		if (tstate.equals(TowerOfEternityStateType.OPEN)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getTowerOfEternitySpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				TowerOfEternitySpawnTemplate towerOfEternitySpawnTemplate = (TowerOfEternitySpawnTemplate) st;
				if (towerOfEternitySpawnTemplate.getTStateType().equals(tstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(towerOfEternitySpawnTemplate, 1));
					broadcastUpdate(loc);
				}
			}
		}
	}

	/**
	 * 清除地点已刷出的 NPC 并广播旗帜消失。
	 * Despawns NPCs at the location and broadcasts flag despawn.
	 *
	 * location
	 */
	public void despawn(TowerOfEternityLocation loc) {
		if (loc.getSpawned() == null) {
			return;
		}
		for (VisibleObject obj : new ArrayList<VisibleObject>(loc.getSpawned())) {
			Npc spawned = (Npc) obj;
			spawned.setDespawnDelayed(true);
			if (spawned.getAggroList().getList().isEmpty()) {
				spawned.getController().cancelTask(TaskId.RESPAWN);
				obj.getController().onDelete();
				broadcastDespawn(loc);
			}
		}
		loc.getSpawned().clear();
	}

	/**
	 * 启动指定 ID 的永恒之塔活动。
	 * Starts the Tower of Eternity event for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void startTowerOfEternity(final int id) {
		TowerOfEternity<?> tower = new Tower(towerOfEternity.get(id));
		if (activeTowerOfEternity.putIfAbsent(id, tower) != null) {
			return;
		}
		tower.start();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopTowerOfEternity(id);
			}
		}, CustomConfig.TOWER_OF_ETERNITY_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定 ID 的永恒之塔活动。
	 * Stops the Tower of Eternity event for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void stopTowerOfEternity(int id) {
		TowerOfEternity<?> tower = activeTowerOfEternity.remove(id);
		if (tower == null || tower.isClosed()) {
			return;
		}
		tower.stop();
	}

	/**
	 * 判断指定塔是否处于活动状态。
	 * Checks whether the tower with the given id is active.
	 *
	 * @param id 地点 ID / location id
	 * whether active
	 */
	public boolean isActive(int id) {
		return activeTowerOfEternity.containsKey(id);
	}

	/**
	 * 获取指定 ID 的活动塔实例。
	 * Returns the active tower instance for the given id.
	 *
	 * @param id 地点 ID / location id
	 * tower instance
	 */
	public TowerOfEternity<?> getActiveTower(int id) {
		return activeTowerOfEternity.get(id);
	}

	/**
	 * 玩家进入永恒之塔相关世界时同步旗帜信息。
	 * Syncs flag info when a player enters a tower-related world.
	 *
	 * @param player 玩家 / player
	 */
	public void onEnterTowerWorld(Player player) {
		if (((player.getWorldId() == 210100000) && (player.getRace() == Race.ELYOS))
				|| ((player.getWorldId() == 220110000) && (player.getRace() == Race.ELYOS))
				|| ((player.getWorldId() == 210100000) && (player.getRace() == Race.ASMODIANS))
				|| ((player.getWorldId() == 220110000) && (player.getRace() == Race.ASMODIANS))) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(player.getWorldId()).getMainWorldMapInstance()
					.doOnAllPlayers(new Visitor<Player>() {
						public void visit(Player player) {
							for (VisibleObject npc : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getNpcs()) {
								if ((npc.getObjectTemplate().getTemplateId() == 833765) && (npc.isSpawned())) {
									if ((player.getWorldId() == 210100000) && (player.getRace() == Race.ELYOS)) {
										PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, (Npc) npc));
									}
								}
								if ((npc.getObjectTemplate().getTemplateId() == 703146) && (npc.isSpawned())) {
									if ((player.getWorldId() == 220110000) && (player.getRace() == Race.ELYOS)) {
										PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, (Npc) npc));
									}
								}
								if ((npc.getObjectTemplate().getTemplateId() == 833765) && (npc.isSpawned())) {
									if ((player.getWorldId() == 210100000) && (player.getRace() == Race.ASMODIANS)) {
										PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, (Npc) npc));
									}
								}
								if ((npc.getObjectTemplate().getTemplateId() == 703146) && (npc.isSpawned())) {
									if ((player.getWorldId() == 220110000) && (player.getRace() == Race.ASMODIANS)) {
										PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, (Npc) npc));
									}
								}
							}
						}
					});
		}
	}

	private void broadcastUpdate(final TowerOfEternityLocation tower) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(tower.getWorldId()).getMainWorldMapInstance()
				.doOnAllPlayers(new Visitor<Player>() {
					public void visit(Player player) {
						for (VisibleObject npc : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getNpcs()) {
							if ((npc.getObjectTemplate().getTemplateId() == 833765) && (npc.isSpawned())) {
								if ((player.getWorldId() == 210100000) && (player.getRace() == Race.ELYOS)) {
									PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, (Npc) npc));
								}
							}
							if ((npc.getObjectTemplate().getTemplateId() == 703146) && (npc.isSpawned())) {
								if ((player.getWorldId() == 220110000) && (player.getRace() == Race.ELYOS)) {
									PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, (Npc) npc));
								}
							}
							if ((npc.getObjectTemplate().getTemplateId() == 833765) && (npc.isSpawned())) {
								if ((player.getWorldId() == 210100000) && (player.getRace() == Race.ASMODIANS)) {
									PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, (Npc) npc));
								}
							}
							if ((npc.getObjectTemplate().getTemplateId() == 703146) && (npc.isSpawned())) {
								if ((player.getWorldId() == 220110000) && (player.getRace() == Race.ASMODIANS)) {
									PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, (Npc) npc));
								}
							}
						}
					}
				});
	}

	private void broadcastDespawn(final TowerOfEternityLocation tower) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(tower.getWorldId()).getMainWorldMapInstance()
				.doOnAllPlayers(new Visitor<Player>() {
					public void visit(Player player) {
						for (VisibleObject npc : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getNpcs()) {
							if ((npc.getObjectTemplate().getTemplateId() == 833765) && (npc.isSpawned())) {
								if ((player.getWorldId() == 210100000) && (player.getRace() == Race.ELYOS)) {
									PacketSendUtility.sendPacket(player, new SM_FLAG_UPDATE((Npc) npc));
								}
							}
							if ((npc.getObjectTemplate().getTemplateId() == 703146) && (npc.isSpawned())) {
								if ((player.getWorldId() == 220110000) && (player.getRace() == Race.ELYOS)) {
									PacketSendUtility.sendPacket(player, new SM_FLAG_UPDATE((Npc) npc));
								}
							}
							if ((npc.getObjectTemplate().getTemplateId() == 833765) && (npc.isSpawned())) {
								if ((player.getWorldId() == 210100000) && (player.getRace() == Race.ASMODIANS)) {
									PacketSendUtility.sendPacket(player, new SM_FLAG_UPDATE((Npc) npc));
								}
							}
							if ((npc.getObjectTemplate().getTemplateId() == 703146) && (npc.isSpawned())) {
								if ((player.getWorldId() == 220110000) && (player.getRace() == Race.ASMODIANS)) {
									PacketSendUtility.sendPacket(player, new SM_FLAG_UPDATE((Npc) npc));
								}
							}
						}
					}
				});
	}

	/**
	 * 判断指定永恒之塔是否进行中。
	 * Checks whether the tower with the given id is in progress.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否进行中 / whether in progress
	 */
	public boolean isTowerOfEternityInProgress(int id) {
		return activeTowerOfEternity.containsKey(id);
	}

	/**
	 * 获取进行中的永恒之塔实例映射。
	 * Returns the map of active tower instances.
	 *
	 * @return 活动实例映射 / active instances map
	 */
	public Map<Integer, TowerOfEternity<?>> getActiveTowerOfEternity() {
		return activeTowerOfEternity;
	}

	/**
	 * 按 ID 获取进行中的永恒之塔实例。
	 * Returns the active tower instance by id.
	 *
	 * @param id 地点 ID / location id
	 * tower instance
	 */
	public TowerOfEternity<?> getActiveTowerOfEternity(int id) {
		return activeTowerOfEternity.get(id);
	}

	/**
	 * 获取活动持续时长（小时）。
	 * Returns the event duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.TOWER_OF_ETERNITY_DURATION;
	}

	/**
	 * 按 ID 获取永恒之塔地点。
	 * Returns the tower location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public TowerOfEternityLocation getTowerOfEternityLocation(int id) {
		return towerOfEternity.get(id);
	}

	/**
	 * 获取全部永恒之塔地点。
	 * Returns all tower locations.
	 *
	 * locations map
	 */
	public Map<Integer, TowerOfEternityLocation> getTowerOfEternityLocations() {
		return towerOfEternity;
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static TowerOfEternityService getInstance() {
		ObjectProvider<TowerOfEternityService> provider = instanceProvider;
		if (provider == null) {
			return TowerOfEternityServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> TowerOfEternityServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<TowerOfEternityService> instanceProvider) {
		TowerOfEternityService.instanceProvider = instanceProvider;
	}

	private static class TowerOfEternityServiceHolder {
		private static final TowerOfEternityService INSTANCE = new TowerOfEternityService();
	}
}
