/*
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.controllers.movement.MinionMoveController;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;

/**
 * @author ATracer, Improved by Neon
 */
public class MinionController extends VisibleObjectController<Minion> {

    private static final int FOLLOW_RANGE = 5;
    private static final int TELEPORT_RANGE = 25;
    private static final int MOVE_UPDATE_RATE = 1000;
    private static final int TELEPORT_CHECK_RATE = 2000;
    private static final byte MOVE_MASK = (byte) 0x40;

    @Override
    public void see(VisibleObject object) {

    }

    @Override
    public void notSee(VisibleObject object, boolean isOutOfRange) {

    }

    public void startFollowing(Player player) {
        Minion minion = getOwner();
        if (minion == null || player == null) {
            return;
        }

        player.getController().cancelTask(TaskId.MINION_UPDATE);
        player.getController().cancelTask(TaskId.MINION_TELEPORT_CHECK);

        player.getController().addTask(TaskId.MINION_UPDATE, ThreadPoolManager.getInstance().scheduleAtFixedRate(new MinionFollowTask(player), 1000, MOVE_UPDATE_RATE));

        player.getController().addTask(TaskId.MINION_TELEPORT_CHECK, ThreadPoolManager.getInstance().scheduleAtFixedRate(new MinionTeleportTask(player), 2000, TELEPORT_CHECK_RATE));
    }

    public void stopFollowing(Player player) {
        if (player != null) {
            player.getController().cancelTask(TaskId.MINION_UPDATE);
            player.getController().cancelTask(TaskId.MINION_TELEPORT_CHECK);
        }
    }

    public void teleportToPlayer(Player player) {
        Minion minion = getOwner();
        if (minion == null || player == null || !minion.isSpawned()) {
            return;
        }

        float oldX = minion.getX();
        float oldY = minion.getY();
        float oldZ = minion.getZ();

        World.getInstance().updatePosition(minion, player.getX(), player.getY(), player.getZ(), player.getHeading());
        
        PacketSendUtility.broadcastPacketAndReceive(minion, new SM_MOVE(minion.getObjectId(), oldX, oldY, oldZ, player.getX(), player.getY(), player.getZ(), player.getHeading(), (byte) 0));
    }

    public class MinionFollowTask implements Runnable {

        private final Player player;

        public MinionFollowTask(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            try {
                Minion minion = getOwner();
                if (minion == null || player == null || player.getMinion() == null) {
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
                System.err.println("MinionFollowTask error: " + e.getMessage());
            }
        }
    }

    public class MinionTeleportTask implements Runnable {

        private final Player player;

        public MinionTeleportTask(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            try {
                Minion minion = getOwner();
                if (minion == null || player == null || player.getMinion() == null) {
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
                System.err.println("MinionTeleportTask error: " + e.getMessage());
            }
        }
    }
}