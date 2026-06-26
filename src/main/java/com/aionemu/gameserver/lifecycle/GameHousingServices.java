package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.TownService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameHousingServices implements DisposableBean {

    public GameHousingServices(ObjectProvider<TownService> townServiceProvider,
            ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider) {
        TownService.setInstanceProvider(townServiceProvider);
        ChallengeTaskService.setInstanceProvider(challengeTaskServiceProvider);
    }

    @Override
    public void destroy() {
        TownService.setInstanceProvider(null);
        ChallengeTaskService.setInstanceProvider(null);
    }
}
