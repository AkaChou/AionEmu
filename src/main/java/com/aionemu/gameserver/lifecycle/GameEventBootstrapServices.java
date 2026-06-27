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
    private static volatile ObjectProvider<ShugoSweepService> shugoSweepServiceProvider;
    private static volatile ObjectProvider<AtreianPassportService> atreianPassportServiceProvider;
    private static volatile ObjectProvider<EventWindowService> eventWindowServiceProvider;

    public GameEventBootstrapServices(ObjectProvider<LunaShopService> lunaShopServiceProvider,
            ObjectProvider<MinionService> minionServiceProvider,
            ObjectProvider<ShugoSweepService> shugoSweepServiceProvider,
            ObjectProvider<AtreianPassportService> atreianPassportServiceProvider,
            ObjectProvider<EventWindowService> eventWindowServiceProvider) {
        GameEventBootstrapServices.lunaShopServiceProvider = lunaShopServiceProvider;
        GameEventBootstrapServices.minionServiceProvider = minionServiceProvider;
        GameEventBootstrapServices.shugoSweepServiceProvider = shugoSweepServiceProvider;
        GameEventBootstrapServices.atreianPassportServiceProvider = atreianPassportServiceProvider;
        GameEventBootstrapServices.eventWindowServiceProvider = eventWindowServiceProvider;
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

    public static ShugoSweepService shugoSweepService() {
        ObjectProvider<ShugoSweepService> provider = shugoSweepServiceProvider;
        if (provider == null) {
            return GameEventBootstrapFallbacks.shugoSweepService();
        }
        return provider.getIfAvailable(GameEventBootstrapFallbacks::shugoSweepService);
    }

    public static AtreianPassportService atreianPassportService() {
        ObjectProvider<AtreianPassportService> provider = atreianPassportServiceProvider;
        if (provider == null) {
            return GameEventBootstrapFallbacks.atreianPassportService();
        }
        return provider.getIfAvailable(GameEventBootstrapFallbacks::atreianPassportService);
    }

    public static EventWindowService eventWindowService() {
        ObjectProvider<EventWindowService> provider = eventWindowServiceProvider;
        if (provider == null) {
            return GameEventBootstrapFallbacks.eventWindowService();
        }
        return provider.getIfAvailable(GameEventBootstrapFallbacks::eventWindowService);
    }

    @Override
    public void destroy() {
        lunaShopServiceProvider = null;
        minionServiceProvider = null;
        shugoSweepServiceProvider = null;
        atreianPassportServiceProvider = null;
        eventWindowServiceProvider = null;
        LunaShopService.setInstanceProvider(null);
        MinionService.setInstanceProvider(null);
        ShugoSweepService.setInstanceProvider(null);
        AtreianPassportService.setInstanceProvider(null);
        EventWindowService.setInstanceProvider(null);
    }
}
