package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;

final class GameHousingFallbacks {

    private GameHousingFallbacks() {
    }

    static HousingBidService housingBidService() {
        return HousingBidServiceFallback.INSTANCE;
    }

    static MaintenanceTask maintenanceTask() {
        return MaintenanceTaskFallback.INSTANCE;
    }

    static TownService townService() {
        return TownServiceFallback.INSTANCE;
    }

    static ChallengeTaskService challengeTaskService() {
        return ChallengeTaskServiceFallback.INSTANCE;
    }

    private static final class HousingBidServiceFallback {
        private static final HousingBidService INSTANCE = HousingBidService.getInstance();
    }

    private static final class MaintenanceTaskFallback {
        private static final MaintenanceTask INSTANCE = MaintenanceTask.getInstance();
    }

    private static final class TownServiceFallback {
        private static final TownService INSTANCE = TownService.getInstance();
    }

    private static final class ChallengeTaskServiceFallback {
        private static final ChallengeTaskService INSTANCE = ChallengeTaskService.getInstance();
    }
}
