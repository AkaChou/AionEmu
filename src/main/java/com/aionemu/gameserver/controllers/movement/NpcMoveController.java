package com.aionemu.gameserver.controllers.movement;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.handler.TargetEventHandler;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.actions.NpcActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.walker.RouteStep;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;
import com.aionemu.gameserver.model.templates.zone.Point2D;
import com.aionemu.gameserver.movement.Global;
import com.aionemu.gameserver.movement.processors.movement.motor.FollowMotor;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.spawnengine.WalkerFormator;
import com.aionemu.gameserver.spawnengine.WalkerGroup;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.collections.LastUsedCache;
import com.aionemu.gameserver.world.geo.path.PathService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.LongAdder;

/**
 * NPC 移动控制器：目标追踪、寻路缓存、巡逻路径、回家与跟随马达。
 * NPC move controller: target tracking, path cache, walk routes, return-home, and follow motor.
 */
@Slf4j
public class NpcMoveController
        extends CreatureMoveController<Npc> {
    /** 移动检测偏移阈值 / Move check offset threshold */
    public static final float MOVE_CHECK_OFFSET = 0.1f;
    /** 内部移动偏移 / Internal move offset */
    private static final float MOVE_OFFSET = 0.05f;
    /** 追击目标相对路径终点偏移超过该值则重寻。 / Repath chase when target drifts this far from path end. */
    static final float CHASE_REPATH_DISTANCE = 2.0f;
    private static final long CHASE_REPATH_NEAR_INTERVAL_MS = 500;
    private static final long CHASE_REPATH_MID_INTERVAL_MS = 750;
    private static final long CHASE_REPATH_FAR_INTERVAL_MS = 1_000;
    /** 同起点/终点的可达性结果复用窗口。 / Reuse reach checks for the same endpoints this long. */
    private static final long REACH_CHECK_CACHE_MS = 100;
    private static final long PATH_RETRY_DELAY_MS = 500;
    private static final long PATH_FAILURE_REACTION_DELAY_MS = 10_000;
    private static final long PATH_AVOIDANCE_INTERVAL_MS = 1_000;
    private static final int PATH_AVOIDANCE_MAX_ATTEMPTS = 4;
    private static final float PATH_AVOIDANCE_STEP = 1.5f;
    private static final long HOME_RETURN_TIMEOUT_MS = 60_000;
    private static final long HOME_SP_RETURN_TIMEOUT_MS = 30_000;
    private static final long CHASE_MOVE_BROADCAST_INTERVAL_MS = 200;
    private static final long WAYPOINT_SKIP_INTERVAL_MS = 250;
    private static final long STUCK_SAMPLE_INTERVAL_MS = 500;
    private static final long STUCK_SAMPLE_MAX_DELAY_MS = 1_500;
    private static final float STUCK_MIN_PROGRESS = 0.10f;
    private static final float STUCK_EXPECTED_PROGRESS_RATIO = 0.15f;
    private static final int STUCK_SUSPECTED_WINDOWS = 2;
    private static final int STUCK_CONFIRMED_WINDOWS = 3;
    private static final long STUCK_REPLAN_COOLDOWN_MS = 750;
    private static final long STUCK_BLOCKED_SEGMENT_TTL_MS = 5_000;
    private static final int STUCK_REPLAN_MAX_ATTEMPTS = 2;
    private static final float TARGET_SLOT_RECALC_DISTANCE = 1;
    private static final float TARGET_SLOT_MAX_ATTACK_RANGE = 4;
    private static final float NEAREST_PATH_RECOVERY_RADIUS = 2;
    private static final float NEAREST_PATH_RECOVERY_VERTICAL = 0.7f;
    private static final float LOCAL_AVOIDANCE_HEIGHT_TOLERANCE = 0.25f;
    private static final int[] TARGET_SLOT_ADJUSTMENTS = {0, 20, -20, 40, -40};
    private static final LongAdder stuckSuspected = new LongAdder();
    private static final LongAdder stuckConfirmed = new LongAdder();
    private static final LongAdder stuckSelfRecovered = new LongAdder();
    private static final LongAdder stuckReplanAttempts = new LongAdder();
    private static final LongAdder stuckReplanFound = new LongAdder();
    private static final LongAdder stuckReplanFailed = new LongAdder();
    private static final LongAdder waypointSkipAttempts = new LongAdder();
    private static final LongAdder waypointSkipSuccess = new LongAdder();
    private static final LongAdder nearestNodeAttempts = new LongAdder();
    private static final LongAdder nearestNodeSuccess = new LongAdder();
    /** 当前目的地类型 / Current destination type */
    private Destination destination = Destination.TARGET_OBJECT;
    /** Point X / Point X */
    private float pointX;
    /** Point Y / Point Y */
    private float pointY;
    /** Point Z / Point Z */
    private float pointZ;
    /** 历史回退步缓存 / Back-step cache */
    private LastUsedCache<Byte, Point3D> lastSteps = null;
    /** 步序号 / Step sequence number */
    private byte stepSequenceNr = 0;
    /** 停止偏移 / Stop offset */
    private float offset = 0.1f;
    /** 当前巡逻路线 / Current walk route */
    List<RouteStep> currentRoute;
    /** 当前路线点索引 / Current route point index */
    int currentPoint;
    /** 路线点停顿毫秒 / Route-step rest time ms */
    int walkPause;
    /** 上次因运行时碰撞重规划时间 / Last runtime-collision replan time */
    private long lastPathReplan;
    /** 最近一次 canReach 检测缓存。 / Last canReach check cache. */
    private float reachFromX = Float.NaN, reachFromY, reachFromZ;
    private float reachToX = Float.NaN, reachToY, reachToZ;
    private boolean reachResult;
    private long reachCheckedAt;
    /** 创建缓存路径时的动态障碍版本。 / Dynamic-obstacle version used by the cached path. */
    private long cachedObstacleVersion;
    /** 缓存路径是否有效 / Whether cached path is valid */
    private volatile boolean cachedPathValid;
    /** 缓存 PATH 路径 / Cached PATH route */
    private float[][] cachedPath;
    /** 缓存路径的目标对象 ID，坐标路径为 0。 / Target object ID of the cached route; 0 for locations. */
    private int cachedPathTargetId;
    /** 正在后台计算的 PATH 路径 / PATH route currently being computed */
    private volatile CompletableFuture<float[][]> pendingPath;
    /** 在途目标对象 ID，坐标路径为 0。 / Target object ID for the pending request; 0 for locations. */
    private int pendingPathTargetId;
    /** 在途请求实际提交的目标坐标。 / Destination coordinates captured when the request was submitted. */
    private float pendingPathX = Float.NaN, pendingPathY, pendingPathZ;
    /** 在途请求提交时间。 / Time when the pending request was submitted. */
    private long pendingPathStartedAt;
    /** 当前请求代际与在途请求快照。 / Current request generation and pending snapshot. */
    private long pathRequestId, pendingPathRequestId, pendingPathObstacleVersion;
    /** 瞬时调度失败后的下一次重试时间。 / Earliest retry time after a transient scheduling failure. */
    private long pathRetryAt;
    /** 当前 PATH 等待阶段是否已经广播停止。 / Whether stop was already broadcast for the current PATH wait. */
    private boolean pathStopSent;
    /** 上次移动目的地是否为中间 PATH 路点。 / Whether the previous destination was an intermediate PATH waypoint. */
    private boolean previousIntermediateWaypoint;
    private long lastMoveBroadcastAt;
    /** 最近一次确定无路的目标坐标。 / Last destination that was confirmed unreachable. */
    private float failedPathX = Float.NaN, failedPathY, failedPathZ;
    /** 首次连续寻路失败时间。 / First failure time in the current consecutive failure period. */
    private long firstPathFailureAt;
    /** 失败时的动态障碍版本。 / Dynamic-obstacle version when the path failed. */
    private long failedPathObstacleVersion;
    /** 当前连续失败是否已经进入失败策略。 / Whether the current failure period already entered its policy. */
    private boolean pathFailureHandled;
    private long lastPathAvoidanceAt;
    private int pathAvoidanceAttempts;
    /** 已应用拉取策略的目标 ID。 / Target ID tracked by the pull-target failure policy. */
    private int pathPullTargetId;
    /** 对同一目标已执行的拉取次数。 / Pull attempts already used for the same target. */
    private int pathPullAttempts;
    /** 进入返回出生点状态的时间。 / Time when return-to-spawn state started. */
    private long homeReturnStartedAt;
    /** 失败策略返回完成后是否回满生命。 / Whether failure-policy return restores full HP. */
    private boolean fullHealOnHomeReturn;
    /** 数字追击超时后要返回的当前巡逻点；为空时返回出生点。 / Current waypoint used after numeric chase timeout; null means spawn. */
    private RouteStep homeReturnWaypoint;
    /** Shadow Stuck 采样状态；仅观测，不触发恢复。 / Shadow Stuck sampling state; observation only. */
    private long progressSampleAt, progressRequestId;
    private float progressX, progressY, progressZ;
    private float progressWaypointX, progressWaypointY, progressWaypointZ;
    private float progressRemainingDistance;
    private int progressPathLength, noProgressWindows;
    private boolean stuckShadowConfirmed;
    private int stuckReplanAttemptCount;
    private long lastStuckReplanAt;
    private boolean pendingPathRecovery;
    private long lastWaypointSkipAt;
    private boolean nearestRecoveryAttempted;
    private float[][] recoveryBridgePath;
    private int trackedTargetId, chaseSlotTargetId;
    private boolean chaseSlotValid;
    private int chaseSlotAdjustment;
    private float trackedTargetX = Float.NaN, trackedTargetY, trackedTargetZ;
    private float chaseSlotAnchorX, chaseSlotAnchorY, chaseSlotX, chaseSlotY, chaseSlotZ;
    /** 跟随马达 / Follow motor */
    private FollowMotor _followMotor;

    /**
     * 使用指定 NPC 构造控制器。
     * Construct the controller for the given NPC.
     *
     * NPC owner
     */
    public NpcMoveController(Npc owner) {
        super(owner);
    }

    /**
     * 移动目的地类型。
     * Destination type for movement.
     */
    private static enum Destination {
        /** 目标对象 / Target object */
        TARGET_OBJECT,
        /** 坐标点 / Point */
        POINT,
        /** 出生点 / Home/spawn */
        HOME,
    }

    /**
     * 对目标应用跟随马达；目标不变时复用。
     * Apply a follow motor to the target; reuse when target is unchanged.
     *
     * Follow target
     */
    private void applyFollow(VisibleObject target) {
        if ((this._followMotor != null && this._followMotor._target == target)) {
            return;
        }
        if (this._followMotor != null) {
            this._followMotor.stop();
        }
        this._followMotor = new FollowMotor(Global.MovementProcessor, (Npc)this.owner, target);
        this._followMotor.start();
    }

    /**
     * 取消并清理跟随马达。
     * Cancel and clear the follow motor.
     */
    private void cancelFollow() {
        if ((this._followMotor != null)) {
            this._followMotor.stop();
            this._followMotor = null;
        }
    }

    /**
     * 路径是否包含中间路点。
     * Whether the path has intermediate waypoints.
     *
     * @param path 路径点数组 / Path waypoints
     * @return 是否有中间点 / Whether intermediate exists
     */
    static boolean hasIntermediateWaypoint(float[][] path) {
        return path != null && path.length > 1;
    }

    static float pathDestinationDrift(float[][] path, float x, float y, float z) {
        if (path == null || path.length == 0) {
            return Float.POSITIVE_INFINITY;
        }
        float[] end = path[path.length - 1];
        return (float) MathUtil.getDistance(end[0], end[1], end[2], x, y, z);
    }

    /**
     * 目标移动后是否丢弃缓存路径：无中间点立即重寻；有中间点/在途请求仅当终点漂移过大时重寻。
     * Whether to drop a chase path after the target moved.
     */
    static boolean shouldInvalidatePath(float[][] path, boolean requestPending, float destinationDrift) {
        if (requestPending || hasIntermediateWaypoint(path)) {
            return destinationDrift > CHASE_REPATH_DISTANCE;
        }
        return true;
    }

    static long chaseRepathInterval(double targetDistance) {
        if (targetDistance <= 30) {
            return CHASE_REPATH_NEAR_INTERVAL_MS;
        }
        return targetDistance <= 60 ? CHASE_REPATH_MID_INTERVAL_MS : CHASE_REPATH_FAR_INTERVAL_MS;
    }

    static boolean shouldRepathChase(boolean distanceTiersEnabled, long lastReplanAt, long now, double targetDistance) {
        return !distanceTiersEnabled || lastReplanAt == 0
                || now - lastReplanAt >= chaseRepathInterval(targetDistance);
    }

    static boolean shouldRetargetPath(float[][] path, boolean targetReachable) {
        return targetReachable && path != null && path.length == 1;
    }

    static boolean targetsAnotherObject(boolean cachedPathValid, int cachedTargetId, boolean requestPending,
            int pendingTargetId, int targetId) {
        return cachedPathValid && cachedTargetId != targetId || requestPending && pendingTargetId != targetId;
    }

    /**
     * 是否应向客户端广播移动状态变化。
     * Whether a movement state change should be broadcast to clients.
     *
     * Current mask
     * New mask
     * @param destinationChanged 目标是否变化 / Whether destination changed
     * Whether to broadcast
     */
    static boolean shouldBroadcastMovement(byte currentMask, byte newMask, boolean destinationChanged) {
        return currentMask != newMask || destinationChanged;
    }

    static boolean shouldRestartMovement(boolean enhancedHomeReturn, boolean chasingTarget, byte currentMask) {
        return enhancedHomeReturn ? currentMask == MovementMask.IMMEDIATE
                : !chasingTarget || currentMask == MovementMask.IMMEDIATE;
    }

    static boolean shouldBroadcastDestination(boolean chasingTarget, boolean pathWaypointTransition,
            boolean destinationChanged, long now, long lastBroadcastAt) {
        return destinationChanged && (pathWaypointTransition || !chasingTarget
                || now - lastBroadcastAt >= CHASE_MOVE_BROADCAST_INTERVAL_MS);
    }

    static boolean hasMeaningfulProgress(float sampleX, float sampleY, float sampleZ, float currentX, float currentY,
            float currentZ, float waypointX, float waypointY, float waypointZ, float previousRemaining, float speed,
            long sampleMillis) {
        float directionX = waypointX - sampleX;
        float directionY = waypointY - sampleY;
        float directionLength = (float) Math.hypot(directionX, directionY);
        float projected = directionLength <= MOVE_OFFSET ? 0
                : ((currentX - sampleX) * directionX + (currentY - sampleY) * directionY) / directionLength;
        float remaining = (float) MathUtil.getDistance(currentX, currentY, currentZ, waypointX, waypointY, waypointZ);
        float required = Math.max(STUCK_MIN_PROGRESS, speed * sampleMillis / 1000f * STUCK_EXPECTED_PROGRESS_RATIO);
        return projected >= required || previousRemaining - remaining >= required;
    }

    public static RecoveryMetrics recoveryMetrics() {
        return new RecoveryMetrics(stuckSuspected.sum(), stuckConfirmed.sum(), stuckSelfRecovered.sum(),
                stuckReplanAttempts.sum(), stuckReplanFound.sum(), stuckReplanFailed.sum(), waypointSkipAttempts.sum(),
                waypointSkipSuccess.sum(), nearestNodeAttempts.sum(), nearestNodeSuccess.sum());
    }

    public record RecoveryMetrics(long stuckSuspected, long stuckConfirmed, long stuckSelfRecovered,
            long replanAttempts, long replanFound, long replanFailed, long waypointSkipAttempts,
            long waypointSkipSuccess, long nearestNodeAttempts, long nearestNodeSuccess) {}

    static boolean shouldRequestStuckRecovery(boolean confirmed, boolean requestPending, int attempts,
            long lastAttemptAt, long now) {
        return confirmed && !requestPending && attempts < STUCK_REPLAN_MAX_ATTEMPTS
                && (lastAttemptAt == 0 || now - lastAttemptAt >= STUCK_REPLAN_COOLDOWN_MS);
    }

    static float blockedSegmentRadius(int attempt) {
        return attempt <= 0 ? 0.35f : 0.75f;
    }

    static float[][] installPathResult(float[][] current, float[][] result, boolean recovery) {
        return recovery && (result == null || result.length == 0) ? current : result;
    }

    static boolean shouldTryWaypointSkip(float[][] path, int lookahead, long lastAttemptAt, long now) {
        return path != null && path.length > 1 && lookahead > 0
                && (lastAttemptAt == 0 || now - lastAttemptAt >= WAYPOINT_SKIP_INTERVAL_MS);
    }

    static boolean shouldUseAttackSlot(boolean spatialPath, float attackDistance) {
        return !spatialPath && attackDistance > 0.75f && attackDistance <= TARGET_SLOT_MAX_ATTACK_RANGE;
    }

    static int attackSlotOffsetDegrees(int ownerId, int targetId) {
        return (Math.floorMod(ownerId * 31 ^ targetId, 7) - 3) * 20;
    }

    static float[] attackSlotCandidate(float ownerX, float ownerY, float targetX, float targetY, float targetZ,
            float radius, int offsetDegrees) {
        double angle = Math.atan2(ownerY - targetY, ownerX - targetX);
        if (ownerX == targetX && ownerY == targetY) {
            angle = Math.toRadians(offsetDegrees * 3);
        } else {
            angle += Math.toRadians(offsetDegrees);
        }
        return new float[] {targetX + (float) Math.cos(angle) * radius,
                targetY + (float) Math.sin(angle) * radius, targetZ};
    }

    static boolean shouldAdjustGeoHeight(boolean enhancedHomeReturn, boolean returning, boolean spawnDestination,
            float[][] path) {
        return enhancedHomeReturn && returning || !returning && !spawnDestination;
    }

    /**
     * 开始向当前目标对象移动。
     * Start moving toward the current target object.
     */
    public synchronized void moveToTargetObject() {
        destination = Destination.TARGET_OBJECT;
        if (!started.getAndSet(true)) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "MC: moveToTarget started");
            }
        }
        updateLastMove();
        GameMovementLoopServices.moveTaskManager().addCreature(owner);
    }

    /**
     * 开始向指定坐标点移动。
     * Start moving toward a specific point.
     *
     * @param x 目标 X / Target X
     * @param y 目标 Y / Target Y
     * @param z 目标 Z / Target Z
     */
    public synchronized void moveToPoint(float x, float y, float z) {
        if (started.compareAndSet(false, true)) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "MC: moveToPoint started");
            }
            destination = Destination.POINT;
            pointX = x;
            pointY = y;
            pointZ = z;
            resetTargetTracking();
            resetPath();
        }
        updateLastMove();
        GameMovementLoopServices.moveTaskManager().addCreature(owner);
    }

    /**
     * 开始返回出生点。
     * Start returning to the spawn/home point.
     */
    public synchronized void moveToHome() {
        clearPathFailureContext();
        clearPathPullAttempts();
        resetTargetTracking();
        if (!started.getAndSet(true)) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "MC: moveToHome started");
            }
        }
        resetPath();
        Point3D target = getHomeReturnDestination();
        destination = Destination.HOME;
        pointX = target.getX();
        pointY = target.getY();
        pointZ = target.getZ();
        updateLastMove();
        GameMovementLoopServices.moveTaskManager().addCreature(owner);
    }

    /**
     * 开始向下一个巡逻点移动。
     * Start moving toward the next walk-route point.
     */
    public synchronized void moveToNextPoint() {
        if (started.compareAndSet(false, true)) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "MC: moveToNextPoint started");
            }
            destination = Destination.POINT;
            resetTargetTracking();
            resetPath();
        }
        updateLastMove();
        GameMovementLoopServices.moveTaskManager().addCreature(owner);
    }

    /**
     * 按当前目的地类型推进一帧移动（目标/坐标点/回家）。
     * Advance one movement frame by current destination type (target/point/home).
     */
    @Override
    public void moveToDestination() {
        if (owner.getAi2().isLogging()) {
            AI2Logger.moveinfo(owner, "moveToDestination destination: " + destination);
        }
        if (NpcActions.isAlreadyDead(owner)) {
            abortMove();
            return;
        }
        if (!owner.canPerformMove() || (owner.getAi2().getSubState() == AISubState.CAST)) {
            resetStuckShadow();
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "moveToDestination can't perform move");
            }
            if (started.compareAndSet(true, false)) {
                cancelFollow();
                setAndSendStopMove(owner);
            }
            updateLastMove();
            return;
        } else if (started.compareAndSet(false, true)) {
            pathStopSent = false;
            movementMask = MovementMask.NPC_STARTMOVE;
            PacketSendUtility.broadcastPacket(owner, new SM_MOVE(owner));
        }

        if (!started.get()) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "moveToDestination not started");
            }
        }
        if (!prepareGroundPath()) {
            stopForPath();
            updateLastMove();
            return;
        }
        switch (destination) {
            case TARGET_OBJECT:
                VisibleObject target = owner.getTarget();
                if (!(target instanceof Creature creature)) {
                    resetTargetTracking();
                    cancelFollow();
                    return;
                }
                if (usesPath()) {
                    boolean targetChanged = trackedTargetId != creature.getObjectId();
                    if (targetChanged) {
                        resetTargetTracking();
                        trackedTargetId = creature.getObjectId();
                        resetPath();
                    }
                    if (!targetChanged && targetsAnotherObject(cachedPathValid, cachedPathTargetId, pendingPath != null, pendingPathTargetId,
                            creature.getObjectId())) {
                        resetPath();
                    }
                    long now = System.currentTimeMillis();
                    if (targetMoved(target.getX(), target.getY(), target.getZ())) {
                        float attackDistance = owner.getController().getAttackDistanceToTarget();
                        boolean spatialPath = GameWorldServices.pathService().usesSpatialPath(owner);
                        float targetZ = getTargetZ(spatialPath, creature);
                        updateTargetDestination(creature, spatialPath, targetZ, attackDistance);
                        trackedTargetX = target.getX();
                        trackedTargetY = target.getY();
                        trackedTargetZ = target.getZ();
                        float[][] path = pathSnapshot();
                        boolean targetReachable = path != null && path.length == 1
                                && canReachWaypointCached(pointX, pointY, pointZ);
                        float destinationDrift = pendingPath != null
                                ? (float) MathUtil.getDistance(pendingPathX, pendingPathY, pendingPathZ, pointX, pointY, pointZ)
                                : pathDestinationDrift(path, pointX, pointY, pointZ);
                        if (shouldRetargetPath(path, targetReachable)) {
                            retargetPath(path, pointX, pointY, pointZ);
                        } else if (shouldInvalidatePath(path, pendingPath != null, destinationDrift)
                                && shouldRepathChase(GeoDataConfig.GEO_PATH_DISTANCE_TIERS_ENABLE, lastPathReplan, now,
                                        MathUtil.getDistance(owner, creature))) {
                            // 保留旧路径继续走，避免重寻期间原地停住。
                            invalidateChasePath();
                        }
                    }
                    boolean retryFailedPath = shouldRetryFailedPath(failedPathX, failedPathY, failedPathZ, pointX, pointY,
                            pointZ, failedPathObstacleVersion, currentObstacleVersion());
                    if (!retryFailedPath && tryPathAvoidance(creature, now)) {
                        updateLastMove();
                        return;
                    }
                    if (!retryFailedPath && shouldReactToPathFailure(firstPathFailureAt, pathFailureHandled, now)) {
                        pathFailureHandled = true;
                        TargetEventHandler.onPathFindFailed((NpcAI2) owner.getAi2());
                        return;
                    }
                    if (!cachedPathValid && GameWorldServices.pathService().hasPathingData(owner) && retryFailedPath) {
                        if (System.currentTimeMillis() >= pathRetryAt) {
                            requestTargetPath(creature);
                        }
                    }
                    float[][] path = skipWaypoints(pathSnapshot(), now);
                    if (path != null && path.length > 0) {
                        float[] p1 = path[0];
                        assert p1.length == 3;
                        moveToLocation(p1[0], p1[1], p1[2], pathMoveOffset(path), path);
                    } else if (!GameWorldServices.pathService().hasPathingData(owner)) {
                        moveToLocation(pointX, pointY, pointZ, offset);
                    } else {
                        stopForPath();
                    }
                } else {
                    if (owner.getAi2().getState() == AIState.FOLLOWING) {
                        cancelFollow();
                        offset = owner.getController().getAttackDistanceToTarget();
                        moveToLocation(target.getX(), target.getY(), target.getZ(), offset);
                        break;
                    }
                    applyFollow(target);
                }
                break;
            case POINT: {
                cancelFollow();
                moveAlongPath();
                break;
            }
            case HOME: {
                long now = System.currentTimeMillis();
                boolean retryFailedPath = shouldRetryFailedPath(failedPathX, failedPathY, failedPathZ, pointX, pointY,
                        pointZ, failedPathObstacleVersion, currentObstacleVersion());
                if (shouldTeleportFailedHomeReturn(isSpReturn(owner), homeReturnStartedAt, now)) {
                    finishFailedHomeReturn();
                    return;
                }
                if (hasHomeReturnTimedOut(homeReturnStartedAt, HOME_RETURN_TIMEOUT_MS, now)) {
                    finishFailedHomeReturn();
                    return;
                }
                if (!usesPath()) {
                    moveToLocation(pointX, pointY, pointZ, offset);
                    break;
                }
                if (GameWorldServices.pathService().hasPathingData(owner)
                        && shouldRequestHomePath(cachedPathValid, retryFailedPath, pendingPath != null, now, pathRetryAt)) {
                    requestLocationPath();
                }
                if (shouldFinishFailedHomeReturn(AIConfig.ENHANCED_HOME_RETURN, firstPathFailureAt, retryFailedPath,
                        pendingPath != null)) {
                    finishFailedHomeReturn();
                    return;
                }
                float[][] path = skipWaypoints(pathSnapshot(), now);
                if (path != null && path.length > 0) {
                    float[] p1 = path[0];
                    moveToLocation(p1[0], p1[1], p1[2], pathMoveOffset(path), path);
                } else if (!GameWorldServices.pathService().hasPathingData(owner)) {
                    moveToLocation(pointX, pointY, pointZ, offset);
                } else {
                    stopForPath();
                }
            }
        }
        this.updateLastMove();
    }

    /**
     * 解析追击目标高度：地面怪物取目标脚下地表，空间寻路保留目标高度。
     * Resolve chase Z: ground movers use the surface below the target; spatial movers keep target Z.
     *
     * @param spatialPath whether movement follows a spatial path
     * @param creature target creature
     * @return effective Z
     */
    private float getTargetZ(boolean spatialPath, Creature creature) {
        float targetZ = creature.getZ();
        float groundZ = spatialPath ? targetZ : GameWorldServices.geoService().getZ(creature);
        return resolvedTargetZ(spatialPath, targetZ, groundZ);
    }

    private boolean targetMoved(float x, float y, float z) {
        return !Float.isFinite(trackedTargetX)
                || MathUtil.getDistance(trackedTargetX, trackedTargetY, trackedTargetZ, x, y, z) > MOVE_CHECK_OFFSET;
    }

    private void updateTargetDestination(Creature target, boolean spatialPath, float targetZ, float attackDistance) {
        if (shouldUseAttackSlot(spatialPath, attackDistance)) {
            boolean refresh = chaseSlotTargetId != target.getObjectId()
                    || MathUtil.getDistance(chaseSlotAnchorX, chaseSlotAnchorY, target.getX(), target.getY())
                            >= TARGET_SLOT_RECALC_DISTANCE;
            if (refresh) {
                float[] slot = findAttackSlot(target, targetZ, attackDistance);
                chaseSlotTargetId = target.getObjectId();
                chaseSlotAnchorX = target.getX();
                chaseSlotAnchorY = target.getY();
                chaseSlotValid = slot != null;
                if (slot != null) {
                    chaseSlotX = slot[0];
                    chaseSlotY = slot[1];
                    chaseSlotZ = slot[2];
                }
            }
            if (chaseSlotTargetId == target.getObjectId() && chaseSlotValid) {
                pointX = chaseSlotX;
                pointY = chaseSlotY;
                pointZ = chaseSlotZ;
                offset = MOVE_OFFSET;
                return;
            }
        } else {
            chaseSlotTargetId = 0;
            chaseSlotValid = false;
        }
        offset = attackDistance;
        pointX = target.getX();
        pointY = target.getY();
        pointZ = targetZ;
    }

    private float[] findAttackSlot(Creature target, float targetZ, float attackDistance) {
        float radius = Math.max(0.5f, attackDistance - 0.25f);
        int baseOffset = attackSlotOffsetDegrees(owner.getObjectId(), target.getObjectId());
        for (int attempt = 0; attempt < TARGET_SLOT_ADJUSTMENTS.length; attempt++) {
            int index = (chaseSlotAdjustment + attempt) % TARGET_SLOT_ADJUSTMENTS.length;
            int adjustment = TARGET_SLOT_ADJUSTMENTS[index];
            float[] candidate = attackSlotCandidate(owner.getX(), owner.getY(), target.getX(), target.getY(), targetZ,
                    radius, baseOffset + adjustment);
            float[] projected = GameWorldServices.pathService().projectGroundPoint(owner, candidate[0], candidate[1],
                    candidate[2]);
            if (projected != null) {
                chaseSlotAdjustment = index;
                return projected;
            }
        }
        return null;
    }

    private void resetTargetTracking() {
        trackedTargetId = 0;
        trackedTargetX = Float.NaN;
        trackedTargetY = 0;
        trackedTargetZ = 0;
        chaseSlotTargetId = 0;
        chaseSlotValid = false;
        chaseSlotAdjustment = 0;
    }

    /**
     * 按速度插值向坐标推进，处理掩码广播与路径缓存消费。
     * Interpolate toward coordinates by speed; handle mask broadcast and path-cache consumption.
     *
     * Target X
     * Target Y
     * Target Z
     * Stop offset
     */
    protected void moveToLocation(float targetX, float targetY, float targetZ, float offset) {
        moveToLocation(targetX, targetY, targetZ, offset, null);
    }

    private void moveToLocation(float targetX, float targetY, float targetZ, float offset, float[][] path) {
        float ownerX = ((Npc)this.owner).getX();
        float ownerY = ((Npc)this.owner).getY();
        float ownerZ = ((Npc)this.owner).getZ();
        boolean intermediateWaypoint = hasIntermediateWaypoint(path);
        boolean pathWaypointTransition = intermediateWaypoint || previousIntermediateWaypoint;
        previousIntermediateWaypoint = intermediateWaypoint;
        boolean destinationChanged = targetX != this.targetDestX || targetY != this.targetDestY || targetZ != this.targetDestZ;
        boolean directionChanged = destinationChanged && shouldRestartMovement(
                AIConfig.ENHANCED_HOME_RETURN && destination == Destination.HOME,
                destination == Destination.TARGET_OBJECT, movementMask);
        if (destinationChanged) {
            this.heading = (byte)(Math.toDegrees(Math.atan2(targetY - ownerY, targetX - ownerX)) / 3.0);
        }
        if (((Npc)this.owner).getAi2().isLogging()) {
            AI2Logger.moveinfo((Creature)this.owner, "OLD targetDestX: " + this.targetDestX + " targetDestY: " + this.targetDestY + " targetDestZ " + this.targetDestZ);
        }
        if (targetX == 0.0f && targetY == 0.0f) {
            targetX = ((Npc)this.owner).getSpawn().getX();
            targetY = ((Npc)this.owner).getSpawn().getY();
            targetZ = ((Npc)this.owner).getSpawn().getZ();
        }
        this.targetDestX = targetX;
        this.targetDestY = targetY;
        this.targetDestZ = targetZ;
        if (((Npc)this.owner).getAi2().isLogging()) {
            AI2Logger.moveinfo((Creature)this.owner, "ownerX=" + ownerX + " ownerY=" + ownerY + " ownerZ=" + ownerZ);
            AI2Logger.moveinfo((Creature)this.owner, "targetDestX: " + this.targetDestX + " targetDestY: " + this.targetDestY + " targetDestZ " + this.targetDestZ);
        }
        float currentSpeed = movementSpeed((Npc) owner);
        long now = System.currentTimeMillis();
        long elapsedMillis = Math.max(1, now - this.lastMoveUpdate);
        float futureDistPassed = currentSpeed * elapsedMillis / 1000.0f;
        float dist = (float)MathUtil.getDistance(ownerX, ownerY, ownerZ, targetX, targetY, targetZ);
        if (((Npc)this.owner).getAi2().isLogging()) {
            AI2Logger.moveinfo((Creature)this.owner, "futureDist: " + futureDistPassed + " dist: " + dist);
        }
        if (dist == 0.0f) {
            resetStuckShadow();
            pathStopSent = false;
            boolean pathCompleted = consumeWaypoint(path);
            if (((Npc)this.owner).getAi2().getState() == AIState.RETURNING
                    && shouldCompleteHomeReturn(path == null || pathCompleted, isHomeReturnDestinationReached())) {
                if (((Npc)this.owner).getAi2().isLogging()) {
                    AI2Logger.moveinfo((Creature)this.owner, "\u72b6\u6001\u8fd4\u56de\uff1a\u4e2d\u6b62\u79fb\u52a8");
                }
                TargetEventHandler.onTargetReached((NpcAI2)((Npc)this.owner).getAi2());
            }
            return;
        }
        if (futureDistPassed > dist) {
            futureDistPassed = dist;
        }
        float distFraction = futureDistPassed / dist;
        float newX = (this.targetDestX - ownerX) * distFraction + ownerX;
        float newY = (this.targetDestY - ownerY) * distFraction + ownerY;
        float newZ = (this.targetDestZ - ownerZ) * distFraction + ownerZ;
        if (pathStopSent) {
            pathStopSent = false;
            directionChanged = true;
        }
        if (ownerX == newX && ownerY == newY && ((Npc)this.owner).getSpawn().getRandomWalk() > 0) {
            return;
        }
        boolean returning = owner.getAi2().getState() == AIState.RETURNING;
        SpawnTemplate spawn = owner.getSpawn();
        boolean spawnDestination = spawn.getX() == targetDestX && spawn.getY() == targetDestY && spawn.getZ() == targetDestZ;
        if (shouldAdjustGeoHeight(AIConfig.ENHANCED_HOME_RETURN, returning, spawnDestination, path)
                && GeoDataConfig.GEO_NPC_MOVE && GeoDataConfig.GEO_ENABLE
                && !GameWorldServices.pathService().usesSpatialPath(owner)
                && owner.getAi2().getSubState() != AISubState.WALK_PATH
                && owner.getGameStats().checkGeoNeedUpdate()) {
            float geoZ = GameWorldServices.geoService().getZ(owner.getWorldId(), newX, newY, newZ, 100, owner.getInstanceId());
            if (Math.abs(newZ - geoZ) > 1) {
                directionChanged = true;
            }
            newZ = geoZ;
        }
        if (((Npc)this.owner).getAi2().isLogging()) {
            AI2Logger.moveinfo((Creature)this.owner, "newX=" + newX + " newY=" + newY + " newZ=" + newZ + " mask=" + this.movementMask);
        }
        com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(this.owner, newX, newY, newZ, this.heading, false);
        sampleStuckShadow(owner.getX(), owner.getY(), owner.getZ(), targetX, targetY, targetZ, currentSpeed, now, path,
                (float) MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(), targetX, targetY, targetZ), offset);
        boolean reachedWaypoint = reachedWaypoint(targetX, targetY, targetZ, owner.getX(), owner.getY(), owner.getZ());
        if (reachedWaypoint && consumeWaypoint(path) && owner.getAi2().getState() == AIState.RETURNING
                && shouldCompleteHomeReturn(true, isHomeReturnDestinationReached())) {
            TargetEventHandler.onTargetReached((NpcAI2) owner.getAi2());
            return;
        }
        byte newMask = this.getMoveMask(directionChanged);
        boolean broadcastDestination = shouldBroadcastDestination(destination == Destination.TARGET_OBJECT,
                pathWaypointTransition, destinationChanged, now, lastMoveBroadcastAt);
        if (shouldBroadcastMovement(this.movementMask, newMask, broadcastDestination || directionChanged)) {
            if (this.movementMask != newMask) {
                if (((Npc)this.owner).getAi2().isLogging()) {
                    AI2Logger.moveinfo((Creature)this.owner, "oldMask=" + this.movementMask + " newMask=" + newMask);
                }
                this.movementMask = newMask;
            }
            lastMoveBroadcastAt = now;
            PacketSendUtility.broadcastPacket(owner, new SM_MOVE(owner.getObjectId(), ownerX, ownerY, ownerZ,
                    targetDestX, targetDestY, targetDestZ, heading, movementMask));
        }
    }

    void sampleStuckShadow(float x, float y, float z, float waypointX, float waypointY, float waypointZ,
            float speed, long now, float[][] path, float remaining, float stopOffset) {
        if (path == null || path.length == 0 || speed <= MOVE_OFFSET
                || destination == Destination.TARGET_OBJECT && path.length == 1 && remaining <= stopOffset) {
            resetStuckShadow();
            return;
        }
        if (progressSampleAt == 0 || progressRequestId != pathRequestId || progressPathLength != path.length) {
            resetStuckShadow();
            beginStuckSample(x, y, z, waypointX, waypointY, waypointZ, remaining, now, path.length);
            return;
        }
        long sampleMillis = now - progressSampleAt;
        if (sampleMillis < STUCK_SAMPLE_INTERVAL_MS) {
            return;
        }
        if (sampleMillis <= 0 || sampleMillis > STUCK_SAMPLE_MAX_DELAY_MS) {
            resetStuckShadow();
            beginStuckSample(x, y, z, waypointX, waypointY, waypointZ, remaining, now, path.length);
            return;
        }
        boolean progressed = hasMeaningfulProgress(progressX, progressY, progressZ, x, y, z, waypointX, waypointY,
                waypointZ, progressRemainingDistance, speed, sampleMillis);
        int previousWindows = noProgressWindows;
        if (progressed) {
            if (previousWindows >= STUCK_SUSPECTED_WINDOWS) {
                stuckSelfRecovered.increment();
                logStuckShadow("SELF_RECOVERED", waypointX, waypointY, waypointZ, speed, sampleMillis);
            }
            if (stuckReplanAttemptCount > 0) {
                cancelPendingStuckRecovery();
                clearPathFailureContext();
            }
            noProgressWindows = 0;
            stuckShadowConfirmed = false;
        } else {
            noProgressWindows++;
            if (previousWindows < STUCK_SUSPECTED_WINDOWS && noProgressWindows >= STUCK_SUSPECTED_WINDOWS) {
                stuckSuspected.increment();
                lastWaypointSkipAt = 0;
                logStuckShadow("SUSPECTED", waypointX, waypointY, waypointZ, speed, sampleMillis);
            }
            if (!stuckShadowConfirmed && noProgressWindows >= STUCK_CONFIRMED_WINDOWS) {
                stuckShadowConfirmed = true;
                stuckConfirmed.increment();
                logStuckShadow("CONFIRMED", waypointX, waypointY, waypointZ, speed, sampleMillis);
            }
            tryStuckRecovery(progressX, progressY, progressZ, waypointX, waypointY, waypointZ, now);
        }
        beginStuckSample(x, y, z, waypointX, waypointY, waypointZ, remaining, now, path.length);
    }

    private void beginStuckSample(float x, float y, float z, float waypointX, float waypointY, float waypointZ,
            float remaining, long now, int pathLength) {
        progressSampleAt = now;
        progressRequestId = pathRequestId;
        progressX = x;
        progressY = y;
        progressZ = z;
        progressWaypointX = waypointX;
        progressWaypointY = waypointY;
        progressWaypointZ = waypointZ;
        progressRemainingDistance = remaining;
        progressPathLength = pathLength;
    }

    private void resetStuckShadow() {
        progressSampleAt = 0;
        progressRequestId = 0;
        progressPathLength = 0;
        noProgressWindows = 0;
        stuckShadowConfirmed = false;
    }

    private void logStuckShadow(String status, float waypointX, float waypointY, float waypointZ, float speed,
            long sampleMillis) {
        if (owner != null && owner.getAi2().isLogging()) {
            AI2Logger.info(owner.getAi2(), "PATH stuck shadow status=" + status + " requestId=" + pathRequestId
                    + " windows=" + noProgressWindows + " sampleMs=" + sampleMillis + " speed=" + speed
                    + " from=(" + progressX + ',' + progressY + ',' + progressZ + ") waypointBefore=("
                    + progressWaypointX + ',' + progressWaypointY + ',' + progressWaypointZ + ") waypointNow=("
                    + waypointX + ',' + waypointY + ',' + waypointZ + ")");
        }
    }

    /**
     * 根据方向变化、AI 状态与速度加成计算移动掩码。
     * Compute the movement mask from direction change, AI state, and speed bonus.
     *
     * @param directionChanged 方向是否变化 / Whether direction changed
     * Movement mask
     */
    private byte getMoveMask(boolean directionChanged) {
        if (directionChanged) {
            return MovementMask.NPC_STARTMOVE;
        }
        if (((Npc)this.owner).getAi2().getState() == AIState.RETURNING) {
            return owner.isInState(CreatureState.WALKING) ? MovementMask.NPC_WALK_FAST : MovementMask.NPC_RUN_FAST;
        }
        if (((Npc)this.owner).getAi2().getState() == AIState.FOLLOWING) {
            return MovementMask.NPC_WALK_SLOW;
        }
        byte mask = MovementMask.IMMEDIATE;
        Stat2 stat = ((Npc)this.owner).getGameStats().getMovementSpeed();
        if (((Npc)this.owner).isInState(CreatureState.WEAPON_EQUIPPED)) {
            mask = stat.getBonus() < 0 ? MovementMask.NPC_RUN_FAST : MovementMask.NPC_RUN_SLOW;
        } else if (((Npc)this.owner).isInState(CreatureState.WALKING) || ((Npc)this.owner).isInState(CreatureState.ACTIVE)) {
            byte by = mask = stat.getBonus() < 0 ? MovementMask.NPC_WALK_FAST : MovementMask.NPC_WALK_SLOW;
        }
        if (((Npc)this.owner).isFlying()) {
            mask |= MovementMask.GLIDE;
        }
        return mask;
    }

    /**
     * 中止移动并广播停止。
     * Abort movement and broadcast stop.
     */
    @Override
    public void abortMove() {
        if (!this.started.get()) {
            GameMovementLoopServices.moveTaskManager().removeCreature(owner);
            resetPath();
            return;
        }
        this.resetMove();
        this.setAndSendStopMove((Creature)this.owner);
    }

    /**
     * 重置移动状态、目标点与跟随马达。
     * Reset move state, destination points, and follow motor.
     */
    public void resetMove() {
        if (owner.getAi2().isLogging()) {
            AI2Logger.moveinfo(owner, "MC perform stop");
        }
        GameMovementLoopServices.moveTaskManager().removeCreature(owner);
        cancelFollow();
        started.set(false);
        targetDestX = 0;
        targetDestY = 0;
        targetDestZ = 0;
        pointX = 0;
        pointY = 0;
        pointZ = 0;
        pathStopSent = false;
        previousIntermediateWaypoint = false;
        resetTargetTracking();
        resetStuckShadow();
        resetPath();
    }

    private void moveAlongPath() {
        if (!usesPath()) {
            moveToLocation(pointX, pointY, pointZ, offset);
            return;
        }
        long now = System.currentTimeMillis();
        if (!cachedPathValid && GameWorldServices.pathService().hasPathingData(owner)
                && now >= pathRetryAt) {
            requestLocationPath();
        }
        float[][] path = skipWaypoints(pathSnapshot(), now);
        if (path != null && path.length > 0) {
            float[] waypoint = path[0];
            moveToLocation(waypoint[0], waypoint[1], waypoint[2], pathMoveOffset(path), path);
        } else if (!GameWorldServices.pathService().hasPathingData(owner)) {
            moveToLocation(pointX, pointY, pointZ, offset);
        } else if (shouldFinishFailedPointMove(firstPathFailureAt, pathFailureHandled, pendingPath != null, now)) {
            pathFailureHandled = true;
            finishFailedPointMove();
        } else {
            stopForPath();
        }
    }

    private void finishFailedPointMove() {
        NpcAI2 npcAI = (NpcAI2) owner.getAi2();
        boolean walking = npcAI.isInState(AIState.WALKING);
        boolean retryRandomWalk = walking && npcAI.isInSubState(AISubState.WALK_RANDOM);
        abortMove();
        clearPathFailureContext();
        if (retryRandomWalk) {
            WalkManager.startRandomWalking(npcAI);
        } else if (walking) {
            WalkManager.stopWalking(npcAI);
        }
    }

    private void stopForPath() {
        resetStuckShadow();
        if (shouldSendPathStop(pathStopSent)) {
            pathStopSent = true;
            setAndSendStopMove(owner);
        }
    }

    static boolean shouldSendPathStop(boolean pathStopSent) {
        return !pathStopSent;
    }

    private boolean usesPath() {
        return GeoDataConfig.GEO_PATH_ENABLE;
    }

    private boolean canReachWaypointCached(float x, float y, float z) {
        float fromX = owner.getX();
        float fromY = owner.getY();
        float fromZ = owner.getZ();
        long now = System.currentTimeMillis();
        if (canReuseReachCheck(now, reachCheckedAt, fromX, fromY, fromZ, reachFromX, reachFromY, reachFromZ,
                x, y, z, reachToX, reachToY, reachToZ)) {
            return reachResult;
        }
        reachResult = GameWorldServices.pathService().canReachWaypoint(owner, x, y, z);
        reachFromX = fromX;
        reachFromY = fromY;
        reachFromZ = fromZ;
        reachToX = x;
        reachToY = y;
        reachToZ = z;
        reachCheckedAt = now;
        return reachResult;
    }

    private float[][] skipWaypoints(float[][] path, long now) {
        int lookahead = Math.max(0, GeoDataConfig.GEO_PATH_WAYPOINT_LOOKAHEAD);
        if (!shouldTryWaypointSkip(path, lookahead, lastWaypointSkipAt, now)) {
            return path;
        }
        lastWaypointSkipAt = now;
        waypointSkipAttempts.increment();
        int firstIndex = GameWorldServices.pathService().waypointSkipIndex(owner, path, lookahead);
        if (firstIndex == 0) {
            return path;
        }
        float[][] skipped;
        synchronized (this) {
            if (cachedPath != path) {
                return cachedPath;
            }
            skipped = remainingPath(path, firstIndex);
            cachedPath = skipped;
        }
        waypointSkipSuccess.increment();
        resetStuckShadow();
        return skipped;
    }

    private float pathMoveOffset(float[][] path) {
        return path != null && path == recoveryBridgePath ? 0 : offset;
    }

    static boolean canReuseReachCheck(long now, long checkedAt, float fromX, float fromY, float fromZ,
            float cachedFromX, float cachedFromY, float cachedFromZ, float toX, float toY, float toZ,
            float cachedToX, float cachedToY, float cachedToZ) {
        return now - checkedAt <= REACH_CHECK_CACHE_MS
                && samePoint(fromX, fromY, fromZ, cachedFromX, cachedFromY, cachedFromZ)
                && samePoint(toX, toY, toZ, cachedToX, cachedToY, cachedToZ);
    }

    private static boolean samePoint(float x, float y, float z, float otherX, float otherY, float otherZ) {
        return Math.abs(x - otherX) <= MOVE_CHECK_OFFSET
                && Math.abs(y - otherY) <= MOVE_CHECK_OFFSET
                && Math.abs(z - otherZ) <= MOVE_CHECK_OFFSET;
    }

    private float[] projectLocalAvoidanceStep(float x, float y, float z) {
        return stableAvoidanceProjection(z, GameWorldServices.pathService().projectLocalStep(owner, x, y, z));
    }

    static float[] stableAvoidanceProjection(float intendedZ, float[] projected) {
        if (projected == null || Math.abs(projected[2] - intendedZ) > LOCAL_AVOIDANCE_HEIGHT_TOLERANCE) {
            return null;
        }
        projected[2] = intendedZ;
        return projected;
    }

    private boolean tryPathAvoidance(Creature target, long now) {
        if (!shouldTryPathAvoidance(firstPathFailureAt, lastPathAvoidanceAt, pathAvoidanceAttempts, now)) {
            return false;
        }
        lastPathAvoidanceAt = now;
        pathAvoidanceAttempts++;
        float[] desired = localAvoidanceTarget(owner.getX(), owner.getY(), owner.getZ(), pointX, pointY, pointZ,
                PATH_AVOIDANCE_STEP);
        if (desired == null) {
            return false;
        }
        float oldX = owner.getX();
        float oldY = owner.getY();
        float oldZ = owner.getZ();
        float[] step = projectLocalAvoidanceStep(desired[0], desired[1], desired[2]);
        if (step == null || !GameWorldServices.pathService().canMoveStraight(owner, step[0], step[1], step[2])) {
            return false;
        }
        moveToLocation(step[0], step[1], step[2], 0);
        if (MathUtil.getDistance(oldX, oldY, oldZ, owner.getX(), owner.getY(), owner.getZ()) <= MOVE_OFFSET) {
            return false;
        }
        resetPath();
        requestTargetPath(target);
        return true;
    }

    void blockedPathStep() {
        resetPath();
        if (destination == Destination.TARGET_OBJECT) {
            recordPathFailure(pointX, pointY, pointZ, false);
        }
    }

    private synchronized void tryStuckRecovery(float fromX, float fromY, float fromZ, float waypointX,
            float waypointY, float waypointZ, long now) {
        if (owner == null || !GeoDataConfig.GEO_PATH_RECOVERY_ENABLE
                || GameWorldServices.pathService().usesSpatialPath(owner)) {
            return;
        }
        Creature target = owner.getTarget() instanceof Creature creature ? creature : null;
        if (destination == Destination.TARGET_OBJECT && target == null) {
            return;
        }
        if (!shouldRequestStuckRecovery(stuckShadowConfirmed, pendingPath != null, stuckReplanAttemptCount,
                lastStuckReplanAt, now)) {
            if (stuckShadowConfirmed && pendingPath == null && stuckReplanAttemptCount >= STUCK_REPLAN_MAX_ATTEMPTS) {
                tryNearestPathRecovery(target);
            }
            return;
        }
        if (target != null && chaseSlotTargetId == target.getObjectId() && chaseSlotValid) {
            refreshAttackSlotForRecovery(target);
        }
        float radius = blockedSegmentRadius(stuckReplanAttemptCount);
        PathService.BlockedSegment blocked = new PathService.BlockedSegment(fromX, fromY, fromZ, waypointX,
                waypointY, waypointZ, radius, now + STUCK_BLOCKED_SEGMENT_TTL_MS);
        stuckReplanAttemptCount++;
        lastStuckReplanAt = now;
        stuckReplanAttempts.increment();
        pendingPathTargetId = target == null ? 0 : target.getObjectId();
        pendingPathX = pointX;
        pendingPathY = pointY;
        pendingPathZ = pointZ;
        recordPathFailure(pendingPathX, pendingPathY, pendingPathZ, false);
        pathRequested(true);
        pendingPath = destination == Destination.TARGET_OBJECT
                ? GameWorldServices.pathService().navigateToTargetAsync(owner, pendingPathX, pendingPathY, pendingPathZ, blocked)
                : GameWorldServices.pathService().navigateToLocationAsync(owner, pendingPathX, pendingPathY, pendingPathZ, blocked);
        logPathRequest("stuck-recovery-" + stuckReplanAttemptCount);
    }

    private void refreshAttackSlotForRecovery(Creature target) {
        int previousAdjustment = chaseSlotAdjustment;
        chaseSlotAdjustment = (chaseSlotAdjustment + 1) % TARGET_SLOT_ADJUSTMENTS.length;
        float attackDistance = owner.getController().getAttackDistanceToTarget();
        float[] slot = findAttackSlot(target, getTargetZ(false, target), attackDistance);
        if (slot == null) {
            chaseSlotAdjustment = previousAdjustment;
            return;
        }
        chaseSlotAnchorX = target.getX();
        chaseSlotAnchorY = target.getY();
        chaseSlotX = pointX = slot[0];
        chaseSlotY = pointY = slot[1];
        chaseSlotZ = pointZ = slot[2];
        offset = MOVE_OFFSET;
    }

    private void tryNearestPathRecovery(Creature target) {
        if (nearestRecoveryAttempted) {
            return;
        }
        nearestRecoveryAttempted = true;
        nearestNodeAttempts.increment();
        float[] point = GameWorldServices.pathService().nearestGroundPoint(owner, NEAREST_PATH_RECOVERY_RADIUS,
                NEAREST_PATH_RECOVERY_VERTICAL);
        if (point == null) {
            return;
        }
        clearPathFailureContext();
        nearestRecoveryAttempted = true;
        pathRequestId++;
        cachedObstacleVersion = currentObstacleVersion();
        cachedPathTargetId = target == null ? 0 : target.getObjectId();
        cachedPath = new float[][] {{point[0], point[1], point[2]}};
        recoveryBridgePath = cachedPath;
        cachedPathValid = true;
        lastWaypointSkipAt = 0;
        nearestNodeSuccess.increment();
        if (owner.getAi2().isLogging()) {
            AI2Logger.info(owner.getAi2(), "PATH nearest-node recovery to=(" + point[0] + ',' + point[1] + ','
                    + point[2] + ")");
        }
    }

    private synchronized void cancelPendingStuckRecovery() {
        if (pendingPathRecovery && pendingPath != null) {
            pathRequestId++;
            pendingPath.cancel(true);
            pendingPath = null;
            pendingPathTargetId = 0;
            pendingPathX = Float.NaN;
            pendingPathStartedAt = 0;
            pendingPathRequestId = 0;
            pendingPathObstacleVersion = 0;
        }
        pendingPathRecovery = false;
        clearStuckRecoveryState();
    }

    private void clearStuckRecoveryState() {
        stuckReplanAttemptCount = 0;
        lastStuckReplanAt = 0;
        nearestRecoveryAttempted = false;
    }

    private boolean prepareGroundPath() {
        collectPath();
        if (!usesPath()) {
            return true;
        }
        if (!GameWorldServices.pathService().hasPathingData(owner)) {
            clearPathFailureContext();
            resetPath();
            return true;
        }
        if (!cachedPathValid) {
            return true;
        }
        long now = System.currentTimeMillis();
        long obstacleVersion = GameWorldServices.pathService().obstacleVersion(owner.getWorldId(), owner.getInstanceId());
        if (obstacleVersion != cachedObstacleVersion) {
            resetPath();
            return true;
        }
        float[][] path = pathSnapshot();
        boolean blocked = path != null && path.length > 0
                && !canReachWaypointCached(path[0][0], path[0][1], path[0][2]);
        if (shouldKeepPathResult(path, blocked)) {
            return true;
        }
        if (now - lastPathReplan < 500) {
            return false;
        }
        resetPath();
        return true;
    }

    private synchronized void resetPath() {
        if (pendingPath != null) {
            pathRequestId++;
            pendingPath.cancel(true);
            pendingPath = null;
        }
        pendingPathTargetId = 0;
        pendingPathX = Float.NaN;
        pendingPathStartedAt = 0;
        pendingPathRequestId = 0;
        pendingPathObstacleVersion = 0;
        pendingPathRecovery = false;
        cachedPathTargetId = 0;
        cachedPath = null;
        recoveryBridgePath = null;
        cachedPathValid = false;
        pathRetryAt = 0;
        lastWaypointSkipAt = 0;
        clearStuckRecoveryState();
    }

    /** 作废缓存并取消在途请求，但保留旧路点供追击继续。 / Drop validity and pending request; keep old waypoints for chase. */
    private synchronized void invalidateChasePath() {
        if (pendingPath != null) {
            pathRequestId++;
            pendingPath.cancel(true);
            pendingPath = null;
        }
        pendingPathTargetId = 0;
        pendingPathX = Float.NaN;
        pendingPathStartedAt = 0;
        pendingPathRequestId = 0;
        pendingPathObstacleVersion = 0;
        pendingPathRecovery = false;
        cachedPathValid = false;
        pathRetryAt = 0;
        clearStuckRecoveryState();
    }

    private synchronized float[][] pathSnapshot() {
        return cachedPath;
    }

    private synchronized void retargetPath(float[][] path, float x, float y, float z) {
        if (cachedPath == path) {
            cachedPath = new float[][] {{x, y, z}};
            lastWaypointSkipAt = 0;
        }
    }

    private synchronized boolean consumeWaypoint(float[][] path) {
        if (path == null || cachedPath != path) {
            return false;
        }
        if (recoveryBridgePath == path) {
            recoveryBridgePath = null;
        }
        cachedPath = consumePath(cachedPath, path);
        lastWaypointSkipAt = 0;
        if (cachedPath == null) {
            cachedPathValid = false;
            return true;
        }
        return false;
    }

    private synchronized void requestTargetPath(Creature target) {
        if (pendingPath != null && pendingPathTargetId != target.getObjectId()) {
            resetPath();
        }
        if (pendingPath == null) {
            pendingPathTargetId = target.getObjectId();
            pendingPathX = pointX;
            pendingPathY = pointY;
            pendingPathZ = pointZ;
            pathRequested();
            pendingPath = GameWorldServices.pathService().navigateToTargetAsync(owner, pendingPathX, pendingPathY, pendingPathZ);
            logPathRequest("target");
        }
        collectPath();
    }

    private synchronized void requestLocationPath() {
        if (pendingPath == null) {
            pendingPathX = pointX;
            pendingPathY = pointY;
            pendingPathZ = pointZ;
            pathRequested();
            pendingPath = GameWorldServices.pathService().navigateToLocationAsync(owner, pointX, pointY, pointZ);
            logPathRequest("location");
        }
        collectPath();
    }

    private void pathRequested() {
        pathRequested(false);
    }

    private void pathRequested(boolean recovery) {
        pendingPathStartedAt = lastPathReplan = System.currentTimeMillis();
        pendingPathRequestId = ++pathRequestId;
        pendingPathObstacleVersion = currentObstacleVersion();
        cachedObstacleVersion = pendingPathObstacleVersion;
        pendingPathRecovery = recovery;
    }

    private void logPathRequest(String kind) {
        if (owner.getAi2().isLogging()) {
            AI2Logger.info(owner.getAi2(), "PATH request kind=" + kind
                    + " requestId=" + pendingPathRequestId + " obstacleVersion=" + pendingPathObstacleVersion
                    + " from=(" + owner.getX() + ',' + owner.getY() + ',' + owner.getZ() + ") to=("
                    + pendingPathX + ',' + pendingPathY + ',' + pendingPathZ + ") distance="
                    + MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(), pendingPathX, pendingPathY, pendingPathZ));
        }
    }

    private synchronized void collectPath() {
        CompletableFuture<float[][]> request = pendingPath;
        if (request == null || !request.isDone()) {
            return;
        }
        int targetId = pendingPathTargetId;
        float targetX = pendingPathX;
        float targetY = pendingPathY;
        float targetZ = pendingPathZ;
        long requestId = pendingPathRequestId;
        long obstacleVersion = pendingPathObstacleVersion;
        boolean recovery = pendingPathRecovery;
        long elapsed = Math.max(0, System.currentTimeMillis() - pendingPathStartedAt);
        try {
            if (!shouldAcceptPathResult(requestId, pathRequestId, obstacleVersion, currentObstacleVersion())) {
                if (!recovery) {
                    cachedPath = null;
                    cachedPathValid = false;
                }
                pathRetryAt = System.currentTimeMillis();
                logPathResult("STALE", elapsed, requestId, obstacleVersion);
                return;
            }
            float[][] result = request.getNow(null);
            cachedPath = installPathResult(cachedPath, result, recovery);
            if (result == null) {
                if (recovery) {
                    stuckReplanFailed.increment();
                    recordPathFailure(targetX, targetY, targetZ, true);
                } else {
                    cachedPathValid = true;
                    recordPathFailure(targetX, targetY, targetZ, false);
                }
                logPathResult("NO_PATH", elapsed, requestId, obstacleVersion);
            } else if (isEmptyPathResult(result)) {
                if (!recovery) {
                    cachedPath = null;
                    cachedPathValid = false;
                } else {
                    stuckReplanFailed.increment();
                }
                pathRetryAt = System.currentTimeMillis() + PATH_RETRY_DELAY_MS;
                logPathResult("EMPTY", elapsed, requestId, obstacleVersion);
            } else {
                cachedPathTargetId = targetId;
                cachedPathValid = true;
                lastWaypointSkipAt = 0;
                if (destination == Destination.POINT) {
                    pointZ = resolvedPointZ(pointZ, cachedPath);
                }
                if (recovery) {
                    stuckReplanFound.increment();
                    pendingPathRecovery = false;
                }
                clearPathFailureContext();
                logPathResult("FOUND points=" + cachedPath.length, elapsed, requestId, obstacleVersion);
            }
        } catch (CancellationException ignored) {
            if (!recovery) {
                cachedPath = null;
                cachedPathValid = false;
            }
            pathRetryAt = System.currentTimeMillis() + PATH_RETRY_DELAY_MS;
            logPathResult("CANCELLED", elapsed, requestId, obstacleVersion);
        } catch (CompletionException e) {
            if (recovery) {
                stuckReplanFailed.increment();
            } else {
                cachedPath = null;
                cachedPathValid = false;
            }
            Throwable failure = e.getCause();
            if (PathService.isDefinitivePathFailure(failure)) {
                recordPathFailure(targetX, targetY, targetZ, true);
            } else {
                pathRetryAt = System.currentTimeMillis() + PATH_RETRY_DELAY_MS;
            }
            logPathResult(PathService.failureStatus(failure == null ? e : failure).name(), elapsed, requestId, obstacleVersion);
        } finally {
            if (pendingPath == request && pendingPathRequestId == requestId) {
                pendingPath = null;
                pendingPathTargetId = 0;
                pendingPathX = Float.NaN;
                pendingPathStartedAt = 0;
                pendingPathRequestId = 0;
                pendingPathObstacleVersion = 0;
                pendingPathRecovery = false;
            }
        }
    }

    private void logPathResult(String status, long elapsed, long requestId, long obstacleVersion) {
        if (owner.getAi2().isLogging()) {
            AI2Logger.info(owner.getAi2(), "PATH result status=" + status
                    + " elapsedMs=" + elapsed + " requestId=" + requestId + " obstacleVersion=" + obstacleVersion
                    + " targetId=" + pendingPathTargetId + " to=(" + pendingPathX + ',' + pendingPathY + ','
                    + pendingPathZ + ")");
        }
    }

    static boolean shouldAcceptPathResult(long requestId, long currentRequestId, long obstacleVersion,
            long currentObstacleVersion) {
        return requestId == currentRequestId && obstacleVersion == currentObstacleVersion;
    }

    static boolean isEmptyPathResult(float[][] path) {
        return path != null && path.length == 0;
    }

    static boolean shouldKeepPathResult(float[][] path, boolean blocked) {
        return path == null || !blocked;
    }

    static float resolvedPointZ(float requestedZ, float[][] path) {
        return path == null || path.length == 0 ? requestedZ : path[path.length - 1][2];
    }

    static float resolvedTargetZ(boolean spatialPath, float targetZ, float groundZ) {
        return spatialPath ? targetZ : groundZ;
    }

    static boolean reachedWaypoint(float targetX, float targetY, float targetZ, float actualX, float actualY, float actualZ) {
        return MathUtil.getDistance(targetX, targetY, targetZ, actualX, actualY, actualZ) <= MOVE_OFFSET;
    }

    static boolean shouldRetryFailedPath(float failedX, float failedY, float failedZ, float targetX, float targetY,
            float targetZ, long failedObstacleVersion, long obstacleVersion) {
        return !Float.isFinite(failedX) || failedObstacleVersion != obstacleVersion
                || MathUtil.getDistance(failedX, failedY, failedZ, targetX, targetY, targetZ) > 1.5f;
    }

    static boolean shouldReactToPathFailure(long firstFailureAt, boolean handled, long now) {
        return !handled && firstFailureAt > 0 && now - firstFailureAt > PATH_FAILURE_REACTION_DELAY_MS;
    }

    static boolean shouldFinishFailedPointMove(long firstFailureAt, boolean handled, boolean requestPending, long now) {
        return !requestPending && shouldReactToPathFailure(firstFailureAt, handled, now);
    }

    static boolean shouldTryPathAvoidance(long firstFailureAt, long lastAttemptAt, int attempts, long now) {
        return firstFailureAt > 0 && attempts < PATH_AVOIDANCE_MAX_ATTEMPTS
                && now - firstFailureAt >= PATH_AVOIDANCE_INTERVAL_MS
                && now - lastAttemptAt >= PATH_AVOIDANCE_INTERVAL_MS;
    }

    static float[] localAvoidanceTarget(float x, float y, float z, float targetX, float targetY, float targetZ, float step) {
        float dx = targetX - x;
        float dy = targetY - y;
        float distance = (float) Math.hypot(dx, dy);
        if (distance <= MOVE_OFFSET) {
            return null;
        }
        float ratio = Math.min(Math.max(0, step), distance) / distance;
        return new float[] {x + dx * ratio, y + dy * ratio, z + (targetZ - z) * ratio};
    }

    static long pathFailureStartedAt(long current, long now, boolean definitive) {
        return current == 0 ? now : current;
    }

    static boolean hasHomeReturnTimedOut(long startedAt, long timeout, long now) {
        return startedAt > 0 && now - startedAt >= timeout;
    }

    static boolean shouldTeleportFailedHomeReturn(boolean spReturn, long startedAt, long now) {
        return spReturn && hasHomeReturnTimedOut(startedAt, HOME_SP_RETURN_TIMEOUT_MS, now);
    }

    static boolean shouldFinishFailedHomeReturn(boolean enhancedHomeReturn, long firstFailureAt, boolean retryFailedPath,
            boolean requestPending) {
        return !enhancedHomeReturn && firstFailureAt > 0 && !retryFailedPath && !requestPending;
    }

    static boolean shouldCompleteHomeReturn(boolean pathFinished, boolean homeReached) {
        return pathFinished && homeReached;
    }

    static boolean shouldRequestHomePath(boolean cachedPathValid, boolean retryFailedPath, boolean requestPending,
            long now, long retryAt) {
        return !requestPending && !cachedPathValid && retryFailedPath && now >= retryAt;
    }

    static float returnSpeed(float baseSpeed, int percent, boolean returningToWaypoint) {
        return returningToWaypoint ? baseSpeed : baseSpeed * Math.max(0, percent) / 100f;
    }

    private float movementSpeed(Npc npc) {
        if (npc.getAi2().getState() != AIState.RETURNING) {
            return npc.getGameStats().getMovementSpeedFloat();
        }
        var definition = DataManager.NPC_PATH_BEHAVIOR_DATA == null ? null
                : DataManager.NPC_PATH_BEHAVIOR_DATA.get(npc.getNpcId());
        int percent = definition == null ? 150 : definition.returnSpeedPercent();
        return returnSpeed(npc.getGameStats().getMovementSpeedFloat(), percent, isReturningToWaypoint());
    }

    private static boolean isSpReturn(Npc npc) {
        var definition = DataManager.NPC_PATH_BEHAVIOR_DATA == null ? null
                : DataManager.NPC_PATH_BEHAVIOR_DATA.get(npc.getNpcId());
        return definition != null && "sp".equalsIgnoreCase(definition.maxChaseTime());
    }

    public void beginHomeReturn() {
        homeReturnStartedAt = System.currentTimeMillis();
    }

    public void clearHomeReturn() {
        homeReturnStartedAt = 0;
        homeReturnWaypoint = null;
        fullHealOnHomeReturn = false;
    }

    public void requestReturnToCurrentWaypoint() {
        homeReturnWaypoint = currentRoute != null && currentPoint >= 0 && currentPoint < currentRoute.size()
                ? currentRoute.get(currentPoint) : null;
    }

    public boolean isReturningToWaypoint() {
        return homeReturnWaypoint != null;
    }

    public Point3D getHomeReturnDestination() {
        if (homeReturnWaypoint != null) {
            return new Point3D(homeReturnWaypoint.getX(), homeReturnWaypoint.getY(), resolveRouteStepZ(homeReturnWaypoint));
        }
        SpawnTemplate spawn = owner.getSpawn();
        return new Point3D(spawn.getX(), spawn.getY(), spawn.getZ());
    }

    private float resolveRouteStepZ(RouteStep routeStep) {
        float z = routeStep.getZ();
        if (GeoDataConfig.GEO_ENABLE && GeoDataConfig.GEO_NPC_MOVE && !owner.isInFlyingState()) {
            return GameWorldServices.geoService().getZ(owner.getWorldId(), routeStep.getX(), routeStep.getY(), z - 1, 100f, 1);
        }
        return z;
    }

    public boolean isHomeReturnDestinationReached() {
        Point3D target = getHomeReturnDestination();
        double horizontalDistance = MathUtil.getDistance(owner.getX(), owner.getY(), target.getX(), target.getY());
        double distance = MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(), target.getX(), target.getY(), target.getZ());
        return isReturnDestinationReached(homeReturnWaypoint != null, horizontalDistance, distance);
    }

    static boolean isReturnDestinationReached(boolean returningToWaypoint, double horizontalDistance, double distance) {
        return (returningToWaypoint ? distance : horizontalDistance) <= MOVE_OFFSET;
    }

    public void requestFullHealOnHomeReturn() {
        fullHealOnHomeReturn = true;
    }

    public boolean consumeFullHealOnHomeReturn() {
        boolean heal = fullHealOnHomeReturn;
        fullHealOnHomeReturn = false;
        return heal;
    }

    public synchronized void clearPathFailureContext() {
        failedPathX = Float.NaN;
        failedPathY = Float.NaN;
        failedPathZ = Float.NaN;
        firstPathFailureAt = 0;
        failedPathObstacleVersion = 0;
        pathFailureHandled = false;
        lastPathAvoidanceAt = 0;
        pathAvoidanceAttempts = 0;
        clearStuckRecoveryState();
        resetStuckShadow();
    }

    private void recordPathFailure(float targetX, float targetY, float targetZ, boolean definitive) {
        boolean newFailure = shouldRetryFailedPath(failedPathX, failedPathY, failedPathZ, targetX, targetY, targetZ,
                failedPathObstacleVersion, cachedObstacleVersion);
        failedPathX = targetX;
        failedPathY = targetY;
        failedPathZ = targetZ;
        failedPathObstacleVersion = cachedObstacleVersion;
        long now = System.currentTimeMillis();
        if (newFailure) {
            lastPathAvoidanceAt = 0;
            pathAvoidanceAttempts = 0;
        }
        if (newFailure || firstPathFailureAt == 0 || definitive) {
            firstPathFailureAt = pathFailureStartedAt(newFailure ? 0 : firstPathFailureAt, now, definitive);
            pathFailureHandled = false;
        }
        pathRetryAt = now + PATH_RETRY_DELAY_MS;
    }

    public synchronized boolean tryPathPull(int targetId) {
        if (targetId <= 0) {
            return false;
        }
        if (pathPullTargetId != targetId) {
            pathPullTargetId = targetId;
            pathPullAttempts = 0;
        }
        return ++pathPullAttempts <= 5;
    }

    public synchronized void clearPathPullAttempts() {
        pathPullTargetId = 0;
        pathPullAttempts = 0;
    }

    private long currentObstacleVersion() {
        return GameWorldServices.pathService().obstacleVersion(owner.getWorldId(), owner.getInstanceId());
    }

    private void finishFailedHomeReturn() {
        Point3D target = getHomeReturnDestination();
        SpawnTemplate spawn = owner.getSpawn();
        byte heading = homeReturnWaypoint == null ? spawn.getHeading() : owner.getHeading();
        clearPathFailureContext();
        abortMove();
        com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(owner, target.getX(), target.getY(),
                target.getZ(), heading);
        owner.getAi2().onGeneralEvent(AIEventType.BACK_HOME);
    }

    static float[][] remainingPath(float[][] path, boolean reached) {
        if (!reached || path == null || path.length == 0) {
            return path;
        }
        if (path.length == 1) {
            return null;
        }
        float[][] remaining = new float[path.length - 1][];
        System.arraycopy(path, 1, remaining, 0, remaining.length);
        return remaining;
    }

    static float[][] remainingPath(float[][] path, int firstIndex) {
        if (path == null || firstIndex <= 0) {
            return path;
        }
        if (firstIndex >= path.length) {
            return null;
        }
        float[][] remaining = new float[path.length - firstIndex][];
        System.arraycopy(path, firstIndex, remaining, 0, remaining.length);
        return remaining;
    }

    static float[][] consumePath(float[][] current, float[][] movedPath) {
        return current == movedPath ? remainingPath(current, true) : current;
    }

    /**
     * 设置当前巡逻路线并重置点索引。
     * Set the current walk route and reset the point index.
     *
     * @param currentRoute 路线步骤列表 / Route step list
     */
    public void setCurrentRoute(List<RouteStep> currentRoute) {
        if (currentRoute == null) {
            AI2Logger.info(owner.getAi2(), String.format("MC: setCurrentRoute is setting route to null (NPC id: {})!!!", owner.getNpcId()));
        } else {
            this.currentRoute = currentRoute;
        }
        this.currentPoint = 0;
    }

    /**
     * 设置当前与上一路线步骤，处理编队偏移与地形高度。
     * Set current/previous route steps; handle formation offset and terrain Z.
     *
     * Current step
     * Previous step
     */
    public void setRouteStep(RouteStep paramRouteStep1, RouteStep paramRouteStep2) {
        Point2D localPoint2D = null;
        if (((Npc)this.owner).getWalkerGroup() != null) {
            if (((Npc)this.owner).getWalkerGroupShift() == null) {
                log.warn(I18n.get("log.351405aaadba", ((Npc)this.owner).getNpcId()));
                return;
            }
            localPoint2D = WalkerGroup.getLinePoint(new Point2D(paramRouteStep2.getX(), paramRouteStep2.getY()), new Point2D(paramRouteStep1.getX(), paramRouteStep1.getY()), ((Npc)this.owner).getWalkerGroupShift());
            this.pointZ = resolveRouteStepZ(paramRouteStep2);
            ((Npc)this.owner).getWalkerGroup().setStep((Npc)this.owner, paramRouteStep1.getRouteStep());
        } else {
            this.pointZ = resolveRouteStepZ(paramRouteStep1);
        }
        this.currentPoint = paramRouteStep1.getRouteStep() - 1;
        this.pointX = localPoint2D == null ? paramRouteStep1.getX() : localPoint2D.getX();
        this.pointY = localPoint2D == null ? paramRouteStep1.getY() : localPoint2D.getY();
        this.destination = Destination.POINT;
        this.walkPause = paramRouteStep1.getRestTime();
    }

    /**
     * 返回当前路线点索引。
     * Return the current route point index.
     *
     * Point index
     */
    public int getCurrentPoint() {
        return this.currentPoint;
    }

    /**
     * 是否已到达当前目标点。
     * Whether the current target point has been reached.
     *
     * Whether reached
     */
    public boolean isReachedPoint() {
        return MathUtil.getDistance(((Npc)this.owner).getX(), ((Npc)this.owner).getY(), ((Npc)this.owner).getZ(), this.pointX, this.pointY, this.pointZ) < (double)0.05f;
    }

    /**
     * 选择下一巡逻步骤；路线为空时尝试重建。
     * Choose the next walk step; rebuild the route when empty.
     */
    public void chooseNextStep() {
        int oldPoint = this.currentPoint;
        if (this.currentRoute == null) {
            WalkerTemplate template;
            WalkManager.stopWalking((NpcAI2)((Npc)this.owner).getAi2());
            if (!WalkerFormator.processClusteredNpc((Npc)this.owner, ((Npc)this.owner).getWorldId(), ((Npc)this.owner).getInstanceId()) && (template = DataManager.WALKER_DATA.getWalkerTemplate(((Npc)this.owner).getSpawn().getWalkerId())) != null) {
                this.currentRoute = template.getRouteSteps();
            }
            if (this.currentRoute == null) {
                log.warn(I18n.get("log.6b808206626a", ((Npc)this.owner).getNpcId(), oldPoint));
                return;
            }
        }
        this.currentPoint = this.currentPoint < this.currentRoute.size() - 1 ? ++this.currentPoint : 0;
        this.setRouteStep(this.currentRoute.get(this.currentPoint), this.currentRoute.get(oldPoint));
    }

    /**
     * 返回当前路线点停顿时间（毫秒）。
     * Return the current route-step rest time in milliseconds.
     *
     * Rest time ms
     */
    public int getWalkPause() {
        return this.walkPause;
    }

    /**
     * 是否正在转向（位于路线起点）。
     * Whether direction is changing (at route start).
     *
     * Whether changing direction
     */
    public boolean isChangingDirection() {
        return this.currentPoint == 0;
    }

    /**
     * 返回目标 X；未启动时返回当前位置。
     * Return target X; current position when not started.
     *
     * Target X
     */
    @Override
    public final float getTargetX2() {
        return this.started.get() ? this.targetDestX : ((Npc)this.owner).getX();
    }

    /**
     * 返回目标 Y；未启动时返回当前位置。
     * Return target Y; current position when not started.
     *
     * Target Y
     */
    @Override
    public final float getTargetY2() {
        return this.started.get() ? this.targetDestY : ((Npc)this.owner).getY();
    }

    /**
     * 返回目标 Z；未启动时返回当前位置。
     * Return target Z; current position when not started.
     *
     * Target Z
     */
    @Override
    public final float getTargetZ2() {
        return this.started.get() ? this.targetDestZ : ((Npc)this.owner).getZ();
    }

    /**
     * 是否正在跟随目标对象。
     * Whether currently following a target object.
     *
     * Whether following
     */
    public boolean isFollowingTarget() {
        return this.destination == Destination.TARGET_OBJECT;
    }

    /**
     * 记录当前位置为回退步（返回中不记录）。
     * Store the current position as a back-step (skipped while returning).
     */
    public void storeStep() {
        if (((Npc)this.owner).getAi2().getState() == AIState.RETURNING) {
            return;
        }
        if (this.lastSteps == null) {
            this.lastSteps = new LastUsedCache(10);
        }
        Point3D currentStep = new Point3D(((Npc)this.owner).getX(), ((Npc)this.owner).getY(), ((Npc)this.owner).getZ());
        if (((Npc)this.owner).getAi2().isLogging()) {
            AI2Logger.moveinfo((Creature)this.owner, "store back step: X=" + ((Npc)this.owner).getX() + " Y=" + ((Npc)this.owner).getY() + " Z=" + ((Npc)this.owner).getZ());
        }
        if (this.stepSequenceNr == 0 || MathUtil.getDistance(this.lastSteps.get(this.stepSequenceNr), currentStep) >= 5.0) {
            this.stepSequenceNr = (byte)(this.stepSequenceNr + 1);
            this.lastSteps.put(this.stepSequenceNr, currentStep);
        }
    }

    /**
     * 取回上一个回退步并设为目标；无记录时回退到出生点。
     * Recall the previous back-step as destination; fall back to spawn when none.
     *
     * Recalled point
     */
    public Point3D recallPreviousStep() {
        Point3D result =  stepSequenceNr == 0 ? null : lastSteps.get(stepSequenceNr--);
        Point3D point3D;
        if (this.lastSteps == null) {
            this.lastSteps = new LastUsedCache(10);
        }
        if (this.stepSequenceNr == 0) {
            point3D = null;
        } else {
            byte by = this.stepSequenceNr;
            this.stepSequenceNr = (byte)(by - 1);
            point3D = result = this.lastSteps.get(by);
        }
        if (result == null) {
            if (((Npc)this.owner).getAi2().isLogging()) {
                AI2Logger.moveinfo((Creature)this.owner, "recall back step: spawn point");
            }
            this.targetDestX = ((Npc)this.owner).getSpawn().getX();
            this.targetDestY = ((Npc)this.owner).getSpawn().getY();
            this.targetDestZ = ((Npc)this.owner).getSpawn().getZ();
            result = new Point3D(this.targetDestX, this.targetDestY, this.targetDestZ);
        } else {
            if (((Npc)this.owner).getAi2().isLogging()) {
                AI2Logger.moveinfo((Creature)this.owner, "recall back step: X=" + result.getX() + " Y=" + result.getY() + " Z=" + result.getZ());
            }
            this.targetDestX = result.getX();
            this.targetDestY = result.getY();
            this.targetDestZ = result.getZ();
        }
        return result;
    }

    /**
     * 清空回退步缓存与移动掩码。
     * Clear the back-step cache and movement mask.
     */
    public void clearBackSteps() {
        this.stepSequenceNr = 0;
        this.lastSteps = null;
        this.movementMask = 0;
    }

    /**
     * NPC 技能施放时不额外修改移动状态。
     * NPC skill casts do not alter movement state by default.
     */
    @Override
    public void skillMovement() {
    }
}
