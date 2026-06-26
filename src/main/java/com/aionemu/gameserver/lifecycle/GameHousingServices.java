package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameHousingServices implements DisposableBean {

    public GameHousingServices(ObjectProvider<HousingBidService> housingBidServiceProvider,
            ObjectProvider<MaintenanceTask> maintenanceTaskProvider, ObjectProvider<TownService> townServiceProvider,
            ObjectProvider<HousingService> housingServiceProvider,
            ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider) {
        HousingBidService.setInstanceProvider(housingBidServiceProvider);
        MaintenanceTask.setInstanceProvider(maintenanceTaskProvider);
        TownService.setInstanceProvider(townServiceProvider);
        HousingService.setInstanceProvider(housingServiceProvider);
        ChallengeTaskService.setInstanceProvider(challengeTaskServiceProvider);
    }

    @Override
    public void destroy() {
        HousingBidService.setInstanceProvider(null);
        MaintenanceTask.setInstanceProvider(null);
        TownService.setInstanceProvider(null);
        HousingService.setInstanceProvider(null);
        ChallengeTaskService.setInstanceProvider(null);
    }
}
