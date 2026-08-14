package com.aionemu.gameserver.spawnengine;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.TemporarySpawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 临时刷怪引擎：按时间窗刷出/回收限时 NPC，并在整点切换时重算。
 * Temporary spawn engine: spawns/despawns timed NPCs and recalculates on hour change.
 */
public class TemporarySpawnEngine {

	/**
	 * 保护临时刷怪列表与实例映射的锁。
	 * Lock guarding temporary spawn lists and instance map.
	 */
	private static final Object temporarySpawnLock = new Object();

	/**
	 * 已注册的临时刷怪组列表。
	 * Registered temporary spawn groups.
	 */
	private static final List<SpawnGroup2> temporarySpawns = new ArrayList<SpawnGroup2>();

	/**
	 * 刷怪组 → 关联实例 ID 集合。
	 * Spawn group to associated instance ids.
	 */
	private static final Map<SpawnGroup2, HashSet<Integer>> tempSpawnInstanceMap = new LinkedHashMap<SpawnGroup2, HashSet<Integer>>();

	/**
	 * 启动时刷出所有处于刷怪时间窗内的临时对象。
	 * Spawns all temporary objects currently inside their spawn window (startup pass).
	 */
	public static void spawnAll() {
		spawn();
	}

	/**
	 * 整点变化或游戏时间被重载时先回收再按新时间窗刷出。
	 * On hour change or game-time reload: despawn then spawn for the new window.
	 */
	public static void onHourChange() {
		despawn();
		spawn();
	}

	/**
	 * 回收所有已离开刷怪时间窗的临时对象。
	 * Despawns all temporary objects that are outside their spawn window now.
	 */
	private static void despawn() {
		for (SpawnGroup2 spawn : temporarySpawnsSnapshot()) {
			for (SpawnTemplate template : spawn.getSpawnTemplates()) {
				if (!template.getTemporarySpawn().isInSpawnTime()) {
					List<VisibleObject> objects = template.getVisibleObjects();
					if (objects == null || objects.isEmpty()) {
						VisibleObject object = template.getVisibleObject();
						if (object != null) {
							objects = new ArrayList<>();
							objects.add(object);
						}
					}
					if (objects == null) {
						continue;
					}
					for (VisibleObject object : new ArrayList<>(objects)) {
						if (object instanceof Npc) {
							Npc npc = (Npc) object;
							if (!npc.getLifeStats().isAlreadyDead() && template.hasPool()) {
								spawn.setTemplateUse(npc.getInstanceId(), template, false);
							}
							npc.getController().cancelTask(TaskId.RESPAWN);
						}
						if (object.isSpawned()) {
							object.getController().onDelete();
						}
					}
					objects.clear();
					template.setVisibleObject(null);
				}
			}
		}
	}

	/**
	 * 按时间窗刷出临时对象；池组补齐缺额，其余仅补未在场的。
	 * Spawns temporary objects inside their spawn window; pools fill the gap, others spawn only when absent.
	 */
	private static void spawn() {
		for (SpawnGroup2 spawn : temporarySpawnsSnapshot()) {
			Set<Integer> instances = instancesSnapshot(spawn);
			if (spawn.hasPool()) {
				if (!spawn.geTemporarySpawn().isInSpawnTime()) {
					continue;
				}
				for (Integer instanceId : instances) {
					spawn.resetTemplates(instanceId);
					int spawned = 0;
					for (SpawnTemplate template : spawn.getSpawnTemplates()) {
						if (template.isInWorld(instanceId)) {
							spawned++;
						}
					}
					for (int pool = spawned; pool < spawn.getPool(); pool++) {
						SpawnTemplate template = spawn.getRndTemplate(instanceId);
						SpawnEngine.spawnObject(template, instanceId);
					}
				}
			} else {
				for (SpawnTemplate template : spawn.getSpawnTemplates()) {
					if (template.getTemporarySpawn().isInSpawnTime()) {
						for (Integer instanceId : instances) {
							if (!template.isInWorld(instanceId)) {
								SpawnEngine.spawnObject(template, instanceId);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 注册临时刷怪组及其实例。
	 * Registers a temporary spawn group for an instance.
	 *
	 * @param spawn 临时刷怪组 / the temporary spawn group
	 * @param instanceId 实例 ID / instance id
	 */
	public static void addSpawnGroup(SpawnGroup2 spawn, int instanceId) {
		synchronized (temporarySpawnLock) {
			HashSet<Integer> instances = tempSpawnInstanceMap.get(spawn);
			if (instances == null) {
				temporarySpawns.add(spawn);
				instances = new HashSet<Integer>();
				tempSpawnInstanceMap.put(spawn, instances);
			}
			instances.add(instanceId);
		}
	}

	/**
	 * 临时刷怪组列表快照，避免持锁遍历。
	 * Snapshot of temporary spawn groups to avoid iterating under lock.
	 *
	 * @return 刷怪组副本 / copy of spawn groups
	 */
	private static List<SpawnGroup2> temporarySpawnsSnapshot() {
		synchronized (temporarySpawnLock) {
			return new ArrayList<SpawnGroup2>(temporarySpawns);
		}
	}

	/**
	 * 指定刷怪组的实例 ID 快照。
	 * Snapshot of instance ids for a spawn group.
	 *
	 * @param spawn 临时刷怪组 / the temporary spawn group
	 * @return 实例 ID 集合副本 / copy of instance ids
	 */
	private static Set<Integer> instancesSnapshot(SpawnGroup2 spawn) {
		synchronized (temporarySpawnLock) {
			HashSet<Integer> instances = tempSpawnInstanceMap.get(spawn);
			if (instances == null) {
				return Collections.emptySet();
			}
			return new HashSet<Integer>(instances);
		}
	}
}
