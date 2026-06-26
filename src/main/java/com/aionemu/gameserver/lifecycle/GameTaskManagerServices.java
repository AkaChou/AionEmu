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

    public GameTaskManagerServices(ObjectProvider<ExpireTimerTask> expireTimerTaskProvider,
            ObjectProvider<TeamEffectUpdater> teamEffectUpdaterProvider,
            ObjectProvider<TeamMoveUpdater> teamMoveUpdaterProvider,
            ObjectProvider<TemporaryTradeTimeTask> temporaryTradeTimeTaskProvider) {
        ExpireTimerTask.setInstanceProvider(expireTimerTaskProvider);
        TeamEffectUpdater.setInstanceProvider(teamEffectUpdaterProvider);
        TeamMoveUpdater.setInstanceProvider(teamMoveUpdaterProvider);
        TemporaryTradeTimeTask.setInstanceProvider(temporaryTradeTimeTaskProvider);
    }

    @Override
    public void destroy() {
        ExpireTimerTask.setInstanceProvider(null);
        TeamEffectUpdater.setInstanceProvider(null);
        TeamMoveUpdater.setInstanceProvider(null);
        TemporaryTradeTimeTask.setInstanceProvider(null);
    }
}
