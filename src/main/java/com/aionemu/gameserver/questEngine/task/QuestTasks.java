package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;
import com.aionemu.gameserver.questEngine.definition.QuestLureCompletion;
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
	 * @param npc 跟随 NPC / following NPC
	 * @param target 目标 NPC / target NPC
	 * @return 定时任务句柄 / scheduled future
	 */
	public static final Future<?> newFollowingToTargetCheckTask(final QuestEnv env, Npc npc, Npc target) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
				new FollowingNpcCheckTask(env, new TargetDestinationChecker(npc, target)), 1000, 1000);
	}

	/**
	 * 调度跟随至指定 NPC 模板存活目标或刷新点的周期检查任务。
	 * Schedules a periodic follow-check task toward a living NPC or its spawn point.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param npc 跟随 NPC / following NPC
	 * @param npcTargetId 目标 NPC 模板 ID / target NPC template id
	 * @return 定时任务句柄 / scheduled future
	 * @throws IllegalArgumentException 目标 NPC 在地图中无刷新点时抛出 / if the target NPC has no spawn in the map
	 */
	public static final Future<?> newFollowingToTargetCheckTask(final QuestEnv env, Npc npc, int npcTargetId) {
		Npc target = findLivingTarget(npc, npcTargetId);
		if (target != null) {
			return newFollowingToTargetCheckTask(env, npc, target);
		}
		SpawnSearchResult searchResult = DataManager.SPAWNS_DATA2.getFirstSpawnByNpcId(npc.getWorldId(), npcTargetId);
		if (searchResult == null) {
			throw new IllegalArgumentException("Supplied npc doesn't exist: " + npcTargetId);
		}
		return GameThreadPoolServices.threadPoolManager()
				.scheduleAtFixedRate(new FollowingNpcCheckTask(env, new CoordinateDestinationChecker(npc,
					searchResult.getSpot().getX(), searchResult.getSpot().getY(), searchResult.getSpot().getZ())), 1000, 1000);
	}

	private static Npc findLivingTarget(Npc follower, int npcTargetId) {
		if (follower == null || follower.getPosition() == null || follower.getPosition().getWorldMapInstance() == null) {
			return null;
		}
		for (Npc target : follower.getPosition().getWorldMapInstance().getNpcs(npcTargetId)) {
			if (target != null && target.isSpawned()
				&& (target.getLifeStats() == null || !target.getLifeStats().isAlreadyDead())) {
				return target;
			}
		}
		return null;
	}

	/**
	 * 调度跟随至指定坐标的周期检查任务。
	 * Schedules a periodic follow-check task toward the given coordinates.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param npc 跟随 NPC / following NPC
	 * @param x 目标 X / Target X
	 * @param y 目标 Y / Target Y
	 * @param z 目标 Z / Target Z
	 * @return 定时任务句柄 / scheduled future
	 */
	public static final Future<?> newFollowingToTargetCheckTask(final QuestEnv env, Npc npc, float x, float y,
			float z) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
				new FollowingNpcCheckTask(env, new CoordinateDestinationChecker(npc, x, y, z)), 1000, 1000);
	}

	/**
	 * 监视可攻击常驻 NPC 依靠仇恨追击被诱导到指定坐标，不改变其战斗 AI。
	 * Watches an attackable resident NPC being lured by combat aggro to a coordinate without changing its AI.
	 */
	public static Future<?> newLuredNpcToCoordinateCheckTask(QuestEnv env, Npc npc,
			float x, float y, float z, float radius) {
		return newLuredNpcToCoordinateCheckTask(env, npc, x, y, z, radius, QuestLureCompletion.DELETE);
	}

	/**
	 * 监视诱导 NPC 到达指定坐标，并按显式策略完成 NPC 世界副作用。
	 * Watches a lured NPC reach a coordinate and applies the explicit NPC world-side completion effect.
	 *
	 * @param env 任务环境 / quest environment
	 * @param npc 被诱导的 NPC / lured NPC
	 * @param x 目标 X / target X
	 * @param y 目标 Y / target Y
	 * @param z 目标 Z / target Z
	 * @param radius 到达半径 / arrival radius
	 * @param completion 到达后的完成策略 / completion effect after arrival
	 * @return 定时任务句柄 / scheduled task handle
	 */
	public static Future<?> newLuredNpcToCoordinateCheckTask(QuestEnv env, Npc npc,
			float x, float y, float z, float radius, QuestLureCompletion completion) {
		LuredNpcCheckTask checker = new LuredNpcCheckTask(env, npc, x, y, z, radius, completion);
		Future<?> task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(checker, 500, 500);
		checker.bind(task);
		return task;
	}

	/**
	 * 调度跟随至指定区域的周期检查任务。
	 * Schedules a periodic follow-check task toward the given zone.
	 *
	 * @param env 任务环境 / Quest environment
	 * @param npc 跟随 NPC / following NPC
	 * @param zoneName 目标区域 / target zone
	 * @return 定时任务句柄 / scheduled future
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
	 * @param npc 跟随 NPC / following NPC
	 * @param zoneName1 区域 1 / zone 1
	 * @param zoneName2 区域 2 / zone 2
	 * @return 定时任务句柄 / scheduled future
	 */
	public static final Future<?> newFollowingToTargetCheckTask(final QuestEnv env, Npc npc, ZoneName zoneName1,
			ZoneName zoneName2) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
				new FollowingNpcCheckTask(env, new ZoneChecker2(npc, zoneName1, zoneName2)), 1000, 1000);
	}
}
