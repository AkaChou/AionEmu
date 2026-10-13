package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 静态数据生命周期：驱动静态数据加载与移动循环初始化，加载状态由基类承载。
 * Static-data lifecycle: drives static-data load and movement-loop init; load state is hosted by
 * the base class.
 * <p>加载必须发生在 Bean 创建回调之外：Spring 创建单例期间持有全局 singletonLock，
 * 若加载在构造中进行，并行加载线程解析任何懒加载 Bean 都会与主线程互相等待形成死锁。
 * Loading must happen outside bean-creation callbacks: Spring holds its global singleton lock
 * while creating a singleton, so loading inside the constructor deadlocks against parallel
 * loader threads resolving lazy beans.</p>
 */
@Component
public class GameStaticDataLifecycle extends GameStartupStage {

    /**
     * DataManager 的可选 Spring 提供者。
     * Optional Spring provider for {@link DataManager}.
     */
    private ObjectProvider<DataManager> dataManagerProvider;

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
     * 注入 DataManager 提供者。
     * Inject the DataManager provider.
     * @param dataManagerProvider DataManager 提供者 / DataManager provider
     */
    @Autowired(required = false)
    void setDataManagerProvider(ObjectProvider<DataManager> dataManagerProvider) {
        this.dataManagerProvider = dataManagerProvider;
    }

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
     * 启动本阶段：加载静态数据并初始化移动循环。
     * Start this stage: load static data and initialize the movement loop.
     */
    @Override
    protected void runStage() {
        loadStaticData();
        initializeMovementLoops();
    }

    /**
     * 加载静态数据：解析 DataManager Bean（轻量构造）后显式触发加载。
     * Load static data: resolve the DataManager bean (cheap construction), then trigger loading explicitly.
     */
    protected void loadStaticData() {
        dataManagerProvider.getObject().load();
    }

    /**
     * 初始化移动循环相关任务（触达解析以激活单例/Bean）。
     * Initialize movement-loop tasks (resolve each to activate singleton/bean).
     */
    protected void initializeMovementLoops() {
        movementNotifyTaskProvider.getObject();
        moveTaskManagerProvider.getObject();
        playerMoveTaskManagerProvider.getObject();
        zoneUpdateServiceProvider.getObject();
    }
}
