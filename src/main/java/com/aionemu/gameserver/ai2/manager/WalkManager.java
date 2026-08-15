package com.aionemu.gameserver.ai2.manager;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.awt.Point;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.bounding.BoundingBox;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.BoundRadius;
import com.aionemu.gameserver.model.templates.walker.RouteStep;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * NPC 行走管理器：路径巡航与随机游荡、到达点切换、卡住/Z 轴校正与停止行走。
 * NPC walk manager: route patrol and random wander, waypoint transitions, stuck/Z correction, and walk stop.
 *
 * @author ATracer
 */
public class WalkManager {

	private static final int WALK_RANDOM_RANGE = 5;

	private static final int MAX_WALK_ATTEMPTS = 10;
	private static final Map<Integer, Integer> walkAttemptCounts = new ConcurrentHashMap<Integer, Integer>();

	// ========== Z 值检查相关常量 ==========
	private static final int Z_CHECK_INTERVAL = 10000;
	private static final float Z_TOLERANCE = 1.0f;
	private static final float STUCK_DISTANCE_THRESHOLD = 0.5f;
	private static final int STUCK_CHECK_COUNT = 3;

	// ========== Z 值检查相关集合 ==========
	private static final Map<Integer, Npc> randomWalkingNpcs = new ConcurrentHashMap<Integer, Npc>();
	private static final Map<Integer, Float> lastCheckPositions = new ConcurrentHashMap<Integer, Float>();
	private static final Map<Integer, Integer> stuckCounters = new ConcurrentHashMap<Integer, Integer>();

	private static final Map<Integer, ScheduledFuture<?>> pendingWalkTasks = new ConcurrentHashMap<Integer, ScheduledFuture<?>>();

	private static Future<?> zCheckTask = null;

	static {
		startZCheckTask();
	}

	/**
	 * 开始行走：优先路径巡航，否则尝试随机游荡。
	 * Starts walking: prefers route patrol, otherwise attempts random wander.
	 *
	 * NPC AI instance
	 *
	 * @param npcAI
	 * @return 成功进入行走时为 {@code true} / {@code true} if walking started
	 */
	public static boolean startWalking(NpcAI2 npcAI) {
		cancelPendingTask(npcAI.getOwner().getObjectId());

		npcAI.setStateIfNot(AIState.WALKING);
		Npc owner = npcAI.getOwner();
		WalkerTemplate template = DataManager.WALKER_DATA.getWalkerTemplate(owner.getSpawn().getWalkerId());
		if (template != null) {
			npcAI.setSubStateIfNot(AISubState.WALK_PATH);
			startRouteWalking(npcAI, owner, template);
		} else {
			return startRandomWalking(npcAI, owner);
		}
		return true;
	}

	private static boolean startRandomWalking(NpcAI2 npcAI, Npc owner) {
		return startRandomWalking(npcAI, owner, false);
	}

	public static boolean startRandomWalking(NpcAI2 npcAI) {
		stopWalking(npcAI);
		npcAI.setStateIfNot(AIState.WALKING);
		return startRandomWalking(npcAI, npcAI.getOwner(), true);
	}

	private static boolean startRandomWalking(NpcAI2 npcAI, Npc owner, boolean forced) {
		if (!AIConfig.ACTIVE_NPC_MOVEMENT) {
			return false;
		}
		int randomWalkNr = owner.getSpawn().getRandomWalk();
		if (!forced && randomWalkNr == 0) {
			return false;
		}
		if (npcAI.setSubStateIfNot(AISubState.WALK_RANDOM)) {
			randomWalkingNpcs.put(owner.getObjectId(), owner);
			EmoteManager.emoteStartWalking(npcAI.getOwner());
			chooseNextRandomPoint(npcAI);
			return true;
		}
		return false;
	}

	protected static void startRouteWalking(NpcAI2 npcAI, Npc owner, WalkerTemplate template) {
		if (!AIConfig.ACTIVE_NPC_MOVEMENT)
			return;
		List<RouteStep> route = template.getRouteSteps();
		int currentPoint = owner.getMoveController().getCurrentPoint();
		RouteStep nextStep = findNextRoutStep(owner, route);
		owner.getMoveController().setCurrentRoute(route);
		owner.getMoveController().setRouteStep(nextStep, route.get(currentPoint));
		EmoteManager.emoteStartWalking(npcAI.getOwner());
		npcAI.getOwner().getMoveController().moveToNextPoint();
	}

	/** 移动到指定路径点；到达后不自动选择下一点。 */
	public static boolean startWalkingToWaypoint(NpcAI2 npcAI, WalkerTemplate template, int waypoint) {
		if (!AIConfig.ACTIVE_NPC_MOVEMENT || !npcAI.isMoveSupported() || template == null
			|| template.getRouteSteps() == null || waypoint < 0 || waypoint >= template.getRouteSteps().size()) {
			return false;
		}
		Npc owner = npcAI.getOwner();
		List<RouteStep> route = template.getRouteSteps();
		cancelPendingTask(owner.getObjectId());
		walkAttemptCounts.remove(owner.getObjectId());
		randomWalkingNpcs.remove(owner.getObjectId());
		owner.getMoveController().abortMove();
		npcAI.setStateIfNot(AIState.WALKING);
		npcAI.setSubStateIfNot(AISubState.WALK_PATH);
		owner.getMoveController().setCurrentRoute(route);
		owner.getMoveController().setRouteStep(route.get(waypoint), route.get(Math.max(0, waypoint - 1)));
		owner.getMoveController().moveToNextPoint();
		return true;
	}

	protected static RouteStep findNextRoutStep(Npc owner, List<RouteStep> route) {
		int currentPoint = owner.getMoveController().getCurrentPoint();
		RouteStep nextStep = null;
		if (currentPoint != 0) {
			nextStep = findNextRouteStepAfterPause(owner, route, currentPoint);
		} else {
			nextStep = findClosestRouteStep(owner, route, nextStep);
		}
		return nextStep;
	}

	protected static RouteStep findClosestRouteStep(Npc owner, List<RouteStep> route, RouteStep nextStep) {
		double closestDist = 0;
		float x = owner.getX();
		float y = owner.getY();
		float z = owner.getZ();

		if (owner.getWalkerGroup() != null) {
			if (owner.getWalkerGroup().getGroupStep() < 2) {
				nextStep = route.get(0);
			} else {
				nextStep = route.get(owner.getWalkerGroup().getGroupStep() - 1);
			}
		} else {
			for (RouteStep step : route) {
				double stepDist = MathUtil.getDistance(x, y, z, step.getX(), step.getY(), step.getZ());
				if (closestDist == 0 || stepDist < closestDist) {
					closestDist = stepDist;
					nextStep = step;
				}
			}
		}
		return nextStep;
	}

	protected static RouteStep findNextRouteStepAfterPause(Npc owner, List<RouteStep> route, int currentPoint) {
		RouteStep nextStep = route.get(currentPoint);
		double stepDist = MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(), nextStep.getX(), nextStep.getY(), nextStep.getZ());
		if (stepDist < 1) {
			nextStep = nextStep.getNextStep();
		}
		return nextStep;
	}

	/**
	 * 判断 NPC 是否应进入行走（支持移动且有路径或为可攻击 NPC）。
	 * Returns whether the NPC should walk (move supported and has routes or is attackable).
	 *
	 * NPC AI instance
	 *
	 * @param npcAI {@code true} if walking is applicable。
	 */
	public static boolean isWalking(NpcAI2 npcAI) {
		return npcAI.isMoveSupported() && (hasWalkRoutes(npcAI) || npcAI.getOwner().isAttackableNpc());
	}

	/**
	 * 判断 NPC 是否配置了行走路径。
	 * Returns whether the NPC has walk routes configured.
	 *
	 * NPC AI instance
	 *
	 * @param npcAI {@code true} if walk routes exist。
	 */
	public static boolean hasWalkRoutes(NpcAI2 npcAI) {
		return npcAI.getOwner().hasWalkRoutes();
	}

	private static void cancelPendingTask(int npcObjectId) {
		ScheduledFuture<?> task = pendingWalkTasks.remove(npcObjectId);
		if (task != null && !task.isDone()) {
			task.cancel(false);
		}
	}

	private static boolean isNpcRegionActive(Npc npc) {
		if (npc == null || !npc.isSpawned()) {
			return false;
		}
		try {
			return npc.getPosition().getMapRegion().isMapRegionActive();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 到达路径点/随机点后的处理：切换下一步、等待组或中止交谈移动。
	 * Handles arrival at a route/random point: next step, wait-group, or abort on talk.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void targetReached(final NpcAI2 npcAI) {
		int npcObjectId = npcAI.getOwner().getObjectId();
		walkAttemptCounts.remove(npcObjectId);

		if (npcAI.isInState(AIState.WALKING)) {
			switch (npcAI.getSubState()) {
			case WALK_PATH:
				npcAI.getOwner().updateKnownlist();
				if (npcAI.getOwner().getWalkerGroup() != null) {
					npcAI.getOwner().getWalkerGroup().targetReached(npcAI);
				} else {
					chooseNextRouteStep(npcAI);
				}
				break;
			case WALK_WAIT_GROUP:
				npcAI.setSubStateIfNot(AISubState.WALK_PATH);
				chooseNextRouteStep(npcAI);
				break;
			case WALK_RANDOM:
				chooseNextRandomPoint(npcAI);
				break;
			case TALK:
				npcAI.getOwner().getMoveController().abortMove();
				break;
			default:
				break;
			}
		}
	}

	protected static void chooseNextRouteStep(final NpcAI2 npcAI) {
		int npcObjectId = npcAI.getOwner().getObjectId();

		Integer attemptCount = walkAttemptCounts.get(npcObjectId);
		if (attemptCount == null) {
			attemptCount = 0;
		}

		if (attemptCount >= MAX_WALK_ATTEMPTS) {
			stopWalking(npcAI);
			walkAttemptCounts.remove(npcObjectId);
			return;
		}

		walkAttemptCounts.put(npcObjectId, attemptCount + 1);

		int walkPause = npcAI.getOwner().getMoveController().getWalkPause();
		if (walkPause == 0) {
			npcAI.getOwner().getMoveController().resetMove();
			npcAI.getOwner().getMoveController().chooseNextStep();
			npcAI.getOwner().getMoveController().moveToNextPoint();
		} else {
			npcAI.getOwner().getMoveController().abortMove();
			npcAI.getOwner().getMoveController().chooseNextStep();

			ScheduledFuture<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					pendingWalkTasks.remove(npcAI.getOwner().getObjectId());
					if (isNpcRegionActive(npcAI.getOwner()) && npcAI.isInState(AIState.WALKING)) {
						npcAI.getOwner().getMoveController().moveToNextPoint();
					}
				}
			}, walkPause);
			pendingWalkTasks.put(npcObjectId, task);
		}
	}

	private static void chooseNextRandomPoint(final NpcAI2 npcAI) {
		final Npc owner = npcAI.getOwner();
		final int npcObjectId = owner.getObjectId();

		owner.getMoveController().abortMove();

		cancelPendingTask(npcObjectId);

		int randomWalkNr = owner.getSpawn().getRandomWalk();
		final int walkRange = Math.max(randomWalkNr, WALK_RANDOM_RANGE);
		final boolean outsideWalkRange = isOutsideRandomWalkRange(owner.getX(), owner.getY(),
			owner.getSpawn().getX(), owner.getSpawn().getY(), walkRange);

		ScheduledFuture<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				pendingWalkTasks.remove(npcObjectId);

				if (!isNpcRegionActive(owner)) {
					return;
				}

				if (!npcAI.isInState(AIState.WALKING)) {
					return;
				}

				if (outsideWalkRange) {
					owner.getMoveController().moveToPoint(owner.getSpawn().getX(), owner.getSpawn().getY(),
						owner.getSpawn().getEffectiveZ());
				} else {
					int maxAttempts = 5;
					int attempts = 0;

					while (attempts < maxAttempts) {
						Point randomPoint = MathUtil.get2DPointInsideCircle(owner.getSpawn().getX(), owner.getSpawn().getY(), walkRange);
						float targetX = randomPoint.x;
						float targetY = randomPoint.y;

						if (!isTargetPointValid(owner, targetX, targetY, owner.getZ())) {
							attempts++;
							continue;
						}

						float targetZ = owner.getZ();

						if (GeoDataConfig.GEO_ENABLE && GeoDataConfig.GEO_NPC_MOVE && !owner.isFlying()) {
							try {
								targetZ = GameWorldServices.geoService().getZ(owner.getWorldId(), targetX, targetY, owner.getZ(), 0.5F, owner.getInstanceId());
							} catch (Exception e) {
								targetZ = owner.getSpawn().getEffectiveZ();
							}
						}

						if (GeoDataConfig.GEO_ENABLE && GeoDataConfig.GEO_NPC_MOVE) {
							BoundRadius radius = owner.getObjectTemplate().getBoundRadius();
							byte flags = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId() | CollisionIntention.WALK.getId());
							Vector3f loc = GameWorldServices.geoService().getClosestCollision(owner, targetX, targetY, targetZ, true, flags);

							if (loc != null && (Math.abs(loc.x - targetX) > 0.5f || Math.abs(loc.y - targetY) > 0.5f)) {
								owner.getMoveController().moveToPoint(loc.x, loc.y, loc.z);
								break;
							} else if (loc != null) {
								owner.getMoveController().moveToPoint(targetX, targetY, targetZ);
								break;
							}
						} else {
							owner.getMoveController().moveToPoint(targetX, targetY, targetZ);
							break;
						}

						attempts++;
					}

					if (attempts >= maxAttempts) {
						owner.getMoveController().moveToPoint(owner.getSpawn().getX(), owner.getSpawn().getY(),
							owner.getSpawn().getEffectiveZ());
					}
				}
			}
		}, Rnd.get(AIConfig.MINIMIMUM_DELAY, AIConfig.MAXIMUM_DELAY) * 1000);

		pendingWalkTasks.put(npcObjectId, task);
	}

	static boolean isOutsideRandomWalkRange(float ownerX, float ownerY, float spawnX, float spawnY, float walkRange) {
		return MathUtil.getDistance(ownerX, ownerY, spawnX, spawnY) > walkRange;
	}

	/**
	 * 停止行走：取消挂起任务、清理状态并中止移动。
	 * Stops walking: cancels pending tasks, cleans state, and aborts movement.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void stopWalking(NpcAI2 npcAI) {
		int npcObjectId = npcAI.getOwner().getObjectId();

		cancelPendingTask(npcObjectId);

		walkAttemptCounts.remove(npcObjectId);
		randomWalkingNpcs.remove(npcObjectId);
		lastCheckPositions.remove(npcObjectId);
		stuckCounters.remove(npcObjectId);

		npcAI.getOwner().getMoveController().abortMove();
		npcAI.setStateIfNot(AIState.IDLE);
		npcAI.setSubStateIfNot(AISubState.NONE);
		EmoteManager.emoteStopWalking(npcAI.getOwner());
	}

	private static boolean isTerrainReachableByAngle(Npc owner, float x, float y, float currentZ, float targetZ) {
		float deltaX = x - owner.getX();
		float deltaY = y - owner.getY();
		float horizontalDistance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

		float verticalDistance = Math.abs(targetZ - currentZ);

		if (horizontalDistance <= 0.1f) {
			return true;
		}

		double angleRadians = Math.atan(verticalDistance / horizontalDistance);
		double angleDegrees = Math.toDegrees(angleRadians);

		return angleDegrees <= 45.0;
	}

	private static boolean isTargetPointValid(Npc owner, float x, float y, float z) {
		if (GeoDataConfig.GEO_ENABLE && GeoDataConfig.GEO_NPC_MOVE) {
			try {
				float actualZ = GameWorldServices.geoService().getZ(owner.getWorldId(), x, y, z, 0.5F, owner.getInstanceId());

				if (!isTerrainReachableByAngle(owner, x, y, owner.getZ(), actualZ)) {
					return false;
				}

				byte flags = (byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.WALK.getId());
				Vector3f loc = GameWorldServices.geoService().getClosestCollision(owner, x, y, actualZ, true, flags);

				if (loc != null && (Math.abs(loc.x - x) > 1.0f || Math.abs(loc.y - y) > 1.0f)) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 判断移动控制器是否已到达当前点。
	 * Returns whether the move controller has reached the current point.
	 *
	 * NPC AI instance
	 *
	 * @param npcAI {@code true} if arrived。
	 */
	public static boolean isArrivedAtPoint(NpcAI2 npcAI) {
		return npcAI.getOwner().getMoveController().isReachedPoint();
	}

	private static void startZCheckTask() {
		if (zCheckTask != null && !zCheckTask.isDone()) {
			return;
		}

		zCheckTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				performZCheck();
			}
		}, Z_CHECK_INTERVAL, Z_CHECK_INTERVAL);
	}

	private static void performZCheck() {
		if (!GeoDataConfig.GEO_ENABLE || !GeoDataConfig.GEO_NPC_MOVE) {
			return;
		}

		if (randomWalkingNpcs.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<Integer, Npc>> iterator = randomWalkingNpcs.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Npc> entry = iterator.next();
			Npc npc = entry.getValue();

			try {
				if (npc == null || !npc.isSpawned()) {
					iterator.remove();
					cleanupNpcData(entry.getKey());
					continue;
				}

				if (!npc.getMoveController().isInMove()) {
					continue;
				}

				float currentX = npc.getX();
				float currentY = npc.getY();
				float currentZ = npc.getZ();

				checkNpcStuck(npc, currentX, currentY);

				float geoZ = getValidGeoZ(npc, currentX, currentY, currentZ);

				if (Float.isNaN(geoZ) || geoZ < -1000f) {
					teleportToSpawnPoint(npc);
					iterator.remove();
					cleanupNpcData(entry.getKey());
					continue;
				}

				float zDiff = Math.abs(currentZ - geoZ);
				if (zDiff > Z_TOLERANCE) {
					correctNpcZ(npc, currentX, currentY, geoZ);
				}

			} catch (Exception e) {
				// 忽略 / ignore
			}
		}
	}

	private static float getValidGeoZ(Npc npc, float x, float y, float currentZ) {
		try {
			return GameWorldServices.geoService().getZ(npc.getWorldId(), x, y, currentZ, 0.5F, npc.getInstanceId());
		} catch (Exception e) {
			return Float.NaN;
		}
	}

	private static void checkNpcStuck(Npc npc, float currentX, float currentY) {
		int npcId = npc.getObjectId();

		Float lastPosX = lastCheckPositions.get(npcId);
		if (lastPosX != null) {
			float lastX = lastPosX;
			float lastY = lastCheckPositions.get(npcId + 1000000);

			float distance = (float) Math.sqrt((currentX - lastX) * (currentX - lastX) + (currentY - lastY) * (currentY - lastY));

			if (distance < STUCK_DISTANCE_THRESHOLD) {
				Integer stuckCount = stuckCounters.get(npcId);
				if (stuckCount == null) {
					stuckCount = 0;
				}
				stuckCount++;

				if (stuckCount >= STUCK_CHECK_COUNT) {
					teleportToSpawnPoint(npc);
					randomWalkingNpcs.remove(npcId);
					cleanupNpcData(npcId);
					return;
				}
				stuckCounters.put(npcId, stuckCount);
			} else {
				stuckCounters.remove(npcId);
			}
		}

		lastCheckPositions.put(npcId, currentX);
		lastCheckPositions.put(npcId + 1000000, currentY);
	}

	private static void correctNpcZ(Npc npc, float x, float y, float correctZ) {
		npc.getPosition().setZ(correctZ);
		PacketSendUtility.broadcastPacket(npc, new SM_MOVE(npc));
	}

	private static void teleportToSpawnPoint(Npc npc) {
		if (npc == null || npc.getSpawn() == null) {
			return;
		}

		cancelPendingTask(npc.getObjectId());

		float spawnX = npc.getSpawn().getX();
		float spawnY = npc.getSpawn().getY();
		float spawnZ = npc.getSpawn().getEffectiveZ();

		npc.getMoveController().abortMove();
		npc.getPosition().setXYZH(spawnX, spawnY, spawnZ, npc.getSpawn().getHeading());
		PacketSendUtility.broadcastPacket(npc, new SM_MOVE(npc));
	}

	private static void cleanupNpcData(int npcId) {
		lastCheckPositions.remove(npcId);
		lastCheckPositions.remove(npcId + 1000000);
		stuckCounters.remove(npcId);
		walkAttemptCounts.remove(npcId);
		cancelPendingTask(npcId);
	}
}
