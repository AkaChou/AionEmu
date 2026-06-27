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

    private static volatile ObjectProvider<HousingBidService> housingBidServiceProvider;
    private static volatile ObjectProvider<MaintenanceTask> maintenanceTaskProvider;
    private static volatile ObjectProvider<TownService> townServiceProvider;
    private static volatile ObjectProvider<HousingService> housingServiceProvider;

    public GameHousingServices(ObjectProvider<HousingBidService> housingBidServiceProvider,
            ObjectProvider<MaintenanceTask> maintenanceTaskProvider, ObjectProvider<TownService> townServiceProvider,
            ObjectProvider<HousingService> housingServiceProvider,
            ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider) {
        GameHousingServices.housingBidServiceProvider = housingBidServiceProvider;
        GameHousingServices.maintenanceTaskProvider = maintenanceTaskProvider;
        GameHousingServices.townServiceProvider = townServiceProvider;
        HousingBidService.setInstanceProvider(housingBidServiceProvider);
        MaintenanceTask.setInstanceProvider(maintenanceTaskProvider);
        TownService.setInstanceProvider(townServiceProvider);
        GameHousingServices.housingServiceProvider = housingServiceProvider;
        HousingService.setInstanceProvider(housingServiceProvider);
        ChallengeTaskService.setInstanceProvider(challengeTaskServiceProvider);
    }

    public static HousingBidService housingBidService() {
        ObjectProvider<HousingBidService> provider = housingBidServiceProvider;
        if (provider == null) {
            return GameHousingFallbacks.housingBidService();
        }
        return provider.getIfAvailable(GameHousingFallbacks::housingBidService);
    }

    public static MaintenanceTask maintenanceTask() {
        ObjectProvider<MaintenanceTask> provider = maintenanceTaskProvider;
        if (provider == null) {
            return GameHousingFallbacks.maintenanceTask();
        }
        return provider.getIfAvailable(GameHousingFallbacks::maintenanceTask);
    }

    public static TownService townService() {
        ObjectProvider<TownService> provider = townServiceProvider;
        if (provider == null) {
            return GameHousingFallbacks.townService();
        }
        return provider.getIfAvailable(GameHousingFallbacks::townService);
    }

    public static HousingService housingService() {
        ObjectProvider<HousingService> provider = housingServiceProvider;
        if (provider == null) {
            return GameHousingFallbacks.housingService();
        }
        return provider.getIfAvailable(GameHousingFallbacks::housingService);
    }

    @Override
    public void destroy() {
        housingBidServiceProvider = null;
        HousingBidService.setInstanceProvider(null);
        maintenanceTaskProvider = null;
        MaintenanceTask.setInstanceProvider(null);
        townServiceProvider = null;
        TownService.setInstanceProvider(null);
        housingServiceProvider = null;
        HousingService.setInstanceProvider(null);
        ChallengeTaskService.setInstanceProvider(null);
    }
}
