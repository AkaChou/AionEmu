package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 移动循环网关：经提供者或 getInstance 初始化移动通知/移动/玩家移动/区域更新任务。
 * Movement-loop gateway: initializes movement-notify/move/player-move/zone-update tasks via providers or getInstance.
 */
@Component
public class GameMovementLoopGateway {

    /**
     * 移动通知任务提供者。
     * Movement-notify task provider.
     */
    private ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider;
    /**
     * 移动任务管理器提供者。
     * Move-task-manager provider.
     */
    private ObjectProvider<MoveTaskManager> moveTaskManagerProvider;
    /**
     * 玩家移动任务管理器提供者。
     * Player-move-task-manager provider.
     */
    private ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider;
    /**
     * 区域更新服务提供者。
     * Zone-update service provider.
     */
    private ObjectProvider<ZoneUpdateService> zoneUpdateServiceProvider;

    /**
     * 可选注入移动通知任务提供者。
     * Optionally inject the movement-notify task provider.
     * @param movementNotifyTaskProvider 移动通知任务提供者 / Movement-notify task provider
     */
    @Autowired(required = false)
    void setMovementNotifyTaskProvider(ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider) {
        this.movementNotifyTaskProvider = movementNotifyTaskProvider;
    }

    /**
     * 可选注入移动任务管理器提供者。
     * Optionally inject the move-task-manager provider.
     * @param moveTaskManagerProvider 移动任务管理器提供者 / Move-task-manager provider
     */
    @Autowired(required = false)
    void setMoveTaskManagerProvider(ObjectProvider<MoveTaskManager> moveTaskManagerProvider) {
        this.moveTaskManagerProvider = moveTaskManagerProvider;
    }

    /**
     * 可选注入玩家移动任务管理器提供者。
     * Optionally inject the player-move-task-manager provider.
     * @param playerMoveTaskManagerProvider 玩家移动任务管理器提供者 / Player-move-task-manager provider
     */
    @Autowired(required = false)
    void setPlayerMoveTaskManagerProvider(ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider) {
        this.playerMoveTaskManagerProvider = playerMoveTaskManagerProvider;
    }

    /**
     * 可选注入区域更新服务提供者。
     * Optionally inject the zone-update service provider.
     * @param zoneUpdateServiceProvider 区域更新服务提供者 / Zone-update service provider
     */
    @Autowired(required = false)
    void setZoneUpdateServiceProvider(ObjectProvider<ZoneUpdateService> zoneUpdateServiceProvider) {
        this.zoneUpdateServiceProvider = zoneUpdateServiceProvider;
    }

    /**
     * 初始化移动循环相关任务（触达解析以激活单例/Bean）。
     * Initialize movement-loop tasks (resolve each to activate singleton/bean).
     */
    public void initialize() {
        movementNotifyTask();
        moveTaskManager();
        playerMoveTaskManager();
        zoneUpdateService();
    }

    /**
     * 触发移动通知任务解析：优先 Spring 提供者，否则单例。
     * Trigger movement-notify task resolution: prefer Spring provider, otherwise singleton.
     */
    private void movementNotifyTask() {
        if (movementNotifyTaskProvider == null) {
            MovementNotifyTask.getInstance();
            return;
        }
        movementNotifyTaskProvider.getIfAvailable(MovementNotifyTask::getInstance);
    }

    /**
     * 触发移动任务管理器解析：优先 Spring 提供者，否则单例。
     * Trigger move-task manager resolution: prefer Spring provider, otherwise singleton.
     */
    private void moveTaskManager() {
        if (moveTaskManagerProvider == null) {
            MoveTaskManager.getInstance();
            return;
        }
        moveTaskManagerProvider.getIfAvailable(MoveTaskManager::getInstance);
    }

    /**
     * 触发玩家移动任务管理器解析：优先 Spring 提供者，否则单例。
     * Trigger player-move-task manager resolution: prefer Spring provider, otherwise singleton.
     */
    private void playerMoveTaskManager() {
        if (playerMoveTaskManagerProvider == null) {
            PlayerMoveTaskManager.getInstance();
            return;
        }
        playerMoveTaskManagerProvider.getIfAvailable(PlayerMoveTaskManager::getInstance);
    }

    /**
     * 触发区域更新服务解析：优先 Spring 提供者，否则单例。
     * Trigger zone-update service resolution: prefer Spring provider, otherwise singleton.
     */
    private void zoneUpdateService() {
        if (zoneUpdateServiceProvider == null) {
            ZoneUpdateService.getInstance();
            return;
        }
        zoneUpdateServiceProvider.getIfAvailable(ZoneUpdateService::getInstance);
    }
}
