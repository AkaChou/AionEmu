package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.services.events.PigPoppyEventService;
import com.aionemu.gameserver.services.events.TreasureAbyssService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameScheduledServicesGateway {

    private ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider;

    @Autowired(required = false)
    void setShugoImperialTombSpawnManagerProvider(ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider) {
        this.shugoImperialTombSpawnManagerProvider = shugoImperialTombSpawnManagerProvider;
    }

    public void start() {
        Util.printSection(" *** Scheduled Services *** ");
        if (EventsConfig.ENABLE_PIG_POPPY_EVENT) {
            PigPoppyEventService.ScheduleCron();
        }
        if (EventsConfig.ENABLE_ABYSS_EVENT) {
            TreasureAbyssService.ScheduleCron();
        }
        if (EventsConfig.IMPERIAL_TOMB_ENABLE) {
            shugoImperialTombSpawnManager().start();
        }
    }

    private ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        if (shugoImperialTombSpawnManagerProvider == null) {
            return ShugoImperialTombSpawnManager.getInstance();
        }
        return shugoImperialTombSpawnManagerProvider.getIfAvailable(ShugoImperialTombSpawnManager::getInstance);
    }
}
