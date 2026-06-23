package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.FFAConfig;
import com.aionemu.gameserver.configs.main.PvPModConfig;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameCustomEventsGateway {

    public void start() {
        Util.printSection(" *** Custom Events *** ");
        if (FFAConfig.FFA_ENABLED) {
            FFAService.getInstance();
        }
        if (PvPModConfig.BG_ENABLED) {
            LadderService.getInstance();
            BGService.getInstance();
        }
        BanditService.getInstance().onInit();
    }
}
