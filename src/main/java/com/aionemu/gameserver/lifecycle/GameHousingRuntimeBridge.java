package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameHousingRuntimeBridge {

    private ObjectProvider<HousingBidService> housingBidServiceProvider;
    private ObjectProvider<MaintenanceTask> maintenanceTaskProvider;
    private ObjectProvider<TownService> townServiceProvider;
    private ObjectProvider<HousingService> housingServiceProvider;
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
    void setHousingServiceProvider(ObjectProvider<HousingService> housingServiceProvider) {
        this.housingServiceProvider = housingServiceProvider;
    }

    @Autowired(required = false)
    void setChallengeTaskServiceProvider(ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider) {
        this.challengeTaskServiceProvider = challengeTaskServiceProvider;
    }

    public void printHousingSection() {
        Util.printSection(" *** Housing *** ");
    }

    public HousingBidService housingBidService() {
        if (housingBidServiceProvider == null) {
            return GameHousingFallbacks.housingBidService();
        }
        return housingBidServiceProvider.getIfAvailable(GameHousingFallbacks::housingBidService);
    }

    public MaintenanceTask maintenanceTask() {
        if (maintenanceTaskProvider == null) {
            return GameHousingFallbacks.maintenanceTask();
        }
        return maintenanceTaskProvider.getIfAvailable(GameHousingFallbacks::maintenanceTask);
    }

    public TownService townService() {
        if (townServiceProvider == null) {
            return GameHousingFallbacks.townService();
        }
        return townServiceProvider.getIfAvailable(GameHousingFallbacks::townService);
    }

    public HousingService housingService() {
        if (housingServiceProvider == null) {
            return GameHousingFallbacks.housingService();
        }
        return housingServiceProvider.getIfAvailable(GameHousingFallbacks::housingService);
    }

    public ChallengeTaskService challengeTaskService() {
        if (challengeTaskServiceProvider == null) {
            return GameHousingFallbacks.challengeTaskService();
        }
        return challengeTaskServiceProvider.getIfAvailable(GameHousingFallbacks::challengeTaskService);
    }
}
