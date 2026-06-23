package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameHousingGateway {

    public void start() {
        Util.printSection(" *** Housing *** ");
        HousingBidService.getInstance().start();
        MaintenanceTask.getInstance();
        TownService.getInstance();
        ChallengeTaskService.getInstance();
    }
}
