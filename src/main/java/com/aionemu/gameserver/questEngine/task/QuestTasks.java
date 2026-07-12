package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 任务跟随检查任务工厂，按不同目的地类型调度 {@link FollowingNpcCheckTask}。
 * Factory for quest follow-check tasks, scheduling {@link FollowingNpcCheckTask} for various destination types.
 *
 * @author ATracer
 */
public class QuestTasks {

	/**
	 * 调度跟随至目标 NPC 的周期检查任务。
	 * Schedules a periodic follow-check task toward a target NPC.
	 *
	 * @param env 任务环境 / Quest environment
	 * Following NPC
	 * Target NPC
	 * Scheduled future
	 */
	public static final Future<?> newFollowingToTargetCheckTask(final QuestEnv env, Npc npc, Npc target) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
				new FollowingNpcCheckTask(env, new TargetDestinationChecker(npc, target)), 1000, 1000);
	}

	/**
	 * 调度跟随至指定 NPC 模板首个刷新点坐标的周期检查任务。
	 * Schedules a periodic follow-check task toward the first spawn coordinates of an NPC template.
	 *
	 * @param env 任务环境 / Quest environment
	 * Following NPC
	 * Target NPC template id
	 * Scheduled future
	 * if the target NPC has no spawn in the map。
	 */
	public static final Future<?> newFollowingToTargetCheckTask(final QuestEnv env, Npc npc, int npcTargetId) {
		SpawnSearchResult searchResult = DataManager.SPAWNS_DATA2.getFirstSpawnByNpcId(npc.getWorldId(), npcTargetId);
		if (searchResult == null) {
			throw new IllegalArgumentException("Supplied npc doesn't exist: " + npcTargetId);
		}
		return GameThreadPoolServices.threadPoolManager()
				.scheduleAtFixedRate(new FollowingNpcCheckTask(env, new CoordinateDestinationChecker(npc,
						searchResult.getSpot().getX(), searchResult.getSpot().getY(), searchResult.getSpot().getZ())),
						1000, 1000);
	}

	/**
	 * 调度跟随至指定坐标的周期检查任务。
	 * Schedules a periodic follow-check task toward the given coordinates.
	 *
	 * @param env 任务环境 / Quest environment
	 * Following NPC
	 * @param x 目标 X / Target X
	 * @param y 目标 Y / Target Y
	 * @param z 目标 Z / Target Z
	 * Scheduled future
	 */
	public static final Future<?> newFollowingToTargetCheckTask(final QuestEnv env, Npc npc, float x, float y,
			float z) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
				new FollowingNpcCheckTask(env, new CoordinateDestinationChecker(npc, x, y, z)), 1000, 1000);
	}

	/**
	 * 调度跟随至指定区域的周期检查任务。
	 * Schedules a periodic follow-check task toward the given zone.
	 *
	 * @param env 任务环境 / Quest environment
	 * Following NPC
	 * Target zone
	 * Scheduled future
	 */
	public static final Future<?> newFollowingToTargetCheckTask(final QuestEnv env, Npc npc, ZoneName zoneName) {
		return GameThreadPoolServices.threadPoolManager()
				.scheduleAtFixedRate(new FollowingNpcCheckTask(env, new ZoneChecker(npc, zoneName)), 1000, 1000);
	}

	/**
	 * 调度跟随至双区域之一的周期检查任务。
	 * Schedules a periodic follow-check task toward either of two zones.
	 *
	 * @param env 任务环境 / Quest environment
	 * Following NPC
	 * Zone 1
	 * Zone 2
	 * Scheduled future
	 */
	public static final Future<?> newFollowingToTargetCheckTask(final QuestEnv env, Npc npc, ZoneName zoneName1,
			ZoneName zoneName2) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
				new FollowingNpcCheckTask(env, new ZoneChecker2(npc, zoneName1, zoneName2)), 1000, 1000);
	}
}
