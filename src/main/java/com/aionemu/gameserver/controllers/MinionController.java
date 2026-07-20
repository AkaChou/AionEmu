package com.aionemu.gameserver.controllers;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.controllers.movement.MinionMoveController;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 小跟班（Minion）控制器，管理跟随与距离过远时的瞬移。
 * Minion controller managing follow behavior and teleport-when-too-far.
 *
 * @author ATracer, Improved by Neon
 */
@Slf4j
public class MinionController extends VisibleObjectController<Minion> {

    /** 开始跟随的距离阈值。 / Distance threshold to start following. */
    private static final int FOLLOW_RANGE = 5;
    /** 超出该距离则瞬移到主人身边。 / Distance beyond which the minion teleports to the master. */
    private static final int TELEPORT_RANGE = 25;
    /** 跟随移动任务间隔（毫秒）。 / Follow-move task interval in milliseconds. */
    private static final int MOVE_UPDATE_RATE = 1000;
    /** 瞬移检测任务间隔（毫秒）。 / Teleport-check task interval in milliseconds. */
    private static final int TELEPORT_CHECK_RATE = 2000;
    /** 移动状态掩码。 / Move state mask. */
    private static final byte MOVE_MASK = (byte) 0x40;

    /**
     * 小跟班看到其他对象时的回调（当前无逻辑）。
     * Callback when the minion sees another object (currently no-op).
     *
     * @param object 进入视野的对象 / the object entering sight
     */
    @Override
    public void see(VisibleObject object) {

    }

    /**
     * 小跟班不再看到其他对象时的回调（当前无逻辑）。
     * Callback when the minion no longer sees another object (currently no-op).
     *
     * @param object 离开视野的对象 / the object leaving sight
     * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
     */
    @Override
    public void notSee(VisibleObject object, boolean isOutOfRange) {

    }

    /**
     * 开始跟随指定玩家，启动移动与瞬移检测任务。
     * Starts following the given player by scheduling move and teleport-check tasks.
     *
     * master player
     */
    public void startFollowing(Player player) {
        Minion minion = getOwner();
        if (minion == null || player == null) {
            return;
        }

        player.getController().cancelTask(TaskId.MINION_UPDATE);
        player.getController().cancelTask(TaskId.MINION_TELEPORT_CHECK);

        player.getController().addTask(TaskId.MINION_UPDATE, GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new MinionFollowTask(player), 1000, MOVE_UPDATE_RATE));

        player.getController().addTask(TaskId.MINION_TELEPORT_CHECK, GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new MinionTeleportTask(player), 2000, TELEPORT_CHECK_RATE));
    }

    /**
     * 停止跟随并取消相关任务。
     * Stops following and cancels related tasks.
     *
     * master player
     */
    public void stopFollowing(Player player) {
        if (player != null) {
            player.getController().cancelTask(TaskId.MINION_UPDATE);
            player.getController().cancelTask(TaskId.MINION_TELEPORT_CHECK);
        }
    }

    /**
     * 将小跟班瞬移到玩家当前位置。
     * Teleports the minion to the player's current position.
     *
     * master player
     */
    public void teleportToPlayer(Player player) {
        Minion minion = getOwner();
        if (minion == null || player == null || minion.getMaster() != player || player.getMinion() != minion
                || !minion.isSpawned()) {
            return;
        }

        float oldX = minion.getX();
        float oldY = minion.getY();
        float oldZ = minion.getZ();

        com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(minion, player.getX(), player.getY(), player.getZ(), player.getHeading());

        PacketSendUtility.broadcastPacketAndReceive(minion, new SM_MOVE(minion.getObjectId(), oldX, oldY, oldZ, player.getX(), player.getY(), player.getZ(), player.getHeading(), (byte) 0));
    }

    /**
     * 周期性跟随任务：按距离移动或瞬移。
     * Periodic follow task that moves or teleports based on distance.
     */
    public class MinionFollowTask implements Runnable {

        /** 主人玩家。 / Master player. */
        private final Player player;

        /**
         * 构造跟随任务。
         * Constructs a follow task.
         *
         * master player
         */
        public MinionFollowTask(Player player) {
            this.player = player;
        }

        /**
         * 执行一次跟随逻辑。
         * Runs one follow tick.
         */
        @Override
        public void run() {
            try {
                Minion minion = getOwner();
                if (minion == null || player == null || player.getMinion() != minion) {
                    return;
                }

                if (minion.getMaster() != player) {
                    return;
                }

                if (!minion.isSpawned()) {
                    return;
                }

                double distance = MathUtil.getDistance(minion, player);

                if (distance > TELEPORT_RANGE) {
                    teleportToPlayer(player);
                    return;
                }

                MinionMoveController moveController = (MinionMoveController) minion.getMoveController();

                if (distance > FOLLOW_RANGE) {
                    moveController.setNewDirection(player.getX(), player.getY(), player.getZ(), player.getHeading());

                    PacketSendUtility.broadcastPacket(minion, new SM_MOVE(minion.getObjectId(), minion.getX(), minion.getY(), minion.getZ(), player.getX(), player.getY(), player.getZ(), minion.getHeading(), MOVE_MASK));
                } else {
                    moveController.abortMove();
                    PacketSendUtility.broadcastPacket(minion, new SM_MOVE(minion.getObjectId(), minion.getX(), minion.getY(), minion.getZ(), minion.getX(), minion.getY(), minion.getZ(), minion.getHeading(), (byte) 0));
                }

            } catch (Exception e) {
                log.error(I18n.get("log.77d741ca3881", player == null ? 0 : player.getObjectId(),
                        getOwner() == null ? 0 : getOwner().getObjectId(), e));
            }
        }
    }

    /**
     * 周期性瞬移检测任务：距离过大时拉回或加速靠近。
     * Periodic teleport-check task that pulls the minion back or speeds approach when far.
     */
    public class MinionTeleportTask implements Runnable {

        /** 主人玩家。 / Master player. */
        private final Player player;

        /**
         * 构造瞬移检测任务。
         * Constructs a teleport-check task.
         *
         * master player
         */
        public MinionTeleportTask(Player player) {
            this.player = player;
        }

        /**
         * 执行一次瞬移检测。
         * Runs one teleport-check tick.
         */
        @Override
        public void run() {
            try {
                Minion minion = getOwner();
                if (minion == null || player == null || player.getMinion() != minion) {
                    return;
                }

                if (minion.getMaster() != player) {
                    return;
                }

                if (!minion.isSpawned()) {
                    return;
                }

                double distance = MathUtil.getDistance(minion, player);

                if (distance > TELEPORT_RANGE) {
                    teleportToPlayer(player);
                    return;
                }

                if (distance > FOLLOW_RANGE * 2) {
                    MinionMoveController moveController = (MinionMoveController) minion.getMoveController();

                    moveController.setNewDirection(player.getX(), player.getY(), player.getZ(), player.getHeading());

                    PacketSendUtility.broadcastPacket(minion, new SM_MOVE(minion.getObjectId(), minion.getX(), minion.getY(), minion.getZ(), player.getX(), player.getY(), player.getZ(), minion.getHeading(), MOVE_MASK));
                }

            } catch (Exception e) {
                log.error(I18n.get("log.d83820e1fe0d", player == null ? 0 : player.getObjectId(), getOwner() == null ? 0 : getOwner().getObjectId(), e));
            }
        }
    }
}
