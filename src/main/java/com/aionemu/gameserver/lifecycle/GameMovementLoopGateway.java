package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameMovementLoopGateway {

    private ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider;
    private ObjectProvider<MoveTaskManager> moveTaskManagerProvider;
    private ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider;
    private ObjectProvider<ZoneUpdateService> zoneUpdateServiceProvider;

    @Autowired(required = false)
    void setMovementNotifyTaskProvider(ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider) {
        this.movementNotifyTaskProvider = movementNotifyTaskProvider;
    }

    @Autowired(required = false)
    void setMoveTaskManagerProvider(ObjectProvider<MoveTaskManager> moveTaskManagerProvider) {
        this.moveTaskManagerProvider = moveTaskManagerProvider;
    }

    @Autowired(required = false)
    void setPlayerMoveTaskManagerProvider(ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider) {
        this.playerMoveTaskManagerProvider = playerMoveTaskManagerProvider;
    }

    @Autowired(required = false)
    void setZoneUpdateServiceProvider(ObjectProvider<ZoneUpdateService> zoneUpdateServiceProvider) {
        this.zoneUpdateServiceProvider = zoneUpdateServiceProvider;
    }

    public void initialize() {
        movementNotifyTask();
        moveTaskManager();
        playerMoveTaskManager();
        zoneUpdateService();
    }

    private MovementNotifyTask movementNotifyTask() {
        if (movementNotifyTaskProvider == null) {
            return MovementNotifyTask.getInstance();
        }
        return movementNotifyTaskProvider.getIfAvailable(MovementNotifyTask::getInstance);
    }

    private MoveTaskManager moveTaskManager() {
        if (moveTaskManagerProvider == null) {
            return MoveTaskManager.getInstance();
        }
        return moveTaskManagerProvider.getIfAvailable(MoveTaskManager::getInstance);
    }

    private PlayerMoveTaskManager playerMoveTaskManager() {
        if (playerMoveTaskManagerProvider == null) {
            return PlayerMoveTaskManager.getInstance();
        }
        return playerMoveTaskManagerProvider.getIfAvailable(PlayerMoveTaskManager::getInstance);
    }

    private ZoneUpdateService zoneUpdateService() {
        if (zoneUpdateServiceProvider == null) {
            return ZoneUpdateService.getInstance();
        }
        return zoneUpdateServiceProvider.getIfAvailable(ZoneUpdateService::getInstance);
    }
}
