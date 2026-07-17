package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 移动循环 Spring 服务门面：将提供者注入各任务管理器，并提供静态访问与销毁清理。
 * Movement-loop Spring service facade: wires providers into task managers, exposes static accessors, and cleans up on destroy.
 */
@Component
public final class GameMovementLoopServices implements DisposableBean {

    /**
     * 移动通知任务的 Spring 提供者。
     * Spring provider for the movement-notify task.
     */
    private static volatile ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider;
    /**
     * 移动任务管理器的 Spring 提供者。
     * Spring provider for the move-task manager.
     */
    private static volatile ObjectProvider<MoveTaskManager> moveTaskManagerProvider;
    /**
     * 玩家移动任务管理器的 Spring 提供者。
     * Spring provider for the player-move-task manager.
     */
    private static volatile ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider;
    /**
     * 区域更新服务的 Spring 提供者。
     * Spring provider for the zone-update service.
     */
    private static volatile ObjectProvider<ZoneUpdateService> zoneUpdateServiceProvider;

    private static volatile MovementNotifyTask resolvedMovementNotifyTask;
    private static volatile MoveTaskManager resolvedMoveTaskManager;
    private static volatile PlayerMoveTaskManager resolvedPlayerMoveTaskManager;
    private static volatile ZoneUpdateService resolvedZoneUpdateService;

    /**
     * 构造并注册各移动循环任务的实例提供者。
     * Construct and register instance providers for each movement-loop task.
     *
     * @param movementNotifyTaskProvider 移动通知任务提供者 / Movement-notify task provider
     * @param moveTaskManagerProvider 移动任务管理器提供者 / Move-task-manager provider
     * @param playerMoveTaskManagerProvider 玩家移动任务管理器提供者 / Player-move-task-manager provider
     * @param zoneUpdateServiceProvider 区域更新服务提供者 / Zone-update service provider
     */
    public GameMovementLoopServices(ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider,
            ObjectProvider<MoveTaskManager> moveTaskManagerProvider,
            ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider,
            ObjectProvider<ZoneUpdateService> zoneUpdateServiceProvider) {
        GameMovementLoopServices.movementNotifyTaskProvider = movementNotifyTaskProvider;
        GameMovementLoopServices.moveTaskManagerProvider = moveTaskManagerProvider;
        GameMovementLoopServices.playerMoveTaskManagerProvider = playerMoveTaskManagerProvider;
        GameMovementLoopServices.zoneUpdateServiceProvider = zoneUpdateServiceProvider;
        resolvedMovementNotifyTask = null;
        resolvedMoveTaskManager = null;
        resolvedPlayerMoveTaskManager = null;
        resolvedZoneUpdateService = null;
        MovementNotifyTask.setInstanceProvider(movementNotifyTaskProvider);
        MoveTaskManager.setInstanceProvider(moveTaskManagerProvider);
        PlayerMoveTaskManager.setInstanceProvider(playerMoveTaskManagerProvider);
        ZoneUpdateService.setInstanceProvider(zoneUpdateServiceProvider);
    }

    /**
     * 解析移动通知任务：优先 Spring 提供者，否则单例。
     * Resolve the movement-notify task: prefer Spring provider, otherwise singleton.
     *
     * @return 移动通知任务 / Movement-notify task
     */
    public static MovementNotifyTask movementNotifyTask() {
        MovementNotifyTask resolved = resolvedMovementNotifyTask;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<MovementNotifyTask> provider = movementNotifyTaskProvider;
        resolved = provider == null ? MovementNotifyTask.getInstance()
                : provider.getIfAvailable(MovementNotifyTask::getInstance);
        resolvedMovementNotifyTask = resolved;
        return resolved;
    }

    /**
     * 解析移动任务管理器：优先 Spring 提供者，否则单例。
     * Resolve the move-task manager: prefer Spring provider, otherwise singleton.
     *
     * @return 移动任务管理器 / Move-task manager
     */
    public static MoveTaskManager moveTaskManager() {
        MoveTaskManager resolved = resolvedMoveTaskManager;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<MoveTaskManager> provider = moveTaskManagerProvider;
        resolved = provider == null ? MoveTaskManager.getInstance()
                : provider.getIfAvailable(MoveTaskManager::getInstance);
        resolvedMoveTaskManager = resolved;
        return resolved;
    }

    /**
     * 解析玩家移动任务管理器：优先 Spring 提供者，否则单例。
     * Resolve the player-move-task manager: prefer Spring provider, otherwise singleton.
     *
     * @return 玩家移动任务管理器 / Player-move-task manager
     */
    public static PlayerMoveTaskManager playerMoveTaskManager() {
        PlayerMoveTaskManager resolved = resolvedPlayerMoveTaskManager;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<PlayerMoveTaskManager> provider = playerMoveTaskManagerProvider;
        resolved = provider == null ? PlayerMoveTaskManager.getInstance()
                : provider.getIfAvailable(PlayerMoveTaskManager::getInstance);
        resolvedPlayerMoveTaskManager = resolved;
        return resolved;
    }

    /**
     * 解析区域更新服务：优先 Spring 提供者，否则单例。
     * Resolve the zone-update service: prefer Spring provider, otherwise singleton.
     *
     * @return 区域更新服务 / Zone-update service
     */
    public static ZoneUpdateService zoneUpdateService() {
        ZoneUpdateService resolved = resolvedZoneUpdateService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<ZoneUpdateService> provider = zoneUpdateServiceProvider;
        resolved = provider == null ? ZoneUpdateService.getInstance()
                : provider.getIfAvailable(ZoneUpdateService::getInstance);
        resolvedZoneUpdateService = resolved;
        return resolved;
    }

    /**
     * 销毁时清理静态提供者与任务实例桥。
     * Clear static providers and task instance bridges on destroy.
     */
    @Override
    public void destroy() {
        movementNotifyTaskProvider = null;
        moveTaskManagerProvider = null;
        playerMoveTaskManagerProvider = null;
        zoneUpdateServiceProvider = null;
        resolvedMovementNotifyTask = null;
        resolvedMoveTaskManager = null;
        resolvedPlayerMoveTaskManager = null;
        resolvedZoneUpdateService = null;
        MovementNotifyTask.setInstanceProvider(null);
        MoveTaskManager.setInstanceProvider(null);
        PlayerMoveTaskManager.setInstanceProvider(null);
        ZoneUpdateService.setInstanceProvider(null);
    }
}
