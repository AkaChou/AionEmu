package com.aionemu.gameserver.controllers.movement;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.handler.FollowEventHandler;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.handler.TargetEventHandler;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
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
import com.aionemu.gameserver.spawnengine.WalkerGroupShift;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.collections.LastUsedCache;
import com.aionemu.gameserver.world.geo.path.PathService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
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
    /** 地面与路径高度来自不同量化层时允许的到点高度误差。 / Arrival Z tolerance for differently quantized ground/path layers. */
    private static final float GROUND_POINT_Z_TOLERANCE = 0.25f;
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
    /** 行走贴地短段：前视 = 本 tick 实测位移 × 该倍数（>1 保证客户端不会在下个包到达前走到点）。 / Walker stream lookahead = measured per-tick travel × this factor (>1 keeps the client from reaching the target before the next packet). */
    static final float WALK_GROUND_STREAM_LEAD_STEPS = 1.3f;
    /**
     * 行走贴地短段：前视距离下限（米）。
     * 客户端对 NPC 移动包有「接近目标即判定到达」的行为：目标太近时每 tick 判到达 → 停-走-停
     * （2026-10-06 实机：前视 0.33m 时多个巡逻「卡一下→原地走→前进一小段」）。1m 是实机验证
     * 不触发该判定的下限；把前视压到 1m 以下换台阶贴合度前，必须先用实机 A/B 找到不触发的不卡点。
     * Minimum lookahead distance (m). The client treats very near targets as already reached
     * (stop-start stutter, observed with a 0.33m lookahead); 1m is the field-proven floor.
     */
    static final float WALK_GROUND_STREAM_MIN_LOOKAHEAD = 1.0f;
    /**
     * 行走贴地短段：收尾段停发余量（米）——距航点不足该值后不再补发（目标已在上一包给到航点本身）。
     * 客户端「接近目标即判定到达」同样作用于收尾段：若继续补发「距航点 0.1–0.3m」的包，客户端
     * 会在每个包上判到达停一下再起步，到点前出现微抖（2026-10-06/07 实机 203111 收尾段）。
     * 0.75m 取在实机触发点（0.33m）之上、前视下限（1m）之下：最后一个补发包的目标仍是航点本身，
     * 客户端在无人补发的最后一段走完并恰在航点停下。
     * Final-approach stop margin (m): below this remaining distance the stream stops and the client
     * walks the last stretch to the waypoint (its last target) on its own.
     */
    private static final float WALK_GROUND_STREAM_FINAL_MARGIN = 0.75f;
    /** 行走态转身平滑：每个移动 tick 的最大转角（度）——拐角处朝向逐 tick 过渡，避免客户端原地转身动画。 / Max walker heading change per movement tick (degrees); smooths corner turns. */
    private static final float WALK_HEADING_STEP_DEGREES = 12.0f;
    /** 行走者每步碰撞解算：步高抬升的单步/上限（米）与最大尝试次数（原版 fun_043：默认 1.5、上限 2.0、最多 9 次）。 / Walker per-step collision lift: step/max in meters and attempts (retail fun_043: 1.5 default, 2.0 cap, 9 tries). */
    static final float WALKER_COLLISION_LIFT_STEP = 1.5f;
    static final float WALKER_COLLISION_LIFT_MAX = 2.0f;
    static final int WALKER_COLLISION_LIFT_ATTEMPTS = 9;
    /** 行走者每步碰撞解算：抬升后向下打地面的探测下落深度（米）。 / Downward ground-probe drop after a lift (m). */
    private static final float WALKER_COLLISION_GROUND_PROBE_DROP = 3f;
    /** 编队偏移近似零容差（米）：|shift| 小于该值视为队长站位（与航点重合）。 / Approximate zero formation shift (m): below this the member stands on the route point itself (leader). */
    static final float FORMATION_SHIFT_EPSILON = 0.01f;
    /** 编队路点暂停停包的截止余量（毫秒）：停包排在「客户端走完最后一条移动包」的估计时刻 + 该余量（覆盖包延迟与一个 AI tick 的调度抖动）。原版以移动包截止驱动停包，本余量是估计口径下的小补量。 / Margin (ms) over the estimated client-walk-finish instant for a formation-waypoint pause stop: the stop is scheduled at the last move packet's deadline plus this margin (packet latency + one AI tick of scheduling jitter). Retail drives the stop off the move-packet deadline; this margin is the small allowance of our estimated variant. */
    static final long WALKER_STOP_DEADLINE_MARGIN_MS = 100;
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
    /** 足迹采样步长（米）：跟随目标位移超过该值记录一个新足迹。 / Sampling step distance for player follow trail (meters). */
    public static final float TRAIL_STEP_DISTANCE = 2.0f;
    /** 足迹点踩中消费阈值（米）：NPC 接近当前足迹点时出队并切换下一个。 / Arrival distance to consume a trail point (meters). */
    public static final float TRAIL_ARRIVE_DISTANCE = 1.2f;
    /** 最大足迹缓存点数。 / Maximum breadcrumb points kept in memory. */
    public static final int TRAIL_MAX_POINTS = 20;
    /** 跟随严重脱节瞬移兜底距离（米）。 / Distance threshold to trigger catch-up teleport when line of sight blocked (meters). */
    public static final float FOLLOW_CATCHUP_TELEPORT_DISTANCE = 20.0f;
    /** 跟随绝对超距瞬移兜底距离（米），防止达到 50 米任务判定失败。 / Absolute distance threshold to force catch-up teleport before quest 50m fail. */
    public static final float FOLLOW_ABSOLUTE_TELEPORT_DISTANCE = 35.0f;
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
    /**
     * 本段行走是否需要沿 Path（setRouteStep 时按配置模式与 PATH-LoS 判定）。
     * 行走 NPC（WALK_PATH）沿 Path 推进的原版语义开关：blocked 模式只对被挡段启用，always 全段启用。
     * Whether the current walk leg needs the Path, decided in setRouteStep from the configured mode and the
     * PATH line-of-sight check. Retail walkers follow the Path connectivity graph between waypoints.
     */
    private boolean walkerLegNeedsPath;
    /**
     * 「本段需要沿 Path」判定与预取对应的航点（同一航点重复刷新去重用；NaN = 尚未记录）。
     * Waypoint the current walker-leg Path verdict and pre-fetch belong to (duplicate-refresh
     * de-duplication; NaN = nothing recorded yet).
     */
    private float walkerLegPathKeyX = Float.NaN;
    private float walkerLegPathKeyY = Float.NaN;
    private float walkerLegPathKeyZ = Float.NaN;
    /**
     * 编队暂停延迟停包的代数：暂停/恢复/重置时递增，使早先排定的停包任务失效。
     * Generation of the delayed formation-pause stop: bumped on every pause/reset/resume so earlier
     * scheduled stop tasks become no-ops.
     */
    private volatile long pauseStopGeneration;
    /** 最近一条移动包走完的估计时刻（毫秒时间戳；0=从未下发移动包）。 / Estimated instant (ms) the client finishes walking the last issued move packet (0 = none issued yet). */
    volatile long movePacketDeadlineMs;
    /** 当前巡逻路线 / Current walk route */
    List<RouteStep> currentRoute;
    /** 当前路线点索引 / Current route point index
	 * -- GETTER --
	 *  返回当前路线点索引。
	 *  Return the current route point index.
	 */
	@Getter
    int currentPoint;
    /** 路线点停顿毫秒 / Route-step rest time ms
     * -- GETTER --
     *  返回当前路线点停顿时间（毫秒）。
     *  Return the current route-step rest time in milliseconds.
     */
    @Getter
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
    /** 行走态平滑朝向（连续度数值，可负；/3 后写入 heading 字节）与目标朝向；NaN = 未处于平滑。 / Smoothed walker heading (continuous degrees) and its target; NaN = not smoothing. */
    private float walkHeadingDegrees = Float.NaN;
    private float walkHeadingTargetDegrees = Float.NaN;
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
    /** 跟随足迹历史队列。 / FIFO breadcrumb trail for escort and follow movement. */
    private final Deque<Point3D> followTrail = new ArrayDeque<>();

    /**
     * 使用指定 NPC 构造控制器。
     * Construct the controller for the given NPC.
     * @param owner NPC 所有者 / NPC owner
     */
    public NpcMoveController(Npc owner) {
        super(owner);
    }

    public boolean isStarted() {
        return this.started.get();
    }

    public boolean isMovingToTarget() {
        return this.destination == Destination.TARGET_OBJECT && this.started.get();
    }

    /**
     * 移动目的地类型。
     * Destination type for movement.
     */
    private enum Destination {
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
     * @param target 跟随目标 / Follow target
     */
    private void applyFollow(VisibleObject target) {
        if ((this._followMotor != null && this._followMotor._target == target)) {
            return;
        }
        if (this._followMotor != null) {
            this._followMotor.stop();
        }
        this._followMotor = new FollowMotor(Global.MovementProcessor, this.owner, target);
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
            clearFollowTrail();
        }
    }

    /**
     * 路径是否包含中间路点。
     * Whether the path has intermediate waypoints.
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
     * @param currentMask 当前掩码 / Current mask
     * @param newMask 新掩码 / New mask
     * @param destinationChanged 目标是否变化 / Whether destination changed
     * @return 是否广播 / Whether to broadcast
     */
    static boolean shouldBroadcastMovement(byte currentMask, byte newMask, boolean destinationChanged) {
        return currentMask != newMask || destinationChanged;
    }

    static boolean shouldRestartMovement(boolean enhancedHomeReturn, boolean chasingTarget, boolean walkingRoute,
            byte currentMask) {
        return enhancedHomeReturn ? currentMask == MovementMask.IMMEDIATE
                : (!walkingRoute && !chasingTarget) || currentMask == MovementMask.IMMEDIATE;
    }

    static boolean shouldUsePath(boolean pathEnabled, boolean walkingRoute) {
        return pathEnabled && !walkingRoute;
    }

    /**
     * 行走 NPC（WALK_PATH）本段是否沿 Path 推进：off 恒否；always 恒是；blocked 仅当本段被判定需要 Path。
     * Whether a walking NPC follows the Path on this leg: off never, always always, blocked only when the
     * leg was judged to need it.
     * @param mode 配置模式（off/blocked/always）/ configured mode
     * @param legNeedsPath 本段 PATH-LoS 判定结果 / whether this leg needs the Path
     * @return 是否沿 Path / whether to follow the Path
     */
    static boolean shouldUseWalkerPath(String mode, boolean legNeedsPath) {
        return "always".equalsIgnoreCase(mode) || ("blocked".equalsIgnoreCase(mode) && legNeedsPath);
    }

    /**
     * 是否为「带非零编队偏移」的编队跟随者：跟随者航点被偏移到成员站位（本路线最多 8m，非路线点本身），
     * 保持直线成员段（AIM-009/010 语义）；队长（shift 0,0）站位与航点重合，按独行语义可沿 Path。
     * 编队内缺 shift 属异常态（setRouteStep 会 warn），保守按跟随者排除。
     * Whether the walker is an offset formation follower: followers stand up to 8 m off the route point and
     * keep straight member segments (AIM-009/010 semantics); the leader (shift 0,0) stands on the route
     * point itself and is treated like a solo walker. A missing shift inside a group is a broken state
     * (setRouteStep warns) and is conservatively treated as a follower.
     * @param hasWalkerGroup 是否属于编队 / whether inside a walker group
     * @param hasShift 是否带偏移对象 / whether a shift object exists
     * @param sagittalShift 矢状（前后）偏移（米）/ sagittal (front/back) shift in meters
     * @param coronalShift 冠状（左右）偏移（米）/ coronal (left/right) shift in meters
     * @return 跟随者返回 true / true for a follower
     */
    static boolean isFormationFollower(boolean hasWalkerGroup, boolean hasShift, float sagittalShift,
            float coronalShift) {
        if (!hasWalkerGroup) {
            return false;
        }
        return !hasShift || Math.abs(sagittalShift) >= FORMATION_SHIFT_EPSILON
                || Math.abs(coronalShift) >= FORMATION_SHIFT_EPSILON;
    }

    /**
     * 行走 NPC 是否具备沿 Path 预取的静态资格：PATH 总开关开、非编队跟随者、非飞行、非空间寻路。
     * Whether the walking NPC is statically eligible for Path pre-fetch: PATH enabled, not an offset
     * formation follower, not flying, not using spatial (3-D flight/swim) pathfinding.
     * @param pathEnabled PATH 总开关 / global PATH toggle
     * @param formationFollower 非零偏移编队跟随者 / offset formation follower
     * @param flying 飞行中 / flying
     * @param spatialPath 空间寻路 / spatial pathfinding
     * @return 有资格返回 true / true when eligible
     */
    static boolean walkerPathEligible(boolean pathEnabled, boolean formationFollower, boolean flying,
            boolean spatialPath) {
        return pathEnabled && !formationFollower && !flying && !spatialPath;
    }

    /**
     * 是否为同一航点（重复刷新去重口径）：平面 5cm、垂直 0.5m 内视为同一点——重复刷新会按当次坐标
     * 重解地面 Z，同一点会有厘米级抖动；NaN 键（未记录）恒不相等。
     * Whether two waypoints count as the same leg target for duplicate-refresh de-duplication: 5 cm
     * horizontally / 0.5 m vertically (re-resolving the leg ground Z wobbles Z by centimeters); NaN keys
     * never compare equal.
     * @param x1 航点 X / waypoint X
     * @param y1 航点 Y / waypoint Y
     * @param z1 航点 Z / waypoint Z
     * @param x2 已记录 X / recorded X
     * @param y2 已记录 Y / recorded Y
     * @param z2 已记录 Z / recorded Z
     * @return 同一航点返回 true / true when the same target
     */
    static boolean sameWalkerLegTarget(float x1, float y1, float z1, float x2, float y2, float z2) {
        return Math.abs(x1 - x2) < 0.05f && Math.abs(y1 - y2) < 0.05f && Math.abs(z1 - z2) < 0.5f;
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
        return shouldUseAttackSlot(spatialPath, attackDistance, false);
    }

    static boolean shouldUseAttackSlot(boolean spatialPath, float attackDistance, boolean following) {
        return !following && !spatialPath && attackDistance > 0.75f && attackDistance <= TARGET_SLOT_MAX_ATTACK_RANGE;
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

    static boolean shouldAdjustGeoHeight(boolean enhancedHomeReturn, boolean returning, boolean spawnDestination) {
        return enhancedHomeReturn && returning || !returning && !spawnDestination;
    }

    static boolean shouldApplyGeoHeightCorrection(boolean enhancedHomeReturn, boolean returning,
            boolean spawnDestination, boolean hasPathWaypoint) {
        return !hasPathWaypoint && shouldAdjustGeoHeight(enhancedHomeReturn, returning, spawnDestination);
    }

    static boolean shouldSkipStationaryRandomWalkStep(float ownerX, float ownerY, float ownerZ,
            float newX, float newY, float newZ, int randomWalk) {
        return randomWalk > 0 && ownerX == newX && ownerY == newY && ownerZ == newZ;
    }

    /**
     * 行走贴地短段本 tick 是否需要补发：行走态、未到点，且距航点仍有收尾余量。
     * 收尾余量内的最后一段不再补发——目标已在上一包给到航点本身，客户端自己走完并恰在航点停下；
     * 继续补发会让包目标落进客户端「接近目标即判定到达」区间，到点前产生停-走微抖。
     * Whether a ground-following walker needs a stream target this tick.
     * @param walkPathSubState 是否行走子状态 / whether substate is WALK_PATH
     * @param reachedWaypoint 是否已到点 / whether the waypoint was reached
     * @param remaining 距航点剩余距离（米）/ remaining distance to the waypoint (m)
     * @return 是否补发 / whether to send
     */
    static boolean shouldStreamWalkGround(boolean walkPathSubState, boolean reachedWaypoint, float remaining) {
        return walkPathSubState && !reachedWaypoint && remaining > WALK_GROUND_STREAM_FINAL_MARGIN;
    }

    /**
     * 行走贴地短段的客户端目标：剩余距离大于前视距离时取前视点（X/Y 线性 + Z 线性回退，调用方再贴地）；
     * 收尾段（剩余 ≤ 前视）直接取航点本身——客户端在航点处到点停下，避免「提前停住 → 下一段起步
     * 回吸 + 原地转身」的观感（2026-10-06 实机 205294）。收尾段不再补发的门槛见
     * {@link #WALK_GROUND_STREAM_FINAL_MARGIN}。
     * Walker stream target: the lookahead point while the leg is long, or the waypoint itself during the
     * final approach so the client stops exactly on the waypoint.
     * @param fromX 起点 X / from X
     * @param fromY 起点 Y / from Y
     * @param fromZ 起点 Z / from Z
     * @param toX 终点 X / destination X
     * @param toY 终点 Y / destination Y
     * @param toZ 终点 Z / destination Z
     * @param lookahead 前视距离 / lookahead distance
     * @return 目标点 {x,y,z} / target point {x,y,z}
     */
    static float[] walkGroundStreamTarget(float fromX, float fromY, float fromZ, float toX, float toY, float toZ,
            float lookahead) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float)Math.hypot(dx, dy);
        if (length <= lookahead) {
            return new float[] {toX, toY, toZ};
        }
        float fraction = lookahead / length;
        return new float[] {fromX + dx * fraction, fromY + dy * fraction, fromZ + (toZ - fromZ) * fraction};
    }

    /**
     * 行走贴地短段的前视距离：每 tick 行程 × 1.3，带下限。
     * Walker stream lookahead: per-tick travel × 1.3, floored.
     * @param travelPerTick 每 tick 行程（米）/ travel per movement tick (m)
     * @return 前视距离（米）/ lookahead distance (m)
     */
    static float walkStreamLookahead(float travelPerTick) {
        return Math.max(WALK_GROUND_STREAM_MIN_LOOKAHEAD, travelPerTick * WALK_GROUND_STREAM_LEAD_STEPS);
    }

    /**
     * 行走沿 Path 推进时的短段剩余量：从当前位置沿「剩余折线 → 本段航点」的总弧长（米）。
     * 与直线版（直接到目标点的距离）同语义；Path 为 null 时退化为到航点的水平距离。
     * Remaining walk distance while following a Path: total arc length from the current position along the
     * remaining polyline to the leg waypoint (m). With a null Path it degenerates to the straight distance.
     * @param fromX 当前位置 X / current X
     * @param fromY 当前位置 Y / current Y
     * @param fromZ 当前位置 Z / current Z
     * @param path 剩余折线（含下一节点在 [0]）/ remaining polyline, next node at [0]
     * @param waypointX 本段航点 X / leg waypoint X
     * @param waypointY 本段航点 Y / leg waypoint Y
     * @param waypointZ 本段航点 Z / leg waypoint Z
     * @return 剩余弧长（米）/ remaining arc length (m)
     */
    static float walkPathRemaining(float fromX, float fromY, float fromZ, float[][] path,
            float waypointX, float waypointY, float waypointZ) {
        float remaining = 0;
        float lastX = fromX;
        float lastY = fromY;
        if (path != null) {
            for (float[] node : path) {
                remaining += (float) Math.hypot(node[0] - lastX, node[1] - lastY);
                lastX = node[0];
                lastY = node[1];
            }
        }
        return remaining + (float) Math.hypot(waypointX - lastX, waypointY - lastY);
    }

    /**
     * 行走沿 Path 推进时的短段目标：沿「剩余折线 → 本段航点」按弧长取前视点；
     * 整条折线（含收尾到航点的一段）不超过前视距离时返回航点本身——与
     * {@link #walkGroundStreamTarget} 的收尾分支同语义（Path 为 null 时两者等价）。
     * Stream target while following a Path: the lookahead point by arc length along the remaining polyline
     * towards the leg waypoint; returns the waypoint itself when the whole polyline is within the lookahead,
     * matching {@link #walkGroundStreamTarget} (the two are equivalent with a null Path).
     * @param fromX 当前位置 X / current X
     * @param fromY 当前位置 Y / current Y
     * @param fromZ 当前位置 Z / current Z
     * @param path 剩余折线（含下一节点在 [0]）/ remaining polyline, next node at [0]
     * @param waypointX 本段航点 X / leg waypoint X
     * @param waypointY 本段航点 Y / leg waypoint Y
     * @param waypointZ 本段航点 Z / leg waypoint Z
     * @param lookahead 前视距离（米）/ lookahead distance (m)
     * @return 短段目标点 {x,y,z} / stream target point {x,y,z}
     */
    static float[] walkPathStreamTarget(float fromX, float fromY, float fromZ, float[][] path,
            float waypointX, float waypointY, float waypointZ, float lookahead) {
        float remaining = lookahead;
        float lastX = fromX;
        float lastY = fromY;
        float lastZ = fromZ;
        if (path != null) {
            for (float[] node : path) {
                if (remaining <= 0) {
                    return new float[] {lastX, lastY, lastZ};
                }
                float segment = (float) Math.hypot(node[0] - lastX, node[1] - lastY);
                if (segment >= remaining) {
                    float fraction = segment <= 0 ? 1f : remaining / segment;
                    return new float[] {lastX + (node[0] - lastX) * fraction, lastY + (node[1] - lastY) * fraction,
                            lastZ + (node[2] - lastZ) * fraction};
                }
                remaining -= segment;
                lastX = node[0];
                lastY = node[1];
                lastZ = node[2];
            }
        }
        float toWaypoint = (float) Math.hypot(waypointX - lastX, waypointY - lastY);
        if (toWaypoint <= remaining) {
            return new float[] {waypointX, waypointY, waypointZ};
        }
        float fraction = remaining / toWaypoint;
        return new float[] {lastX + (waypointX - lastX) * fraction, lastY + (waypointY - lastY) * fraction,
                lastZ + (waypointZ - lastZ) * fraction};
    }

    /**
     * 行走者每步碰撞解算的第 i 次抬升量（米）：i × 1.5，上限 2.0（原版 fun_043 的 fVar26 语义）。
     * Per-step collision lift for attempt i (m): i × 1.5 capped at 2.0 (retail fun_043 fVar26 semantics).
     * @param attempt 第几次尝试（从 1 起）/ attempt index (1-based)
     * @return 抬升量（米）/ lift in meters
     */
    static float walkerLiftOffset(int attempt) {
        return Math.min(attempt * WALKER_COLLISION_LIFT_STEP, WALKER_COLLISION_LIFT_MAX);
    }

    /**
     * 行走者每步碰撞/步高抬升解算（原版 fun_043 结构）：
     * ① 水平通行检测（{@link GeoMap#canPassWalker}：两端抬高 0.5m、跳过首个命中——地形小高差不挡、墙挡）；
     * ② 被挡 → 抬升循环 i=1..9（{@link #walkerLiftOffset}）在更高处重查；通过即向下打地面把移动点放回面上；
     * ③ 9 次仍冲突 → 返回最远可达点（{@link GeoMap#getClosestCollision}，含 0.5m 回退与贴地），
     * 即障碍前停住、不瞬移（原版「Too many collisions」分支的保守等价物：原版取值路径节点，这里不越障）。
     * 返回点仅在「被挡」时非 null——未被挡/无地图时调用方保持原线性推进点，由逐 tick 贴地接管。
     * Walker per-step collision / step-height lift resolution (retail fun_043 shape): (1) horizontal passability
     * via {@link GeoMap#canPassWalker} (both ends raised 0.5 m, first hit skipped — terrain micro-steps pass,
     * walls block); (2) when blocked, lift i×1.5 (cap 2.0) for up to 9 tries and drop the point back onto the
     * walkable surface; (3) after 9 tries return the closest collision point (0.5 m fallback + ground snap) so
     * the walker stops at the obstacle instead of phasing through it. Non-null only when blocked.
     * @param map 地理地图（可空）/ geo map (nullable)
     * @param instanceId 实例 ID / instance id
     * @param fromX 起点 X / from X
     * @param fromY 起点 Y / from Y
     * @param fromZ 起点 Z / from Z
     * @param toX 计划推进点 X / planned step X
     * @param toY 计划推进点 Y / planned step Y
     * @param toZ 计划推进点 Z / planned step Z
     * @return 解算后的移动点 {x,y,z}；null = 未受阻 / resolved step {x,y,z}; null when unobstructed
     */
    static float[] resolveWalkerCollisionStep(GeoMap map, int instanceId, float fromX, float fromY, float fromZ,
            float toX, float toY, float toZ) {
        if (map == null) {
            return null;
        }
        float limit = (float) Math.hypot(toX - fromX, toY - fromY) + 1f;
        if (map.canPassWalker(fromX, fromY, fromZ, toX, toY, toZ, limit, instanceId)) {
            return null;
        }
        for (int i = 1; i <= WALKER_COLLISION_LIFT_ATTEMPTS; i++) {
            float lifted = toZ + walkerLiftOffset(i);
            if (map.canPassWalker(fromX, fromY, fromZ, toX, toY, lifted, limit, instanceId)) {
                float groundZ = map.getZ(toX, toY, lifted, lifted - WALKER_COLLISION_GROUND_PROBE_DROP, instanceId);
                return Float.isNaN(groundZ) ? new float[] {toX, toY, lifted} : new float[] {toX, toY, groundZ};
            }
        }
        Vector3f closest = map.getClosestCollision(fromX, fromY, fromZ, toX, toY, toZ, true, false, instanceId,
                CollisionIntention.DEFAULT_COLLISIONS.getId(), null);
        return closest == null ? null : new float[] {closest.getX(), closest.getY(), closest.getZ()};
    }

    /**
     * 行走态朝向平滑：把当前朝向朝目标方向转动最多 maxStepDegrees（取最短转角）。
     * 返回值归一化到 (−180, 180]，与 atan2 原始取值范围一致（heading 字节 = 度数/3，保持原有编码语义）。
     * Walker heading smoothing step: rotate current towards target by at most maxStepDegrees along the
     * shortest arc; the result is normalised to (-180, 180], matching the atan2 convention the legacy
     * heading byte (degrees / 3) encoding relies on.
     * @param currentDegrees 当前朝向（度）/ current heading (degrees)
     * @param targetDegrees 目标朝向（度）/ target heading (degrees)
     * @param maxStepDegrees 单次最大转角（度）/ max rotation per step (degrees)
     * @return 过渡后的朝向（度）/ stepped heading (degrees)
     */
    static float stepHeadingDegrees(float currentDegrees, float targetDegrees, float maxStepDegrees) {
        float delta = (targetDegrees - currentDegrees) % 360f;
        if (delta > 180f) {
            delta -= 360f;
        } else if (delta < -180f) {
            delta += 360f;
        }
        if (delta > maxStepDegrees) {
            delta = maxStepDegrees;
        } else if (delta < -maxStepDegrees) {
            delta = -maxStepDegrees;
        }
        float next = (currentDegrees + delta) % 360f;
        if (next > 180f) {
            next -= 360f;
        } else if (next <= -180f) {
            next += 360f;
        }
        return next;
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
        pauseStopGeneration++;
        if (started.compareAndSet(false, true)) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "MC: moveToNextPoint started");
            }
            destination = Destination.POINT;
            resetTargetTracking();
            // 行走者本段预取的 Path 必须活过本次启动：WalkManager 在 setRouteStep 之后立即调用本方法，
            // 无条件的 resetPath 会丢弃刚预取（或在途）的路径，使首个移动 tick 的 moveAlongPath 判定
            // 「无有效路径」再发一次 A*（实机 2026-10-08：每个被挡航段成对 PATH request，第二次即来自
            // tick）。仅当在途/缓存路径的目标仍是当前航点时保留。
            // A walker leg's pre-fetched Path must survive this call: WalkManager invokes moveToNextPoint
            // right after setRouteStep, and an unconditional resetPath() drops the fresh (or in-flight)
            // request, so the first movement tick's moveAlongPath sees no valid path and issues a second A*
            // (field logs 2026-10-08: paired PATH requests on every blocked leg, the second from the tick).
            // The path is kept only while its target is still the current leg waypoint.
            if (!walkerPathTargetsCurrentLeg()) {
                resetPath();
            }
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
        if (abortTimedOutHomeReturn()) {
            return;
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
                boolean following = owner != null && owner.getAi2() != null
                        && owner.getAi2().getState() == AIState.FOLLOWING;
                if (following) {
                    if (MathUtil.isIn3dRange(owner, creature, FollowEventHandler.CLOSE_FOLLOW_RANGE)) {
                        clearFollowTrail();
                        abortMove();
                        return;
                    }
                    if (tryFollowCatchupTeleport(creature)) {
                        return;
                    }
                    float targetZ = getTargetZ(GameWorldServices.pathService().usesSpatialPath(owner), creature);
                    updateFollowTrail(creature, targetZ);
                    advanceFollowTrail(creature, targetZ);
                    Point3D waypoint = selectFollowWaypoint(creature, targetZ);
                    pointX = waypoint.getX();
                    pointY = waypoint.getY();
                    pointZ = waypoint.getZ();
                    moveToLocation(pointX, pointY, pointZ, FollowEventHandler.CLOSE_FOLLOW_RANGE);
                    break;
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
                            // Keep walking along the old path so the NPC does not stand still while a new path is computed.
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
                        offset = FollowEventHandler.CLOSE_FOLLOW_RANGE;
                        moveToLocation(pointX, pointY, pointZ, offset);
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
        boolean following = owner != null && owner.getAi2() != null
                && owner.getAi2().getState() == AIState.FOLLOWING;
        if (shouldUseAttackSlot(spatialPath, attackDistance, following)) {
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
        offset = following ? FollowEventHandler.CLOSE_FOLLOW_RANGE : attackDistance;
        if (following) {
            updateFollowTrail(target, targetZ);
            Point3D waypoint = selectFollowWaypoint(target, targetZ);
            pointX = waypoint.getX();
            pointY = waypoint.getY();
            pointZ = waypoint.getZ();
        } else {
            pointX = target.getX();
            pointY = target.getY();
            pointZ = targetZ;
        }
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

    public void clearFollowTrail() {
        followTrail.clear();
    }

    public int followTrailSize() {
        return followTrail.size();
    }

    void updateFollowTrail(Creature target, float targetZ) {
        if (owner == null || target == null
                || target.getWorldId() != owner.getWorldId()
                || target.getInstanceId() != owner.getInstanceId()) {
            followTrail.clear();
            return;
        }
        float tx = target.getX();
        float ty = target.getY();
        if (followTrail.isEmpty()) {
            float distToOwner = (float) MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(), tx, ty, targetZ);
            if (distToOwner >= TRAIL_STEP_DISTANCE) {
                followTrail.addLast(new Point3D(tx, ty, targetZ));
            }
        } else {
            Point3D last = followTrail.peekLast();
            float distFromLast = (float) MathUtil.getDistance(last.getX(), last.getY(), last.getZ(), tx, ty, targetZ);
            if (distFromLast >= TRAIL_STEP_DISTANCE) {
                if (followTrail.size() >= TRAIL_MAX_POINTS) {
                    followTrail.pollFirst();
                }
                followTrail.addLast(new Point3D(tx, ty, targetZ));
            }
        }
    }

    Point3D selectFollowWaypoint(Creature target, float targetZ) {
        if (followTrail.isEmpty()) {
            return new Point3D(target.getX(), target.getY(), targetZ);
        }
        while (!followTrail.isEmpty()) {
            Point3D head = followTrail.peekFirst();
            if (head == null) {
                break;
            }
            float dist = (float) MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(),
                    head.getX(), head.getY(), head.getZ());
            if (dist <= TRAIL_ARRIVE_DISTANCE) {
                followTrail.pollFirst();
                continue;
            }
            if (followTrail.size() >= 2) {
                Iterator<Point3D> it = followTrail.iterator();
                it.next();
                Point3D second = it.next();
                float distSecond = (float) MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(),
                        second.getX(), second.getY(), second.getZ());
                if (distSecond < dist) {
                    followTrail.pollFirst();
                    continue;
                }
            }
            break;
        }
        if (followTrail.isEmpty()) {
            return new Point3D(target.getX(), target.getY(), targetZ);
        }
        return followTrail.peekFirst();
    }

    boolean advanceFollowTrail(Creature target, float targetZ) {
        if (followTrail.isEmpty()) {
            return false;
        }
        Point3D head = followTrail.peekFirst();
        if (head == null) {
            return false;
        }
        float dist = (float) MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(),
                head.getX(), head.getY(), head.getZ());
        boolean shouldPop = dist <= TRAIL_ARRIVE_DISTANCE;
        if (!shouldPop && followTrail.size() >= 2) {
            Iterator<Point3D> it = followTrail.iterator();
            it.next();
            Point3D second = it.next();
            float distSecond = (float) MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(),
                    second.getX(), second.getY(), second.getZ());
            if (distSecond < dist) {
                shouldPop = true;
            }
        }
        if (shouldPop) {
            followTrail.pollFirst();
            Point3D next = followTrail.isEmpty() ? new Point3D(target.getX(), target.getY(), targetZ)
                    : followTrail.peekFirst();
            pointX = next.getX();
            pointY = next.getY();
            pointZ = next.getZ();
            resetPath();
            return true;
        }
        return false;
    }

    private boolean canPassDirectly(Creature target) {
        if (owner == null || target == null) {
            return false;
        }
        if (!GeoDataConfig.GEO_ENABLE) {
            return followTrail.isEmpty();
        }
        return GameWorldServices.geoService().canPass(owner, target);
    }

    boolean tryFollowCatchupTeleport(Creature target) {
        if (owner == null || target == null) {
            return false;
        }
        float distance = (float) MathUtil.getDistance(owner, target);
        if (distance >= FOLLOW_CATCHUP_TELEPORT_DISTANCE && !canPassDirectly(target)) {
            catchupTeleportTo(target);
            return true;
        }
        if (distance >= FOLLOW_ABSOLUTE_TELEPORT_DISTANCE) {
            catchupTeleportTo(target);
            return true;
        }
        return false;
    }

    void catchupTeleportTo(Creature target) {
        if (owner == null || target == null) {
            return;
        }
        clearFollowTrail();
        clearPathFailureContext();
        clearStuckRecoveryState();
        resetStuckShadow();
        abortMove();
        float targetZ = target.getZ();
        if (GeoDataConfig.GEO_ENABLE && !owner.isFlying()) {
            targetZ = GameWorldServices.geoService().getZ(target);
        }
        float oldX = owner.getX();
        float oldY = owner.getY();
        float oldZ = owner.getZ();
        GameWorldBootstrapServices.world().updatePosition(owner, target.getX(), target.getY(), targetZ, target.getHeading());
        if (owner.getKnownList() != null) {
            PacketSendUtility.broadcastPacket(owner, new SM_MOVE(owner.getObjectId(), oldX, oldY, oldZ,
                    target.getX(), target.getY(), targetZ, target.getHeading(), MovementMask.IMMEDIATE));
        }
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
     * @param targetX 目标 X 坐标 / Target X
     * @param targetY 目标 Y 坐标 / Target Y
     * @param targetZ 目标 Z 坐标 / Target Z
     * @param offset 停止偏移 / Stop offset
     */
    protected void moveToLocation(float targetX, float targetY, float targetZ, float offset) {
        moveToLocation(targetX, targetY, targetZ, offset, null);
    }

    private void moveToLocation(float targetX, float targetY, float targetZ, float offset, float[][] path) {
        float ownerX = this.owner.getX();
        float ownerY = this.owner.getY();
        float ownerZ = this.owner.getZ();
        boolean intermediateWaypoint = hasIntermediateWaypoint(path);
        boolean pathWaypointTransition = intermediateWaypoint || previousIntermediateWaypoint;
        previousIntermediateWaypoint = intermediateWaypoint;
        boolean destinationChanged = targetX != this.targetDestX || targetY != this.targetDestY || targetZ != this.targetDestZ;
        boolean directionChanged = destinationChanged && shouldRestartMovement(
                AIConfig.ENHANCED_HOME_RETURN && destination == Destination.HOME,
                destination == Destination.TARGET_OBJECT,
                owner.getAi2().getSubState() == AISubState.WALK_PATH, movementMask);
        if (destinationChanged) {
            float newHeadingDegrees = (float)Math.toDegrees(Math.atan2(targetY - ownerY, targetX - ownerX));
            if (owner.getAi2().getSubState() == AISubState.WALK_PATH) {
                // 行走态转身平滑：新段朝向不瞬跳，逐 tick 以有限角速度转到新方向；起点取当前已生效朝向。
                // 客户端对负角朝向按原编码（度/3，可为负）解释；这里保持同一语义，只做连续过渡。
                // Walkers turn gradually: a sharp heading jump makes the client play its turn-in-place
                // animation (observed corner stutter), so step the heading towards the new leg.
                this.walkHeadingDegrees = this.heading * 3.0f;
                this.walkHeadingTargetDegrees = newHeadingDegrees;
            } else {
                this.walkHeadingTargetDegrees = Float.NaN;
                this.heading = (byte)(newHeadingDegrees / 3.0);
            }
        }
        if (!Float.isNaN(this.walkHeadingTargetDegrees)) {
            this.walkHeadingDegrees = stepHeadingDegrees(this.walkHeadingDegrees, this.walkHeadingTargetDegrees,
                    WALK_HEADING_STEP_DEGREES);
            this.heading = (byte)(this.walkHeadingDegrees / 3.0);
        }
        if (this.owner.getAi2().isLogging()) {
            AI2Logger.moveinfo(this.owner, "OLD targetDestX: " + this.targetDestX + " targetDestY: " + this.targetDestY + " targetDestZ " + this.targetDestZ);
        }
        if (targetX == 0.0f && targetY == 0.0f) {
            targetX = this.owner.getSpawn().getX();
            targetY = this.owner.getSpawn().getY();
            targetZ = this.owner.getSpawn().getEffectiveZ();
        }
        this.targetDestX = targetX;
        this.targetDestY = targetY;
        this.targetDestZ = targetZ;
        if (this.owner.getAi2().isLogging()) {
            AI2Logger.moveinfo(this.owner, "ownerX=" + ownerX + " ownerY=" + ownerY + " ownerZ=" + ownerZ);
            AI2Logger.moveinfo(this.owner, "targetDestX: " + this.targetDestX + " targetDestY: " + this.targetDestY + " targetDestZ " + this.targetDestZ);
        }
        float currentSpeed = movementSpeed(owner);
        long now = System.currentTimeMillis();
        long elapsedMillis = Math.max(1, now - this.lastMoveUpdate);
        float futureDistPassed = currentSpeed * elapsedMillis / 1000.0f;
        float dist = (float)MathUtil.getDistance(ownerX, ownerY, ownerZ, targetX, targetY, targetZ);
        if (this.owner.getAi2().isLogging()) {
            AI2Logger.moveinfo(this.owner, "futureDist: " + futureDistPassed + " dist: " + dist);
        }
        if (dist == 0.0f) {
            resetStuckShadow();
            pathStopSent = false;
            boolean pathCompleted = consumeWaypoint(path);
            if (this.owner.getAi2().getState() == AIState.RETURNING
                    && shouldCompleteHomeReturn(path == null || pathCompleted, isHomeReturnDestinationReached())) {
                if (this.owner.getAi2().isLogging()) {
                    AI2Logger.moveinfo(this.owner, "状态\u8fd4\u56de\uff1a\u4e2d\u6b62\u79fb\u52a8");
                }
                TargetEventHandler.onTargetReached((NpcAI2) this.owner.getAi2());
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
        if (shouldSkipStationaryRandomWalkStep(ownerX, ownerY, ownerZ, newX, newY, newZ,
                this.owner.getSpawn().getRandomWalk())) {
            return;
        }
        boolean returning = owner.getAi2().getState() == AIState.RETURNING;
        SpawnTemplate spawn = owner.getSpawn();
        boolean spawnDestination = spawn.getX() == targetDestX && spawn.getY() == targetDestY
                && spawn.getEffectiveZ() == targetDestZ;
        boolean walkingRoute = owner.getAi2().getSubState() == AISubState.WALK_PATH;
        // 行走者被挡段的每步碰撞/步高抬升解算（原版 fun_043 结构；独立开关，默认关时零差异）。
        // 只对被判「需要沿 Path」的段启用：平地段一次通行检测即返回，成本 O(1)。
        // Per-step collision/lift resolution for blocked walker legs (retail fun_043 shape; behind its own
        // switch). Only enabled on legs judged to need the Path: flat legs cost one passability probe.
        if (GeoDataConfig.GEO_NPC_WALK_COLLISION_ENABLE && walkingRoute && walkerLegNeedsPath
                && GeoDataConfig.GEO_ENABLE) {
            float[] resolvedStep = resolveWalkerCollisionStep(GameWorldServices.geoService().getGeoMap(owner.getWorldId()),
                    owner.getInstanceId(), ownerX, ownerY, ownerZ, newX, newY, newZ);
            if (resolvedStep != null) {
                newX = resolvedStep[0];
                newY = resolvedStep[1];
                newZ = resolvedStep[2];
            }
        }
        // 行走者沿 Path 推进时同样逐 tick 贴地：原版地面 NPC 每步做碰撞/地表解算，
        // 且贴地是短段重锚不产生反向拉扯的前提（AIM-011）。path != null 的排除只适用于追击/归家。
        // Walkers keep per-tick ground following even while following a Path: retail ground NPCs resolve
        // the surface on every step, and the ground-true stream re-anchoring depends on it (AIM-011).
        if (shouldApplyGeoHeightCorrection(AIConfig.ENHANCED_HOME_RETURN, returning, spawnDestination,
                path != null && !walkingRoute)
                && GeoDataConfig.GEO_NPC_MOVE && GeoDataConfig.GEO_ENABLE
                && !GameWorldServices.pathService().usesSpatialPath(owner)) {
            // 每 tick 采地表并半步插值：600ms 节流会在中间 5 个 tick 让 Z 漂回线性值，
            // 坡面不齐处表现为上下抖动。每 tick 平滑贴地消除锯齿。
            // 行走态（WALK_PATH）同样贴地：原版地面 NPC 每步做碰撞/地表解算；服务端位置贴地也是
            // 贴地短段下发的前提（否则短段重锚的起点会把客户端拉回弦线）。
            // Walkers ground-follow too: the retail server resolves collisions and the walkable
            // surface on every movement step for ground NPCs.
            float geoZ = GameWorldServices.geoService().getZ(owner.getWorldId(), newX, newY, newZ, 100, owner.getInstanceId());
            // 障碍物顶面等离散跳变：按移动速度限制单 tick Z 爬升幅度，避免瞬抬/瞬降
            float maxZStep = Math.max(0.5f, currentSpeed * elapsedMillis / 1000.0f * 1.5f);
            float zDelta = geoZ - newZ;
            if (Math.abs(zDelta) > maxZStep) {
                zDelta = Math.signum(zDelta) * maxZStep;
            }
            newZ += zDelta;
            // 仅显著落差（>1）才重置方向，避免每 tick STARTMOVE 造成客户端抖动
            if (Math.abs(ownerZ - newZ) > 1) {
                directionChanged = true;
            }
        }
        if (this.owner.getAi2().isLogging()) {
            AI2Logger.moveinfo(this.owner, "newX=" + newX + " newY=" + newY + " newZ=" + newZ + " mask=" + this.movementMask);
        }
        com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(this.owner, newX, newY, newZ, this.heading, false);
        sampleStuckShadow(owner.getX(), owner.getY(), owner.getZ(), targetX, targetY, targetZ, currentSpeed, now, path,
                (float) MathUtil.getDistance(owner.getX(), owner.getY(), owner.getZ(), targetX, targetY, targetZ), offset);
        boolean reachedWaypoint = reachedWaypoint(usesSpatialArrival(),
                targetX, targetY, targetZ, owner.getX(), owner.getY(), owner.getZ());
        if (reachedWaypoint && consumeWaypoint(path) && owner.getAi2().getState() == AIState.RETURNING
                && shouldCompleteHomeReturn(true, isHomeReturnDestinationReached())) {
            TargetEventHandler.onTargetReached((NpcAI2) owner.getAi2());
            return;
        }
        byte newMask = this.getMoveMask(directionChanged);
        boolean broadcastDestination = shouldBroadcastDestination(destination == Destination.TARGET_OBJECT,
                pathWaypointTransition, destinationChanged, now, lastMoveBroadcastAt);
        // 行走贴地短段（原版「持续推进」的近似复刻）：行走态每个移动 tick 补发一个前视点
        // （前视 = max(1m, 本 tick 实际位移 × 1.3)，终点 Z 取地表），让客户端始终持有「还没走到」的新目标；
        // 距航点不足收尾余量（0.75m）后停发，最后一段由客户端走向「上一包已给到的航点本身」。
        // 下限 1m 是客户端「接近目标即判定到达」的门槛：低于它会出现停-走-停（2026-10-06 实机 v3
        // 实测前视 0.33m 时多个巡逻「卡一下→原地走→小段前移」）；1.3× 只对极速行走者生效，防止其先到点（v1 停-跳）。
        // Walker ground-following stream: re-send a ground-true lookahead point every movement tick.
        float walkStepDistance = (float)MathUtil.getDistance(ownerX, ownerY, ownerZ, newX, newY, newZ);
        float streamLookahead = walkStreamLookahead(walkStepDistance);
        // 沿 Path 推进的行走者：剩余量按「剩余折线 → 本段航点」的弧长计、短段目标沿折线取点——
        // Path 节点间距只有 0.5m 级，若按「到最近节点」计剩余，0.75m 停发门槛会吞掉整段短段补发，
        // 客户端就会收到 <1m 的到点包（触发「接近目标即判定到达」→ 停-走-停，AIM-011 实机否决）。
        // Path-following walkers measure the remaining distance along the polyline to the leg waypoint and
        // take the stream target on the polyline: node spacing is sub-meter, so a nearest-node reading would
        // swallow the stream and expose sub-1m targets to the client's reach check (AIM-011).
        boolean walkerPathStream = walkingRoute && path != null;
        float walkRemaining = walkerPathStream
                ? walkPathRemaining(newX, newY, newZ, path, pointX, pointY, pointZ)
                : (float)MathUtil.getDistance(newX, newY, newZ, targetDestX, targetDestY, targetDestZ);
        float[] streamTarget = shouldStreamWalkGround(walkingRoute, reachedWaypoint, walkRemaining)
                        ? (walkerPathStream
                                ? walkPathStreamTarget(newX, newY, newZ, path, pointX, pointY, pointZ, streamLookahead)
                                : walkGroundStreamTarget(newX, newY, newZ, targetDestX, targetDestY, targetDestZ,
                                        streamLookahead))
                        : null;
        if (streamTarget != null) {
            streamTarget[2] = resolveGroundZ(streamTarget[0], streamTarget[1], streamTarget[2]);
        }
        if (shouldBroadcastMovement(this.movementMask, newMask, broadcastDestination || directionChanged
                || streamTarget != null)) {
            if (this.movementMask != newMask) {
                if (this.owner.getAi2().isLogging()) {
                    AI2Logger.moveinfo(this.owner, "oldMask=" + this.movementMask + " newMask=" + newMask);
                }
                this.movementMask = newMask;
            }
            lastMoveBroadcastAt = now;
            float fromX = ownerX;
            float fromY = ownerY;
            float fromZ = ownerZ;
            float toX = targetDestX;
            float toY = targetDestY;
            float toZ = targetDestZ;
            if (streamTarget != null) {
                // 短段包：起点用本 tick 贴地后的位置，终点用前视贴地点
                fromX = newX;
                fromY = newY;
                fromZ = newZ;
                toX = streamTarget[0];
                toY = streamTarget[1];
                toZ = streamTarget[2];
            }
            if (this.owner.getAi2().isLogging() && streamTarget != null) {
                AI2Logger.moveinfo(this.owner, "walkGroundStream from=" + fromX + "," + fromY + "," + fromZ
                        + " to=" + toX + "," + toY + "," + toZ);
            }
            // 记下客户端走完本包的估计时刻（原版移动包截止语义）：编队路点暂停的停包以此为锚——
            // 暂停瞬间客户端仍在走最后一段时不会被打断，等待超过该时刻仍不恢复才发停。
            // Record the estimated instant the client finishes this packet (retail's move deadline):
            // the formation-waypoint pause stop is anchored to it, so a pause while the client still
            // walks its last segment is never interrupted and a longer wait still ends with a stop.
            this.movePacketDeadlineMs = movePacketDeadline(now, fromX, fromY, fromZ, toX, toY, toZ, currentSpeed);
            PacketSendUtility.broadcastPacket(owner, new SM_MOVE(owner.getObjectId(), fromX, fromY, fromZ,
                    toX, toY, toZ, heading, movementMask));
        }
    }

    void sampleStuckShadow(float x, float y, float z, float waypointX, float waypointY, float waypointZ,
            float speed, long now, float[][] path, float remaining, float stopOffset) {
        boolean following = owner != null && owner.getAi2() != null
                && owner.getAi2().getState() == AIState.FOLLOWING;
        if ((!following && (path == null || path.length == 0)) || speed <= MOVE_OFFSET
                || (destination == Destination.TARGET_OBJECT && path != null && path.length == 1 && remaining <= stopOffset)) {
            resetStuckShadow();
            return;
        }
        int effectivePathLength = path == null ? 0 : path.length;
        if (progressSampleAt == 0 || (!following && (progressRequestId != pathRequestId || progressPathLength != effectivePathLength))) {
            resetStuckShadow();
            beginStuckSample(x, y, z, waypointX, waypointY, waypointZ, remaining, now, effectivePathLength);
            return;
        }
        long sampleMillis = now - progressSampleAt;
        if (sampleMillis < STUCK_SAMPLE_INTERVAL_MS) {
            return;
        }
        if (sampleMillis <= 0 || sampleMillis > STUCK_SAMPLE_MAX_DELAY_MS) {
            resetStuckShadow();
            beginStuckSample(x, y, z, waypointX, waypointY, waypointZ, remaining, now, effectivePathLength);
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
        beginStuckSample(x, y, z, waypointX, waypointY, waypointZ, remaining, now, effectivePathLength);
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
     * @param directionChanged 方向是否变化 / Whether direction changed
     * @return 移动掩码 / Movement mask
     */
    private byte getMoveMask(boolean directionChanged) {
        if (directionChanged) {
            return MovementMask.NPC_STARTMOVE;
        }
        if (this.owner.getAi2().getState() == AIState.RETURNING) {
            return owner.isInState(CreatureState.WALKING) ? MovementMask.NPC_WALK_FAST : MovementMask.NPC_RUN_FAST;
        }
        if (this.owner.getAi2().getState() == AIState.FOLLOWING) {
            return MovementMask.NPC_WALK_SLOW;
        }
        byte mask = MovementMask.IMMEDIATE;
        Stat2 stat = this.owner.getGameStats().getMovementSpeed();
        if (this.owner.isInState(CreatureState.WEAPON_EQUIPPED)) {
            mask = stat.getBonus() < 0 ? MovementMask.NPC_RUN_FAST : MovementMask.NPC_RUN_SLOW;
        } else if (this.owner.isInState(CreatureState.WALKING) || this.owner.isInState(CreatureState.ACTIVE)) {
            byte by = mask = stat.getBonus() < 0 ? MovementMask.NPC_WALK_FAST : MovementMask.NPC_WALK_SLOW;
        }
        if (this.owner.isFlying()) {
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
        this.setAndSendStopMove(this.owner);
    }

    /**
     * 重置移动状态、目标点与跟随马达。
     * Reset move state, destination points, and follow motor.
     */
    public void resetMove() {
        pauseStopGeneration++;
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
        walkHeadingDegrees = Float.NaN;
        walkHeadingTargetDegrees = Float.NaN;
        resetTargetTracking();
        resetStuckShadow();
        resetPath();
    }

    /**
     * 在编队路点暂停服务端推进：立即保留客户端移动掩码和已发送的路点目标（暂停瞬间客户端多半
     * 还在走最后 ~1m——最后流式目标=航点本身——立刻发停会把它冻在半途、恢复时再被拉一步），
     * 同时按**移动包截止**排定停包：原版运动控制器在「移动包截止已到且没有排队的下一段移动」时
     * 广播停包（NpcMotionController::_CommonUpdate）；这里等价地把停包排在「客户端走完最后一条
     * 移动包」的估计时刻 + {@link #WALKER_STOP_DEADLINE_MARGIN_MS}——在该时刻前恢复则停包作废
     * （短暂停旧行为逐字保留），等待超过该时刻才广播停，避免长等待期间客户端一直播放行走动画
     * （实机 2026-10-08：等 −8m 成员 ~5s「原地空踏步」）。从未下发过移动包（截止为 0）时无停可发——
     * 原版同款护栏（其运动截止为 0 时不发停）。
     * Pauses server movement at a formation waypoint: keeps the client mask and the already-sent
     * waypoint target immediately (at the pause instant the client is usually still walking the last
     * ~1 m; an immediate stop would freeze it mid-stride and the resume would pull it forward), and
     * arms the stop off the move-packet deadline, retail-style (NpcMotionController::_CommonUpdate
     * broadcasts the stop once the deadline passed with no queued next move): the stop is scheduled
     * for the estimated instant the client finishes its last move packet plus
     * {@link #WALKER_STOP_DEADLINE_MARGIN_MS} — a resume before that instant voids it (short pauses
     * keep the previous behaviour byte-for-byte), a longer wait gets an explicit stop so the client
     * does not animate a walk in place (field 2026-10-08: ~5 s waiting for the -8 m member). With no
     * move packet ever issued (deadline 0) there is nothing to stop — the same guard as retail (its
     * motion deadline 0 skips the stop).
     */
    public void pauseAtRoutePoint() {
        resetMove();
        long deadline = this.movePacketDeadlineMs;
        if (deadline == 0) {
            return;
        }
        long generation = ++pauseStopGeneration;
        GameThreadPoolServices.threadPoolManager().schedule(() -> sendPauseStopAtDeadline(generation),
                pauseStopDelayMs(deadline, System.currentTimeMillis()));
    }

    /**
     * 停包排定延迟：到移动包截止（客户端走完最后一段的估计时刻）加余量；已过期则为 0（立即）。
     * Delay before the scheduled pause stop: up to the move-packet deadline (estimated client walk
     * finish) plus the margin; an already-past deadline yields 0 (immediate).
     * @param packetDeadlineMs 移动包截止时间戳 / move-packet deadline timestamp
     * @param nowMs 当前时间戳 / current timestamp
     * @return 延迟毫秒（≥0）/ delay in ms (>= 0)
     */
    static long pauseStopDelayMs(long packetDeadlineMs, long nowMs) {
        return Math.max(0L, packetDeadlineMs + WALKER_STOP_DEADLINE_MARGIN_MS - nowMs);
    }

    /**
     * 由本条移动包（起点→终点、速度）估算客户端走完时刻；速度非正时视为即刻走完。
     * Estimates the instant the client finishes this move packet from its from/to positions and
     * speed; a non-positive speed counts as already finished.
     * @return 估算时间戳 / estimated timestamp
     */
    static long movePacketDeadline(long nowMs, float fromX, float fromY, float fromZ, float toX, float toY, float toZ,
            float speed) {
        if (speed <= 0f) {
            return nowMs;
        }
        float distance = (float)MathUtil.getDistance(fromX, fromY, fromZ, toX, toY, toZ);
        return nowMs + (long)(distance / speed * 1000.0f);
    }

    /**
     * 截止到期后的停包发送（见 {@link #pauseAtRoutePoint}）；代数不符（期间暂停/恢复/重置）或已不
     * 在等待时不发；发出后清零截止——该包对应的客户端行走已结束，重复暂停不重发。
     * Deadline stop broadcast (see {@link #pauseAtRoutePoint}); skipped when the generation changed
     * (pause/resume/reset happened meanwhile) or the member is no longer waiting. Clears the deadline
     * on send: the walk it referred to is over, so a repeated pause does not re-send.
     * @param generation 排定时的暂停代数 / pause generation captured when scheduled
     */
    private void sendPauseStopAtDeadline(long generation) {
        if (!shouldSendPauseStop(generation == pauseStopGeneration, started.get(),
                owner.getAi2().getSubState() == AISubState.WALK_WAIT_GROUP) || NpcActions.isAlreadyDead(owner)) {
            return;
        }
        this.movePacketDeadlineMs = 0;
        if (owner.getAi2().isLogging()) {
            AI2Logger.moveinfo(owner, "pauseStop at route point (move packet deadline passed)");
        }
        setAndSendStopMove(owner);
    }

    static boolean shouldSendPauseStop(boolean generationMatches, boolean started, boolean waitingGroup) {
        return generationMatches && !started && waitingGroup;
    }

    private void moveAlongPath() {
        if (!usesPath()) {
            moveToLocation(pointX, pointY, pointZ, offset);
            return;
        }
        boolean walkingRoute = owner != null && owner.getAi2().getSubState() == AISubState.WALK_PATH;
        long now = System.currentTimeMillis();
        if (!cachedPathValid && GameWorldServices.pathService().hasPathingData(owner)
                && now >= pathRetryAt) {
            requestLocationPath();
        }
        float[][] path = skipWaypoints(pathSnapshot(), now);
        if (path != null && path.length > 0) {
            float[] waypoint = path[0];
            moveToLocation(waypoint[0], waypoint[1], waypoint[2], pathMoveOffset(path), path);
        } else if (walkingRoute || !GameWorldServices.pathService().hasPathingData(owner)) {
            // 行走者无 Path / 已走完 / 请求失败：回退直线走完本段，绝不进入追击的停车阶梯
            // （stopForPath / finishFailedPointMove 会让巡逻停走）。到点由 isReachedPoint 判定、
            // 换段由 WalkManager.targetReached 收口；本段走完前 Path 若晚到，下一 tick 自然切回沿 Path。
            // Walkers fall back to the straight leg when the Path is absent/finished/failed and never enter
            // the chase stop ladder; arrival and leg switching stay with the existing AI/WalkManager flow.
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
        if (owner != null && owner.getAi2().getSubState() == AISubState.WALK_PATH) {
            // 行走者按原版语义逐段决定是否沿 Path（mode=off 时与旧行为逐分支等价）。
            // Walkers decide per leg whether to follow the Path (mode=off matches the old behaviour exactly).
            return GeoDataConfig.GEO_PATH_ENABLE
                    && shouldUseWalkerPath(GeoDataConfig.GEO_NPC_WALK_PATH_MODE, walkerLegNeedsPath);
        }
        return GeoDataConfig.GEO_PATH_ENABLE;
    }

    private boolean usesSpatialArrival() {
        return owner != null && (owner.isFlying()
                || GeoDataConfig.GEO_ENABLE && GameWorldServices.pathService().usesSpatialPath(owner));
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
        boolean following = owner.getAi2() != null && owner.getAi2().getState() == AIState.FOLLOWING;
        if (following && target != null && stuckShadowConfirmed) {
            catchupTeleportTo(target);
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
        // 行走者的 Path 是「本段折线」（起点=段起点、终点=本段航点），不是动态追击目标：不做
        // 每 tick 的「path 头可达」复检（那是有界 A*），也不进重规划停车分支；动态障碍由上面的
        // obstacleVersion 检查兜底。短段推进保持 10Hz 的 O(1) 预算，且绝不发停包（AIM-011）。
        // Walker paths are per-leg polylines with fixed endpoints, not moving chase targets: skip the
        // per-tick head-reachability replan check (a bounded A*) and never enter the stop branch;
        // obstacleVersion above still covers dynamic obstacles, keeping the 10 Hz step O(1).
        boolean walkingRoute = owner != null && owner.getAi2().getSubState() == AISubState.WALK_PATH;
        boolean blocked = !walkingRoute && path != null && path.length > 0
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
                clearPathFailureContext(!recovery);
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

    static boolean reachedWaypoint(boolean spatialPath, float targetX, float targetY, float targetZ,
            float actualX, float actualY, float actualZ) {
        if (spatialPath) {
            return MathUtil.getDistance(targetX, targetY, targetZ, actualX, actualY, actualZ) <= MOVE_OFFSET;
        }
        return MathUtil.getDistance(targetX, targetY, actualX, actualY) <= MOVE_OFFSET
                && Math.abs(targetZ - actualZ) <= GROUND_POINT_Z_TOLERANCE;
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
        return new Point3D(spawn.getX(), spawn.getY(), spawn.getEffectiveZ());
    }

    private float resolveRouteStepZ(RouteStep routeStep) {
        return resolveGroundZ(routeStep.getX(), routeStep.getY(), routeStep.getZ());
    }

    /**
     * 按坐标解析地表高度；geo 不可用时回退到给定 Z。
     * Resolves ground Z at the coordinates; falls back to the given Z when geo is unavailable.
     * @param x X 坐标 / X
     * @param y Y 坐标 / Y
     * @param fallbackZ 回退高度 / fallback height
     * @return 地表高度 / ground height
     */
    private float resolveGroundZ(float x, float y, float fallbackZ) {
        if (GeoDataConfig.GEO_ENABLE && GeoDataConfig.GEO_NPC_MOVE && !owner.isInFlyingState()) {
            return GameWorldServices.geoService().getZ(owner.getWorldId(), x, y, fallbackZ - 1, 100f, 1);
        }
        return fallbackZ;
    }

    public boolean isHomeReturnDestinationReached() {
        Point3D target = getHomeReturnDestination();
        if (homeReturnWaypoint != null && !usesSpatialArrival()) {
            return reachedWaypoint(false, target.getX(), target.getY(), target.getZ(), owner.getX(), owner.getY(),
                    owner.getZ());
        }
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
        clearPathFailureContext(true);
    }

    private void clearPathFailureContext(boolean clearStuckRecovery) {
        failedPathX = Float.NaN;
        failedPathY = Float.NaN;
        failedPathZ = Float.NaN;
        firstPathFailureAt = 0;
        failedPathObstacleVersion = 0;
        pathFailureHandled = false;
        lastPathAvoidanceAt = 0;
        pathAvoidanceAttempts = 0;
        if (clearStuckRecovery) {
            clearStuckRecoveryState();
        }
        resetStuckShadow();
        clearFollowTrail();
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

    /**
     * 判定归家是否已超时；超时则瞬移回出生点。
     * Evaluates the home-return timeout and teleports the NPC back to its spawn when exceeded.
     * <p>该判定必须发生在 {@link #moveToDestination()} 的所有提前返回之前：水中寻路持续失败时，
     * 早退分支会让 {@code case HOME} 永远不被执行，归家兜底也就永远不会触发。
     * This check must run before every early return of {@link #moveToDestination()}: while swimming,
     * failing pathing keeps the method from ever reaching {@code case HOME}, so the return-home
     * fallback could never fire.</p>
     * @return 本次移动帧是否因归家超时而中止 / whether this move frame was aborted by the home-return timeout
     */
    private boolean abortTimedOutHomeReturn() {
        if (destination != Destination.HOME || homeReturnStartedAt <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (!shouldTeleportFailedHomeReturn(isSpReturn(owner), homeReturnStartedAt, now)
                && !hasHomeReturnTimedOut(homeReturnStartedAt, HOME_RETURN_TIMEOUT_MS, now)) {
            return false;
        }
        if (owner.getAi2().isLogging()) {
            AI2Logger.moveinfo(owner, "MC: home return timed out, teleporting to spawn");
        }
        finishFailedHomeReturn();
        return true;
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
     * @param currentRoute 路线步骤列表 / Route step list
     */
    public void setCurrentRoute(List<RouteStep> currentRoute) {
        if (currentRoute == null) {
            AI2Logger.info(owner.getAi2(), String.format("MC: setCurrentRoute is setting route to null (NPC id: %d)!!!", owner.getNpcId()));
        } else {
            this.currentRoute = currentRoute;
        }
        this.currentPoint = 0;
    }

    /**
     * 设置当前与上一路线步骤，处理编队偏移与地形高度。
     * Set current/previous route steps; handle formation offset and terrain Z.
     * @param paramRouteStep1 当前步骤 / Current step
     * @param paramRouteStep2 上一步骤 / Previous step
     */
    public void setRouteStep(RouteStep paramRouteStep1, RouteStep paramRouteStep2) {
        Point2D localPoint2D = null;
        if (this.owner.getWalkerGroup() != null) {
            if (this.owner.getWalkerGroupShift() == null) {
                log.warn(I18n.get("log.351405aaadba", this.owner.getNpcId()));
                return;
            }
            localPoint2D = WalkerGroup.getLinePoint(new Point2D(paramRouteStep2.getX(), paramRouteStep2.getY()), new Point2D(paramRouteStep1.getX(), paramRouteStep1.getY()), this.owner.getWalkerGroupShift());
            this.owner.getWalkerGroup().setStep(this.owner, paramRouteStep1.getRouteStep());
        }
        this.currentPoint = paramRouteStep1.getRouteStep() - 1;
        this.pointX = localPoint2D == null ? paramRouteStep1.getX() : localPoint2D.getX();
        this.pointY = localPoint2D == null ? paramRouteStep1.getY() : localPoint2D.getY();
        // 编队成员的航点被编队偏移后移（本路线最多 8m）：Z 必须按航点自身坐标采样。取上一步坐标时，
        // 斜坡/台阶段上误差随偏移放大（实机 LF1A_NPCPath_Ermona 离线复刻：-2/-3/-6/-8m 偏移 →
        // 0.26/0.27/0.84/1.17m；2026-10-06 799731/799736/799737/799746「上台阶悬空、下台阶入地」；
        // 队长偏移 0 不受影响）。回退值与旧实现一致（上一步模板 Z）。
        // A formation waypoint sits up to 8 m behind the previous route point: sample the ground at
        // the waypoint itself. Sampling at the previous point misses by up to ~1 m on steps/slopes.
        this.pointZ = localPoint2D == null ? resolveRouteStepZ(paramRouteStep1)
            : resolveGroundZ(this.pointX, this.pointY, paramRouteStep2.getZ());
        this.destination = Destination.POINT;
        this.walkPause = paramRouteStep1.getRestTime();
        refreshWalkerLegPath();
    }

    /**
     * 换段时刷新「本段是否需要沿 Path」并按需预取本段 Path：
     * blocked 模式对「当前位置 → 本段航点」做一次廉价 PATH-LoS（网格 Bresenham），不通才预取；
     * always 模式无条件预取；off 模式、带非零编队偏移的跟随者、飞行/游泳不预取（保持直线，与旧行为一致）；
     * 编队队长（shift 0,0）站位与航点重合，按独行语义参与预取。
     * 预取走异步队列（单 NPC 单在途 + 背压），绝不阻塞移动；Path 到达前本段沿直线先走，
     * 到达后 moveAlongPath 下一 tick 自然切到沿 Path 推进。
     * Refreshes whether the current walk leg follows the Path and pre-fetches it when needed: blocked mode
     * runs one cheap PATH line-of-sight check (grid Bresenham) and fetches only when it fails; always mode
     * always fetches; off mode, offset formation followers and flying/swimming walkers stay straight. The
     * formation leader (shift 0,0) stands on the route point itself and is fetched like a solo walker.
     * The fetch is async (single in-flight request per NPC, queue back-pressure) and never blocks movement;
     * until it lands the leg walks straight and moveAlongPath switches over on the next tick.
     */
    /**
     * 本段的沿 Path 预取（在途请求或已缓存路径）是否仍指向当前航点——moveToNextPoint 保留预取的判据。
     * Whether this leg's Path pre-fetch (in-flight request or cached path) still targets the current
     * waypoint; used by moveToNextPoint to keep it instead of resetting.
     * @return 预取属于当前航点返回 true / true when the pre-fetch belongs to the current leg
     */
    private boolean walkerPathTargetsCurrentLeg() {
        if (!walkerLegNeedsPath) {
            return false;
        }
        if (pendingPath != null) {
            return sameWalkerLegTarget(pointX, pointY, pointZ, pendingPathX, pendingPathY, pendingPathZ);
        }
        return cachedPathValid && sameWalkerLegTarget(pointX, pointY, pointZ,
                walkerLegPathKeyX, walkerLegPathKeyY, walkerLegPathKeyZ);
    }

    private void refreshWalkerLegPath() {
        WalkerGroupShift shift = owner.getWalkerGroupShift();
        boolean follower = isFormationFollower(owner.getWalkerGroup() != null, shift != null,
                shift == null ? 0f : shift.getSagittalShift(), shift == null ? 0f : shift.getCoronalShift());
        if (!walkerPathEligible(GeoDataConfig.GEO_PATH_ENABLE, follower, owner.isFlying(),
                GameWorldServices.pathService().usesSpatialPath(owner))) {
            this.walkerLegNeedsPath = false;
            return;
        }
        String mode = GeoDataConfig.GEO_NPC_WALK_PATH_MODE;
        boolean always = "always".equalsIgnoreCase(mode);
        if (!always && !"blocked".equalsIgnoreCase(mode)) {
            this.walkerLegNeedsPath = false;
            return;
        }
        // 同一航点的重复刷新（行走 AI 重入 startWalking/恢复走路）复用已有判定与在途请求：
        // 不再 resetPath 丢弃在途 A* 再重发。
        // A duplicate refresh for the same waypoint (the walking AI re-enters startWalking / resumes the
        // walk) reuses the existing verdict and in-flight request instead of resetting and re-issuing.
        if (this.walkerLegNeedsPath && sameWalkerLegTarget(pointX, pointY, pointZ,
                walkerLegPathKeyX, walkerLegPathKeyY, walkerLegPathKeyZ)) {
            return;
        }
        this.walkerLegNeedsPath = false;
        // 换段统一收口：清掉上一段的路径缓存与在途请求（追击/归家不走本入口，不受影响）。
        // Single place to drop the previous leg's path cache and in-flight request (chase/home never
        // enter through setRouteStep).
        resetPath();
        if (!always && GameWorldServices.pathService().canWalkStraightLine(owner.getWorldId(),
                owner.getX(), owner.getY(), owner.getZ(), pointX, pointY, pointZ)) {
            return;
        }
        this.walkerLegNeedsPath = true;
        this.walkerLegPathKeyX = pointX;
        this.walkerLegPathKeyY = pointY;
        this.walkerLegPathKeyZ = pointZ;
        requestLocationPath();
    }

	/**
     * 是否已到达当前目标点。
     * Whether the current target point has been reached.
     * @return 是否已到达 / Whether reached
     */
    public boolean isReachedPoint() {
        return reachedWaypoint(usesSpatialArrival(), pointX, pointY, pointZ, owner.getX(), owner.getY(), owner.getZ());
    }

    /**
     * 选择下一巡逻步骤；路线为空时尝试重建。
     * Choose the next walk step; rebuild the route when empty.
     */
    public void chooseNextStep() {
        int oldPoint = this.currentPoint;
        if (this.currentRoute == null) {
            WalkerTemplate template;
            WalkManager.stopWalking((NpcAI2) this.owner.getAi2());
            if (!WalkerFormator.processClusteredNpc(this.owner, this.owner.getWorldId(), this.owner.getInstanceId()) && (template = DataManager.WALKER_DATA.getWalkerTemplate(this.owner.getSpawn().getWalkerId())) != null) {
                this.currentRoute = template.getRouteSteps();
            }
            if (this.currentRoute == null) {
                log.warn(I18n.get("log.6b808206626a", this.owner.getNpcId(), oldPoint));
                return;
            }
        }
        this.currentPoint = this.currentPoint < this.currentRoute.size() - 1 ? ++this.currentPoint : 0;
        this.setRouteStep(this.currentRoute.get(this.currentPoint), this.currentRoute.get(oldPoint));
    }

    /**
     * 是否正在转向（位于路线起点）。
     * Whether direction is changing (at route start).
     * @return 是否正在转向 / Whether changing direction
     */
    public boolean isChangingDirection() {
        return this.currentPoint == 0;
    }

    /**
     * 返回目标 X；未启动时返回当前位置。
     * Return target X; current position when not started.
     * @return 目标 X / Target X
     */
    @Override
    public final float getTargetX2() {
        return this.started.get() ? this.targetDestX : this.owner.getX();
    }

    /**
     * 返回目标 Y；未启动时返回当前位置。
     * Return target Y; current position when not started.
     * @return 目标 Y / Target Y
     */
    @Override
    public final float getTargetY2() {
        return this.started.get() ? this.targetDestY : this.owner.getY();
    }

    /**
     * 返回目标 Z；未启动时返回当前位置。
     * Return target Z; current position when not started.
     * @return 目标 Z / Target Z
     */
    @Override
    public final float getTargetZ2() {
        return this.started.get() ? this.targetDestZ : this.owner.getZ();
    }

    /**
     * 是否正在跟随目标对象。
     * Whether currently following a target object.
     * @return 是否正在跟随 / Whether following
     */
    public boolean isFollowingTarget() {
        return this.destination == Destination.TARGET_OBJECT;
    }

    /**
     * 记录当前位置为回退步（返回中不记录）。
     * Store the current position as a back-step (skipped while returning).
     */
    public void storeStep() {
        if (this.owner.getAi2().getState() == AIState.RETURNING) {
            return;
        }
        if (this.lastSteps == null) {
            this.lastSteps = new LastUsedCache(10);
        }
        Point3D currentStep = new Point3D(this.owner.getX(), this.owner.getY(), this.owner.getZ());
        if (this.owner.getAi2().isLogging()) {
            AI2Logger.moveinfo(this.owner, "store back step: X=" + this.owner.getX() + " Y=" + this.owner.getY() + " Z=" + this.owner.getZ());
        }
        if (this.stepSequenceNr == 0 || MathUtil.getDistance(this.lastSteps.get(this.stepSequenceNr), currentStep) >= 5.0) {
            this.stepSequenceNr = (byte)(this.stepSequenceNr + 1);
            this.lastSteps.put(this.stepSequenceNr, currentStep);
        }
    }

    /**
     * 取回上一个回退步并设为目标；无记录时回退到出生点。
     * Recall the previous back-step as destination; fall back to spawn when none.
     * @return 回退点 / Recalled point
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
            if (this.owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(this.owner, "recall back step: spawn point");
            }
            this.targetDestX = this.owner.getSpawn().getX();
            this.targetDestY = this.owner.getSpawn().getY();
            this.targetDestZ = this.owner.getSpawn().getEffectiveZ();
            result = new Point3D(this.targetDestX, this.targetDestY, this.targetDestZ);
        } else {
            if (this.owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(this.owner, "recall back step: X=" + result.getX() + " Y=" + result.getY() + " Z=" + result.getZ());
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
