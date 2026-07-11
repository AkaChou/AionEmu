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
     *
     * @param movementNotifyTaskProvider 移动通知任务提供者 / Movement-notify task provider
     */
    @Autowired(required = false)
    void setMovementNotifyTaskProvider(ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider) {
        this.movementNotifyTaskProvider = movementNotifyTaskProvider;
    }

    /**
     * 可选注入移动任务管理器提供者。
     * Optionally inject the move-task-manager provider.
     *
     * @param moveTaskManagerProvider 移动任务管理器提供者 / Move-task-manager provider
     */
    @Autowired(required = false)
    void setMoveTaskManagerProvider(ObjectProvider<MoveTaskManager> moveTaskManagerProvider) {
        this.moveTaskManagerProvider = moveTaskManagerProvider;
    }

    /**
     * 可选注入玩家移动任务管理器提供者。
     * Optionally inject the player-move-task-manager provider.
     *
     * @param playerMoveTaskManagerProvider 玩家移动任务管理器提供者 / Player-move-task-manager provider
     */
    @Autowired(required = false)
    void setPlayerMoveTaskManagerProvider(ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider) {
        this.playerMoveTaskManagerProvider = playerMoveTaskManagerProvider;
    }

    /**
     * 可选注入区域更新服务提供者。
     * Optionally inject the zone-update service provider.
     *
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
     * 解析移动通知任务：优先 Spring 提供者，否则单例。
     * Resolve the movement-notify task: prefer Spring provider, otherwise singleton.
     *
     * @return 移动通知任务 / Movement-notify task
     */
    private MovementNotifyTask movementNotifyTask() {
        if (movementNotifyTaskProvider == null) {
            return MovementNotifyTask.getInstance();
        }
        return movementNotifyTaskProvider.getIfAvailable(MovementNotifyTask::getInstance);
    }

    /**
     * 解析移动任务管理器：优先 Spring 提供者，否则单例。
     * Resolve the move-task manager: prefer Spring provider, otherwise singleton.
     *
     * @return 移动任务管理器 / Move-task manager
     */
    private MoveTaskManager moveTaskManager() {
        if (moveTaskManagerProvider == null) {
            return MoveTaskManager.getInstance();
        }
        return moveTaskManagerProvider.getIfAvailable(MoveTaskManager::getInstance);
    }

    /**
     * 解析玩家移动任务管理器：优先 Spring 提供者，否则单例。
     * Resolve the player-move-task manager: prefer Spring provider, otherwise singleton.
     *
     * @return 玩家移动任务管理器 / Player-move-task manager
     */
    private PlayerMoveTaskManager playerMoveTaskManager() {
        if (playerMoveTaskManagerProvider == null) {
            return PlayerMoveTaskManager.getInstance();
        }
        return playerMoveTaskManagerProvider.getIfAvailable(PlayerMoveTaskManager::getInstance);
    }

    /**
     * 解析区域更新服务：优先 Spring 提供者，否则单例。
     * Resolve the zone-update service: prefer Spring provider, otherwise singleton.
     *
     * @return 区域更新服务 / Zone-update service
     */
    private ZoneUpdateService zoneUpdateService() {
        if (zoneUpdateServiceProvider == null) {
            return ZoneUpdateService.getInstance();
        }
        return zoneUpdateServiceProvider.getIfAvailable(ZoneUpdateService::getInstance);
    }
}
