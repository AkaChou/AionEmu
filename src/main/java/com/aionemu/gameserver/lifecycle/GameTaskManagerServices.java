package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.taskmanager.tasks.TeamEffectUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TeamMoveUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TemporaryTradeTimeTask;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameTaskManagerServices implements DisposableBean {

    private static volatile ObjectProvider<ExpireTimerTask> expireTimerTaskProvider;
    private static volatile ObjectProvider<TeamEffectUpdater> teamEffectUpdaterProvider;
    private static volatile ObjectProvider<TeamMoveUpdater> teamMoveUpdaterProvider;
    private static volatile ObjectProvider<TemporaryTradeTimeTask> temporaryTradeTimeTaskProvider;

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

    public static ExpireTimerTask expireTimerTask() {
        ObjectProvider<ExpireTimerTask> provider = expireTimerTaskProvider;
        if (provider == null) {
            return ExpireTimerTask.getInstance();
        }
        return provider.getIfAvailable(ExpireTimerTask::getInstance);
    }

    public static TeamEffectUpdater teamEffectUpdater() {
        ObjectProvider<TeamEffectUpdater> provider = teamEffectUpdaterProvider;
        if (provider == null) {
            return TeamEffectUpdater.getInstance();
        }
        return provider.getIfAvailable(TeamEffectUpdater::getInstance);
    }

    public static TeamMoveUpdater teamMoveUpdater() {
        ObjectProvider<TeamMoveUpdater> provider = teamMoveUpdaterProvider;
        if (provider == null) {
            return TeamMoveUpdater.getInstance();
        }
        return provider.getIfAvailable(TeamMoveUpdater::getInstance);
    }

    public static TemporaryTradeTimeTask temporaryTradeTimeTask() {
        ObjectProvider<TemporaryTradeTimeTask> provider = temporaryTradeTimeTaskProvider;
        if (provider == null) {
            return TemporaryTradeTimeTask.getInstance();
        }
        return provider.getIfAvailable(TemporaryTradeTimeTask::getInstance);
    }

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
