package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameHousingGateway {

    private ObjectProvider<HousingBidService> housingBidServiceProvider;
    private ObjectProvider<MaintenanceTask> maintenanceTaskProvider;
    private ObjectProvider<TownService> townServiceProvider;
    private ObjectProvider<HousingService> housingServiceProvider;
    private ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider;
    private ObjectProvider<GameHousingRuntimeBridge> runtimeBridgeProvider;

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

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameHousingRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void start() {
        runtimeBridge().printHousingSection();
        housingBidService().start();
        maintenanceTask();
        townService();
        housingService();
        challengeTaskService();
    }

    private HousingBidService housingBidService() {
        if (housingBidServiceProvider == null) {
            return runtimeBridge().housingBidService();
        }
        return housingBidServiceProvider.getIfAvailable(() -> runtimeBridge().housingBidService());
    }

    private MaintenanceTask maintenanceTask() {
        if (maintenanceTaskProvider == null) {
            return runtimeBridge().maintenanceTask();
        }
        return maintenanceTaskProvider.getIfAvailable(() -> runtimeBridge().maintenanceTask());
    }

    private TownService townService() {
        if (townServiceProvider == null) {
            return runtimeBridge().townService();
        }
        return townServiceProvider.getIfAvailable(() -> runtimeBridge().townService());
    }

    private HousingService housingService() {
        if (housingServiceProvider == null) {
            return runtimeBridge().housingService();
        }
        return housingServiceProvider.getIfAvailable(() -> runtimeBridge().housingService());
    }

    private ChallengeTaskService challengeTaskService() {
        if (challengeTaskServiceProvider == null) {
            return runtimeBridge().challengeTaskService();
        }
        return challengeTaskServiceProvider.getIfAvailable(() -> runtimeBridge().challengeTaskService());
    }

    private GameHousingRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameHousingRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameHousingRuntimeBridge::new);
    }
}
