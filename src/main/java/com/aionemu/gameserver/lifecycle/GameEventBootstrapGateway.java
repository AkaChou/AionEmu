package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameEventBootstrapGateway {

    public void bootstrap() {
        Util.printSection(" *** Luna Shop System *** ");
        LunaShopService.getInstance().init();
        Util.printSection(" *** Minion System *** ");
        MinionService.getInstance().init();
        Util.printSection(" *** Shugo Sweep System *** ");
        ShugoSweepService.getInstance().initShugoSweep();
        Util.printSection(" *** Atreian Passport System *** ");
        AtreianPassportService.getInstance().onStart();
        Util.printSection(" *** Event Window System *** ");
        EventWindowService.getInstance().initialize();
    }
}
