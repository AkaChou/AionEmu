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

    private static volatile ObjectProvider<LunaShopService> lunaShopServiceProvider;
    private static volatile ObjectProvider<MinionService> minionServiceProvider;

    public GameEventBootstrapServices(ObjectProvider<LunaShopService> lunaShopServiceProvider,
            ObjectProvider<MinionService> minionServiceProvider,
            ObjectProvider<ShugoSweepService> shugoSweepServiceProvider,
            ObjectProvider<AtreianPassportService> atreianPassportServiceProvider,
            ObjectProvider<EventWindowService> eventWindowServiceProvider) {
        GameEventBootstrapServices.lunaShopServiceProvider = lunaShopServiceProvider;
        GameEventBootstrapServices.minionServiceProvider = minionServiceProvider;
        LunaShopService.setInstanceProvider(lunaShopServiceProvider);
        MinionService.setInstanceProvider(minionServiceProvider);
        ShugoSweepService.setInstanceProvider(shugoSweepServiceProvider);
        AtreianPassportService.setInstanceProvider(atreianPassportServiceProvider);
        EventWindowService.setInstanceProvider(eventWindowServiceProvider);
    }

    public static LunaShopService lunaShopService() {
        ObjectProvider<LunaShopService> provider = lunaShopServiceProvider;
        if (provider == null) {
            return GameEventBootstrapFallbacks.lunaShopService();
        }
        return provider.getIfAvailable(GameEventBootstrapFallbacks::lunaShopService);
    }

    public static MinionService minionService() {
        ObjectProvider<MinionService> provider = minionServiceProvider;
        if (provider == null) {
            return GameEventBootstrapFallbacks.minionService();
        }
        return provider.getIfAvailable(GameEventBootstrapFallbacks::minionService);
    }

    @Override
    public void destroy() {
        lunaShopServiceProvider = null;
        minionServiceProvider = null;
        LunaShopService.setInstanceProvider(null);
        MinionService.setInstanceProvider(null);
        ShugoSweepService.setInstanceProvider(null);
        AtreianPassportService.setInstanceProvider(null);
        EventWindowService.setInstanceProvider(null);
    }
}
