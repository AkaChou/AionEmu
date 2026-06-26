package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameEventBootstrapServices implements DisposableBean {

    public GameEventBootstrapServices(ObjectProvider<LunaShopService> lunaShopServiceProvider,
            ObjectProvider<MinionService> minionServiceProvider,
            ObjectProvider<ShugoSweepService> shugoSweepServiceProvider,
            ObjectProvider<AtreianPassportService> atreianPassportServiceProvider,
            ObjectProvider<EventWindowService> eventWindowServiceProvider) {
        LunaShopService.setInstanceProvider(lunaShopServiceProvider);
        MinionService.setInstanceProvider(minionServiceProvider);
        ShugoSweepService.setInstanceProvider(shugoSweepServiceProvider);
        AtreianPassportService.setInstanceProvider(atreianPassportServiceProvider);
        EventWindowService.setInstanceProvider(eventWindowServiceProvider);
    }

    @Override
    public void destroy() {
        LunaShopService.setInstanceProvider(null);
        MinionService.setInstanceProvider(null);
        ShugoSweepService.setInstanceProvider(null);
        AtreianPassportService.setInstanceProvider(null);
        EventWindowService.setInstanceProvider(null);
    }
}
