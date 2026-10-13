package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 移动循环服务静态绑定：把 Spring ObjectProvider 喂入移动循环域服务自持的 fail-fast 单例通道。
 * Movement-loop static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Movement-loop services.
 * <p>本组件取代已退役的移动循环静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Movement-loop static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class MovementLoopServiceBindings implements DisposableBean {

    /**
     * 构造并注册各移动循环服务的实例提供者。
     * Construct and register instance providers for Movement-loop services.
     * @param movementNotifyTaskProvider MovementNotifyTask 提供者 / MovementNotifyTask provider
     * @param moveTaskManagerProvider MoveTaskManager 提供者 / MoveTaskManager provider
     * @param playerMoveTaskManagerProvider PlayerMoveTaskManager 提供者 / PlayerMoveTaskManager provider
     * @param zoneUpdateServiceProvider ZoneUpdateService 提供者 / ZoneUpdateService provider
     */
    public MovementLoopServiceBindings(ObjectProvider<MovementNotifyTask> movementNotifyTaskProvider,
            ObjectProvider<MoveTaskManager> moveTaskManagerProvider,
            ObjectProvider<PlayerMoveTaskManager> playerMoveTaskManagerProvider,
            ObjectProvider<ZoneUpdateService> zoneUpdateServiceProvider) {
        MovementNotifyTask.setInstanceProvider(movementNotifyTaskProvider);
        MoveTaskManager.setInstanceProvider(moveTaskManagerProvider);
        PlayerMoveTaskManager.setInstanceProvider(playerMoveTaskManagerProvider);
        ZoneUpdateService.setInstanceProvider(zoneUpdateServiceProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        MovementNotifyTask.setInstanceProvider(null);
        MoveTaskManager.setInstanceProvider(null);
        PlayerMoveTaskManager.setInstanceProvider(null);
        ZoneUpdateService.setInstanceProvider(null);
    }
}
