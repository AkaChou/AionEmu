package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.taskmanager.tasks.TeamEffectUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TeamMoveUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TemporaryTradeTimeTask;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 任务管理器服务定位器：向各类周期任务注入 Spring 提供者。
 * Task-manager service locator: injects Spring providers into periodic task types.
 */
@Component
public final class GameTaskManagerServices implements DisposableBean {

    /**
     * ExpireTimerTask 提供者的静态缓存。
     * Static cache of the ExpireTimerTask provider.
     */
    private static volatile ObjectProvider<ExpireTimerTask> expireTimerTaskProvider;

    /**
     * TeamEffectUpdater 提供者的静态缓存。
     * Static cache of the TeamEffectUpdater provider.
     */
    private static volatile ObjectProvider<TeamEffectUpdater> teamEffectUpdaterProvider;

    /**
     * TeamMoveUpdater 提供者的静态缓存。
     * Static cache of the TeamMoveUpdater provider.
     */
    private static volatile ObjectProvider<TeamMoveUpdater> teamMoveUpdaterProvider;

    /**
     * TemporaryTradeTimeTask 提供者的静态缓存。
     * Static cache of the TemporaryTradeTimeTask provider.
     */
    private static volatile ObjectProvider<TemporaryTradeTimeTask> temporaryTradeTimeTaskProvider;

    /**
     * 构造并注册各任务管理器组件的实例提供者。
     * Construct and register instance providers for task-manager components.
     *
     * @param expireTimerTaskProvider 过期计时任务提供者 / Expire-timer-task provider
     * @param teamEffectUpdaterProvider 队伍效果更新器提供者 / Team-effect-updater provider
     * @param teamMoveUpdaterProvider 队伍移动更新器提供者 / Team-move-updater provider
     * @param temporaryTradeTimeTaskProvider 临时交易时间任务提供者 / Temporary-trade-time-task provider
     */
    public GameTaskManagerServices(ObjectProvider<ExpireTimerTask> expireTimerTaskProvider,
            ObjectProvider<TeamEffectUpdater> teamEffectUpdaterProvider,
            ObjectProvider<TeamMoveUpdater> teamMoveUpdaterProvider,
            ObjectProvider<TemporaryTradeTimeTask> temporaryTradeTimeTaskProvider) {
        GameTaskManagerServices.expireTimerTaskProvider = expireTimerTaskProvider;
        GameTaskManagerServices.teamEffectUpdaterProvider = teamEffectUpdaterProvider;
        GameTaskManagerServices.teamMoveUpdaterProvider = teamMoveUpdaterProvider;
        GameTaskManagerServices.temporaryTradeTimeTaskProvider = temporaryTradeTimeTaskProvider;
        ExpireTimerTask.setInstanceProvider(expireTimerTaskProvider);
        TeamEffectUpdater.setInstanceProvider(teamEffectUpdaterProvider);
        TeamMoveUpdater.setInstanceProvider(teamMoveUpdaterProvider);
        TemporaryTradeTimeTask.setInstanceProvider(temporaryTradeTimeTaskProvider);
    }

    /**
     * 解析 ExpireTimerTask：优先 Spring，否则 getInstance。
     * Resolve ExpireTimerTask: prefer Spring, otherwise {@code getInstance}.
     *
     * @return ExpireTimerTask 实例 / ExpireTimerTask instance
     */
    public static ExpireTimerTask expireTimerTask() {
        ObjectProvider<ExpireTimerTask> provider = expireTimerTaskProvider;
        if (provider == null) {
            return ExpireTimerTask.getInstance();
        }
        return provider.getIfAvailable(ExpireTimerTask::getInstance);
    }

    /**
     * 解析 TeamEffectUpdater：优先 Spring，否则 getInstance。
     * Resolve TeamEffectUpdater: prefer Spring, otherwise {@code getInstance}.
     *
     * @return TeamEffectUpdater 实例 / TeamEffectUpdater instance
     */
    public static TeamEffectUpdater teamEffectUpdater() {
        ObjectProvider<TeamEffectUpdater> provider = teamEffectUpdaterProvider;
        if (provider == null) {
            return TeamEffectUpdater.getInstance();
        }
        return provider.getIfAvailable(TeamEffectUpdater::getInstance);
    }

    /**
     * 解析 TeamMoveUpdater：优先 Spring，否则 getInstance。
     * Resolve TeamMoveUpdater: prefer Spring, otherwise {@code getInstance}.
     *
     * @return TeamMoveUpdater 实例 / TeamMoveUpdater instance
     */
    public static TeamMoveUpdater teamMoveUpdater() {
        ObjectProvider<TeamMoveUpdater> provider = teamMoveUpdaterProvider;
        if (provider == null) {
            return TeamMoveUpdater.getInstance();
        }
        return provider.getIfAvailable(TeamMoveUpdater::getInstance);
    }

    /**
     * 解析 TemporaryTradeTimeTask：优先 Spring，否则 getInstance。
     * Resolve TemporaryTradeTimeTask: prefer Spring, otherwise {@code getInstance}.
     *
     * @return TemporaryTradeTimeTask 实例 / TemporaryTradeTimeTask instance
     */
    public static TemporaryTradeTimeTask temporaryTradeTimeTask() {
        ObjectProvider<TemporaryTradeTimeTask> provider = temporaryTradeTimeTaskProvider;
        if (provider == null) {
            return TemporaryTradeTimeTask.getInstance();
        }
        return provider.getIfAvailable(TemporaryTradeTimeTask::getInstance);
    }

    /**
     * 销毁时清空静态提供者与单例注册。
     * Clear static providers and singleton registrations on destroy.
     */
    @Override
    public void destroy() {
        expireTimerTaskProvider = null;
        ExpireTimerTask.setInstanceProvider(null);
        teamEffectUpdaterProvider = null;
        TeamEffectUpdater.setInstanceProvider(null);
        teamMoveUpdaterProvider = null;
        TeamMoveUpdater.setInstanceProvider(null);
        temporaryTradeTimeTaskProvider = null;
        TemporaryTradeTimeTask.setInstanceProvider(null);
    }
}
