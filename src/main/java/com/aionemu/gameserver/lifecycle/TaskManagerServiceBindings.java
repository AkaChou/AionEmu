package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.taskmanager.tasks.TeamEffectUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TeamMoveUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TemporaryTradeTimeTask;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 任务管理服务静态绑定：把 Spring ObjectProvider 喂入任务管理域服务自持的 fail-fast 单例通道。
 * Task-manager static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Task-manager services.
 * <p>本组件取代已退役的任务管理静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Task-manager static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class TaskManagerServiceBindings implements DisposableBean {

    /**
     * 构造并注册各任务管理服务的实例提供者。
     * Construct and register instance providers for Task-manager services.
     * @param expireTimerTaskProvider ExpireTimerTask 提供者 / ExpireTimerTask provider
     * @param teamEffectUpdaterProvider TeamEffectUpdater 提供者 / TeamEffectUpdater provider
     * @param teamMoveUpdaterProvider TeamMoveUpdater 提供者 / TeamMoveUpdater provider
     * @param temporaryTradeTimeTaskProvider TemporaryTradeTimeTask 提供者 / TemporaryTradeTimeTask provider
     */
    public TaskManagerServiceBindings(ObjectProvider<ExpireTimerTask> expireTimerTaskProvider,
            ObjectProvider<TeamEffectUpdater> teamEffectUpdaterProvider,
            ObjectProvider<TeamMoveUpdater> teamMoveUpdaterProvider,
            ObjectProvider<TemporaryTradeTimeTask> temporaryTradeTimeTaskProvider) {
        ExpireTimerTask.setInstanceProvider(expireTimerTaskProvider);
        TeamEffectUpdater.setInstanceProvider(teamEffectUpdaterProvider);
        TeamMoveUpdater.setInstanceProvider(teamMoveUpdaterProvider);
        TemporaryTradeTimeTask.setInstanceProvider(temporaryTradeTimeTaskProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        ExpireTimerTask.setInstanceProvider(null);
        TeamEffectUpdater.setInstanceProvider(null);
        TeamMoveUpdater.setInstanceProvider(null);
        TemporaryTradeTimeTask.setInstanceProvider(null);
    }
}
