package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameSiegeScheduleGateway {

    public void start() {
        Util.printSection(" *** Sieges *** ");
        SiegeService.getInstance().initSieges();
        BaseService.getInstance().initBases();
    }
}
