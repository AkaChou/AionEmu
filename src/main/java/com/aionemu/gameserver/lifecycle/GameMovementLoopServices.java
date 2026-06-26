package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameMovementLoopServices implements DisposableBean {

    public GameMovementLoopServices(ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider,
            ObjectProvider<MoveTaskManager> moveTaskManagerProvider,
            ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider,
            ObjectProvider<ZoneUpdateService> zoneUpdateServiceProvider) {
        MovementNotifyTask.setInstanceProvider(movementNotifyTaskProvider);
        MoveTaskManager.setInstanceProvider(moveTaskManagerProvider);
        PlayerMoveTaskManager.setInstanceProvider(playerMoveTaskManagerProvider);
        ZoneUpdateService.setInstanceProvider(zoneUpdateServiceProvider);
    }

    @Override
    public void destroy() {
        MovementNotifyTask.setInstanceProvider(null);
        MoveTaskManager.setInstanceProvider(null);
        PlayerMoveTaskManager.setInstanceProvider(null);
        ZoneUpdateService.setInstanceProvider(null);
    }
}
