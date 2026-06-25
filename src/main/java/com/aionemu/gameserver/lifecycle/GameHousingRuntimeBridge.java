package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameHousingRuntimeBridge {

    public void printHousingSection() {
        Util.printSection(" *** Housing *** ");
    }

    public HousingBidService housingBidService() {
        return HousingBidService.getInstance();
    }

    public MaintenanceTask maintenanceTask() {
        return MaintenanceTask.getInstance();
    }

    public TownService townService() {
        return TownService.getInstance();
    }

    public ChallengeTaskService challengeTaskService() {
        return ChallengeTaskService.getInstance();
    }
}
