package com.aionemu.gameserver.controllers.movement;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.handler.TargetEventHandler;
import com.aionemu.gameserver.ai2.manager.WalkManager;
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
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * NPC 移动控制器：目标追踪、寻路缓存、巡逻路径、回家与跟随马达。
 * NPC move controller: target tracking, nav cache, walk routes, return-home, and follow motor.
 */
@Slf4j
public class NpcMoveController
        extends CreatureMoveController<Npc> {
    /** 移动检测偏移阈值 / Move check offset threshold */
    public static final float MOVE_CHECK_OFFSET = 0.1f;
    /** 内部移动偏移 / Internal move offset */
    private static final float MOVE_OFFSET = 0.05f;
    /** 回家寻路重试次数 / Return-home nav retry count */
    private int returnAttempts;
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
    /** Cached target Z / Cached target Z */
    private float cachedTargetZ;
    /** 缓存路径是否有效 / Whether cached path is valid */
    private boolean cachedPathValid;
    /** 缓存导航路径 / Cached navigation path */
    private float[][] cachedPath;
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

    /**
     * 开始向当前目标对象移动。
     * Start moving toward the current target object.
     */
    public void moveToTargetObject() {
        if (started.compareAndSet(false, true)) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "MC: moveToTarget started");
            }
            destination = Destination.TARGET_OBJECT;
            updateLastMove();
            GameMovementLoopServices.moveTaskManager().addCreature(owner);
        }
    }

    /**
     * 开始向指定坐标点移动。
     * Start moving toward a specific point.
     *
     * @param x 目标 X / Target X
     * @param y 目标 Y / Target Y
     * @param z 目标 Z / Target Z
     */
    public void moveToPoint(float x, float y, float z) {
        if (started.compareAndSet(false, true)) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "MC: moveToPoint started");
            }
            destination = Destination.POINT;
            pointX = x;
            pointY = y;
            pointZ = z;
            updateLastMove();
            GameMovementLoopServices.moveTaskManager().addCreature(owner);
        }
    }

    /**
     * 开始返回出生点。
     * Start returning to the spawn/home point.
     */
    public void moveToHome() {
        if (started.compareAndSet(false, true)) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "MC: moveToHome started");
            }
            cachedPathValid = false;
            float x = owner.getSpawn().getX(), y = owner.getSpawn().getY(), z = owner.getSpawn().getZ();
            destination = Destination.HOME;
            pointX = x;
            pointY = y;
            pointZ = z;
            updateLastMove();
            GameMovementLoopServices.moveTaskManager().addCreature(owner);
        }
    }

    /**
     * 开始向下一个巡逻点移动。
     * Start moving toward the next walk-route point.
     */
    public void moveToNextPoint() {
        if (started.compareAndSet(false, true)) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "MC: moveToNextPoint started");
            }
            destination = Destination.POINT;
            updateLastMove();
            GameMovementLoopServices.moveTaskManager().addCreature(owner);
        }
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
            movementMask = MovementMask.NPC_STARTMOVE;
            PacketSendUtility.broadcastPacket(owner, new SM_MOVE(owner));
        }

        if (!started.get()) {
            if (owner.getAi2().isLogging()) {
                AI2Logger.moveinfo(owner, "moveToDestination not started");
            }
        }
        switch (destination) {
            case TARGET_OBJECT:
                VisibleObject target = owner.getTarget();
                if (!(target instanceof Creature creature)) {
                    cancelFollow();
                    return;
                }
                if (GeoDataConfig.GEO_NAV_ENABLE) {
                    returnAttempts = 0;
                    if ((MathUtil.getDistance(target.getX(), target.getY(), pointZ, pointX, pointY, pointZ) > MOVE_CHECK_OFFSET)) {
                        offset = owner.getController().getAttackDistanceToTarget();
                        pointX = target.getX();
                        pointY = target.getY();
                        pointZ = getTargetZ(owner, creature);
                        if (!hasIntermediateWaypoint(cachedPath)) {
                            cachedPathValid = false;
                        }
                    }
                    if (!cachedPathValid || cachedPath == null) {
                        cachedPath = GameWorldServices.navService().navigateToTarget(owner, creature);
                        cachedPathValid = true;
                    }
                    if (cachedPath != null && cachedPath.length > 0) {
                        float[] p1 = cachedPath[0];
                        assert p1.length == 3;
                        moveToLocation(p1[0], p1[1], getTargetZ(owner, p1[0], p1[1], p1[2]), offset);
                    } else {
                        if (cachedPath != null) cachedPath = null;
                        moveToLocation(pointX, pointY, pointZ, offset);
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
                moveToLocation(pointX, pointY, pointZ, offset);
                break;
            }
            case HOME: {
                if ((!cachedPathValid || cachedPath == null) && (returnAttempts<3)) {
                    cachedPath = GameWorldServices.navService().navigateToLocation(owner, pointX, pointY, pointZ);
                    returnAttempts++;
                    cachedPathValid = true;
                }
                if ((cachedPath != null) && (cachedPath.length > 0) && (returnAttempts<3)) {
                    float[] p1 = cachedPath[0];
                    moveToLocation(p1[0], p1[1], getTargetZ(owner, p1[0], p1[1], p1[2]), offset);
                } else{
                    moveToLocation(pointX, pointY, pointZ, offset);
                }
            }
        }
        this.updateLastMove();
    }

    /**
     * 解析目标生物的有效 Z（飞行目标对地时取地形高度）。
     * Resolve the effective Z of a target creature (terrain Z when target flies and NPC does not).
     *
     * @param npc NPC
     * Target creature
     * Effective Z
     */
    private float getTargetZ(Npc npc, Creature creature) {
        float targetZ = creature.getZ();
        if (GeoDataConfig.GEO_NPC_MOVE && creature.isInFlyingState() && !npc.isInFlyingState()) {
//            if (npc.getGameStats().checkGeoNeedUpdate()) {
            this.cachedTargetZ = GameWorldServices.geoService().getZ(creature);
 //           }
            targetZ = this.cachedTargetZ;
        }
        return targetZ;
    }

    /**
     * 解析坐标点的有效 Z（非飞行 NPC 时取地形高度）。
     * Resolve the effective Z of a point (terrain Z when NPC is not flying).
     *
     * @param npc NPC
     * @param x X
     * @param y Y
     * @param z 原始 Z / Original Z
     * Effective Z
     */
    private float getTargetZ(Npc npc, float x, float y, float z) {
        float targetZ = z;
        if (GeoDataConfig.GEO_NPC_MOVE && !npc.isFlying()) {
//            if (npc.getGameStats().checkGeoNeedUpdate()) {
            cachedTargetZ = GameWorldServices.geoService().getZ(npc.getWorldId(), x, y, z, 1.1F, npc.getInstanceId());
            targetZ = cachedTargetZ;
//            }
        }
        return targetZ;
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
        boolean directionChanged = false;
        float ownerX = ((Npc)this.owner).getX();
        float ownerY = ((Npc)this.owner).getY();
        float ownerZ = ((Npc)this.owner).getZ();
        boolean bl = directionChanged = targetX != this.targetDestX || targetY != this.targetDestY || targetZ != this.targetDestZ;
        if (directionChanged) {
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
        float currentSpeed = ((Npc)this.owner).getGameStats().getMovementSpeedFloat();
        float futureDistPassed = currentSpeed * (float)(System.currentTimeMillis() - this.lastMoveUpdate) / 1000.0f;
        float dist = (float)MathUtil.getDistance(ownerX, ownerY, ownerZ, targetX, targetY, targetZ);
        if (((Npc)this.owner).getAi2().isLogging()) {
            AI2Logger.moveinfo((Creature)this.owner, "futureDist: " + futureDistPassed + " dist: " + dist);
        }
        if (dist == 0.0f) {
            if (((Npc)this.owner).getAi2().getState() == AIState.RETURNING) {
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
        if (futureDistPassed == dist
                && (destination == Destination.TARGET_OBJECT || destination == Destination.HOME)) {
            if (cachedPath != null && cachedPath.length > 0) {
                float[][] tempCache = new float[cachedPath.length - 1][];
                if (tempCache.length > 0) {
                    System.arraycopy(cachedPath, 1, tempCache, 0, cachedPath.length - 1);
                    cachedPath = tempCache;
                } else {
                    cachedPath = null;
                    cachedPathValid = false;
                }
            }
        }
        float distFraction = futureDistPassed / dist;
        float newX = (this.targetDestX - ownerX) * distFraction + ownerX;
        float newY = (this.targetDestY - ownerY) * distFraction + ownerY;
        float newZ = (this.targetDestZ - ownerZ) * distFraction + ownerZ;
        if (ownerX == newX && ownerY == newY && ((Npc)this.owner).getSpawn().getRandomWalk() > 0) {
            return;
        }
        if (GeoDataConfig.GEO_NPC_MOVE && GeoDataConfig.GEO_ENABLE && owner.getAi2().getSubState() != AISubState.WALK_PATH && owner.getAi2().getState() != AIState.RETURNING
                && owner.getGameStats().checkGeoNeedUpdate()) {
            if (owner.getSpawn().getX() != targetDestX || owner.getSpawn().getY() != targetDestY || owner.getSpawn().getZ() != targetDestZ) {
                float geoZ = GameWorldServices.geoService().getZ(owner.getWorldId(), newX, newY, newZ, 0, owner.getInstanceId());
                if (Math.abs(newZ - geoZ) > 1) {
                    directionChanged = true;
                }
                newZ = geoZ ;
            }
        }
        if (((Npc)this.owner).getAi2().isLogging()) {
            AI2Logger.moveinfo((Creature)this.owner, "newX=" + newX + " newY=" + newY + " newZ=" + newZ + " mask=" + this.movementMask);
        }
        com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(this.owner, newX, newY, newZ, this.heading, false);
        byte newMask = this.getMoveMask(directionChanged);
        if (shouldBroadcastMovement(this.movementMask, newMask, directionChanged)) {
            if (this.movementMask != newMask) {
                if (((Npc)this.owner).getAi2().isLogging()) {
                    AI2Logger.moveinfo((Creature)this.owner, "oldMask=" + this.movementMask + " newMask=" + newMask);
                }
                this.movementMask = newMask;
            }
            PacketSendUtility.broadcastPacket(this.owner, new SM_MOVE((Creature)this.owner));
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
            return MovementMask.NPC_RUN_FAST;
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
        cancelFollow();
        started.set(false);
        targetDestX = 0;
        targetDestY = 0;
        targetDestZ = 0;
        pointX = 0;
        pointY = 0;
        pointZ = 0;
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
            this.pointZ = paramRouteStep2.getZ();
            if (GeoDataConfig.GEO_ENABLE && GeoDataConfig.GEO_NPC_MOVE && !(this.owner.isInFlyingState())) {
                this.pointZ = GameWorldServices.geoService().getZ(((Creature)this.owner).getWorldId(), paramRouteStep2.getX(), paramRouteStep2.getY(), paramRouteStep2.getZ()-1, 100f, 1);
            }
            ((Npc)this.owner).getWalkerGroup().setStep((Npc)this.owner, paramRouteStep1.getRouteStep());
        } else {
            this.pointZ = paramRouteStep1.getZ();
            if (GeoDataConfig.GEO_ENABLE && GeoDataConfig.GEO_NPC_MOVE && !(this.owner.isInFlyingState())) {
                this.pointZ = GameWorldServices.geoService().getZ(((Creature)this.owner).getWorldId(), paramRouteStep1.getX(), paramRouteStep1.getY(), paramRouteStep1.getZ()-1, 100f, 1);
            }
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
