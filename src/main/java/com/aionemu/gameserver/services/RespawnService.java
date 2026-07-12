package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;

import java.util.Set;
import java.util.concurrent.Future;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.world.World;

/**
 * NPC 尸体消散与重生调度服务。
 * Service for scheduling NPC corpse decay and respawn.
 *
 * @author ATracer, Source, xTz
 */
public class RespawnService {
	private static final int IMMEDIATE_DECAY = 5 * 1000;
	private static final int WITHOUT_DROP_DECAY = 0;
	private static final int WITH_DROP_DECAY = 5 * 60 * 1000;

	/**
	 * 根据掉落情况调度 NPC 尸体消散任务。
	 * Schedules NPC corpse decay based on current drop state.
	 *
	 * target NPC
	 *
	 * @param npc
	 * @return 消散任务句柄 / decay task handle
	 */
	public static Future<?> scheduleDecayTask(Npc npc) {
		int decayInterval;
		Set<DropItem> drop = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());

		if (drop == null) {
			decayInterval = IMMEDIATE_DECAY;
		} else if (drop.isEmpty()) {
			decayInterval = WITHOUT_DROP_DECAY;
		} else {
			decayInterval = WITH_DROP_DECAY;
		}
		return scheduleDecayTask(npc, decayInterval);
	}

	/**
	 * 按指定间隔调度 NPC 尸体消散任务。
	 * Schedules NPC corpse decay after the given interval.
	 *
	 * target NPC
	 *
	 * @param decayInterval 消散延迟（毫秒） / decay delay in milliseconds
	 * @param decayInterval
	 * @return 消散任务句柄 / decay task handle
	 */
	public static Future<?> scheduleDecayTask(Npc npc, long decayInterval) {
		return GameThreadPoolServices.threadPoolManager().schedule(new DecayTask(npc.getObjectId()), decayInterval);
	}

	/**
	 * 按刷新模板的重生时间调度可见对象重生。
	 * Schedules respawn of a visible object using its spawn template interval.
	 *
	 * visible object
	 *
	 * @param visibleObject
	 * @return 重生任务句柄 / respawn task handle
	 */
	public static final Future<?> scheduleRespawnTask(VisibleObject visibleObject) {
		final int interval = visibleObject.getSpawn().getRespawnTime();
		SpawnTemplate spawnTemplate = visibleObject.getSpawn();
		int instanceId = visibleObject.getInstanceId();
		return GameThreadPoolServices.threadPoolManager().schedule(new RespawnTask(spawnTemplate, instanceId), interval * 1000);
	}

	/**
	 * 在指定副本实例中执行一次重生。
	 * Performs a single respawn in the given instance.
	 *
	 * spawn template
	 * instance id
	 * @return 重生后的可见对象，不可重生时为 null / respawned object, or null if not allowed
	 */
	private static final VisibleObject respawn(SpawnTemplate spawnTemplate, final int instanceId) {
		if (spawnTemplate.isTemporarySpawn() && !spawnTemplate.getTemporarySpawn().canSpawn()
				&& !spawnTemplate.getTemporarySpawn().isInSpawnTime()) {
			return null;
		}
		int worldId = spawnTemplate.getWorldId();
		boolean instanceExists = InstanceService.isInstanceExist(worldId, instanceId);
		if (spawnTemplate.isNoRespawn() || !instanceExists) {
			return null;
		}

		if (spawnTemplate.hasPool()) {
			spawnTemplate = spawnTemplate.changeTemplate(instanceId);
		}
		return SpawnEngine.spawnObject(spawnTemplate, instanceId);
	}

	/**
	 * 尸体消散任务，到时删除对应可见对象。
	 * Decay task that deletes the corresponding visible object when due.
	 */
	private static class DecayTask implements Runnable {

		private final int npcId;

		DecayTask(int npcId) {
			this.npcId = npcId;
		}

		@Override
		public void run() {
			VisibleObject visibleObject = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(npcId);
			if (visibleObject != null) {
				visibleObject.getController().onDelete();
			}
		}
	}

	/**
	 * 重生任务，取消旧 RESPAWN 任务后按模板重生。
	 * Respawn task that cancels the old RESPAWN task then respawns from the template.
	 */
	private static class RespawnTask implements Runnable {

		private final SpawnTemplate spawn;
		private final int instanceId;

		RespawnTask(SpawnTemplate spawn, int instanceId) {
			this.spawn = spawn;
			this.instanceId = instanceId;
		}

		@Override
		public void run() {
			VisibleObject visibleObject = spawn.getVisibleObject();
			if (visibleObject != null && visibleObject instanceof Npc) {
				((Npc) visibleObject).getController().cancelTask(TaskId.RESPAWN);
			}
			respawn(spawn, instanceId);
		}
	}
}
