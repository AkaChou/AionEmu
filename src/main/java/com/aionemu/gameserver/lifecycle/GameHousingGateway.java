package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameHousingGateway {

    private ObjectProvider<HousingBidService> housingBidServiceProvider;
    private ObjectProvider<MaintenanceTask> maintenanceTaskProvider;
    private ObjectProvider<TownService> townServiceProvider;
    private ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider;

    @Autowired(required = false)
    void setHousingBidServiceProvider(ObjectProvider<HousingBidService> housingBidServiceProvider) {
        this.housingBidServiceProvider = housingBidServiceProvider;
    }

    @Autowired(required = false)
    void setMaintenanceTaskProvider(ObjectProvider<MaintenanceTask> maintenanceTaskProvider) {
        this.maintenanceTaskProvider = maintenanceTaskProvider;
    }

    @Autowired(required = false)
    void setTownServiceProvider(ObjectProvider<TownService> townServiceProvider) {
        this.townServiceProvider = townServiceProvider;
    }

    @Autowired(required = false)
    void setChallengeTaskServiceProvider(ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider) {
        this.challengeTaskServiceProvider = challengeTaskServiceProvider;
    }

    public void start() {
        Util.printSection(" *** Housing *** ");
        housingBidService().start();
        maintenanceTask();
        townService();
        challengeTaskService();
    }

    private HousingBidService housingBidService() {
        if (housingBidServiceProvider == null) {
            return HousingBidService.getInstance();
        }
        return housingBidServiceProvider.getIfAvailable(HousingBidService::getInstance);
    }

    private MaintenanceTask maintenanceTask() {
        if (maintenanceTaskProvider == null) {
            return MaintenanceTask.getInstance();
        }
        return maintenanceTaskProvider.getIfAvailable(MaintenanceTask::getInstance);
    }

    private TownService townService() {
        if (townServiceProvider == null) {
            return TownService.getInstance();
        }
        return townServiceProvider.getIfAvailable(TownService::getInstance);
    }

    private ChallengeTaskService challengeTaskService() {
        if (challengeTaskServiceProvider == null) {
            return ChallengeTaskService.getInstance();
        }
        return challengeTaskServiceProvider.getIfAvailable(ChallengeTaskService::getInstance);
    }
}
