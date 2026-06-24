package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.services.events.PigPoppyEventService;
import com.aionemu.gameserver.services.events.TreasureAbyssService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameScheduledServicesGateway {

    public void start() {
        Util.printSection(" *** Scheduled Services *** ");
        if (EventsConfig.ENABLE_PIG_POPPY_EVENT) {
            PigPoppyEventService.ScheduleCron();
        }
        if (EventsConfig.ENABLE_ABYSS_EVENT) {
            TreasureAbyssService.ScheduleCron();
        }
        if (EventsConfig.IMPERIAL_TOMB_ENABLE) {
            ShugoImperialTombSpawnManager.getInstance().start();
        }
    }
}
