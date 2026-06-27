package com.aionemu.gameserver.lifecycle;

import java.util.function.Supplier;

import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameMovementLoopServices implements DisposableBean {

    private static volatile ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider;
    private static volatile ObjectProvider<MoveTaskManager> moveTaskManagerProvider;
    private static volatile ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider;
    private static volatile ObjectProvider<ZoneUpdateService> zoneUpdateServiceProvider;

    public GameMovementLoopServices(ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider,
            ObjectProvider<MoveTaskManager> moveTaskManagerProvider,
            ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider,
            ObjectProvider<ZoneUpdateService> zoneUpdateServiceProvider) {
        GameMovementLoopServices.movementNotifyTaskProvider = movementNotifyTaskProvider;
        GameMovementLoopServices.moveTaskManagerProvider = moveTaskManagerProvider;
        GameMovementLoopServices.playerMoveTaskManagerProvider = playerMoveTaskManagerProvider;
        GameMovementLoopServices.zoneUpdateServiceProvider = zoneUpdateServiceProvider;
        MovementNotifyTask.setInstanceProvider(movementNotifyTaskProvider);
        MoveTaskManager.setInstanceProvider(moveTaskManagerProvider);
        PlayerMoveTaskManager.setInstanceProvider(playerMoveTaskManagerProvider);
        ZoneUpdateService.setInstanceProvider(zoneUpdateServiceProvider);
    }

    public static MovementNotifyTask movementNotifyTask() {
        return getIfAvailable(movementNotifyTaskProvider, MovementNotifyTask::getInstance);
    }

    public static MoveTaskManager moveTaskManager() {
        return getIfAvailable(moveTaskManagerProvider, MoveTaskManager::getInstance);
    }

    public static PlayerMoveTaskManager playerMoveTaskManager() {
        return getIfAvailable(playerMoveTaskManagerProvider, PlayerMoveTaskManager::getInstance);
    }

    public static ZoneUpdateService zoneUpdateService() {
        return getIfAvailable(zoneUpdateServiceProvider, ZoneUpdateService::getInstance);
    }

    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }

    @Override
    public void destroy() {
        movementNotifyTaskProvider = null;
        moveTaskManagerProvider = null;
        playerMoveTaskManagerProvider = null;
        zoneUpdateServiceProvider = null;
        MovementNotifyTask.setInstanceProvider(null);
        MoveTaskManager.setInstanceProvider(null);
        PlayerMoveTaskManager.setInstanceProvider(null);
        ZoneUpdateService.setInstanceProvider(null);
    }
}
