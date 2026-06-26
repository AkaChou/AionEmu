package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameEventBootstrapRuntimeBridge {

    private ObjectProvider<LunaShopService> lunaShopServiceProvider;
    private ObjectProvider<MinionService> minionServiceProvider;
    private ObjectProvider<ShugoSweepService> shugoSweepServiceProvider;
    private ObjectProvider<AtreianPassportService> atreianPassportServiceProvider;
    private ObjectProvider<EventWindowService> eventWindowServiceProvider;

    @Autowired(required = false)
    void setLunaShopServiceProvider(ObjectProvider<LunaShopService> lunaShopServiceProvider) {
        this.lunaShopServiceProvider = lunaShopServiceProvider;
    }

    @Autowired(required = false)
    void setMinionServiceProvider(ObjectProvider<MinionService> minionServiceProvider) {
        this.minionServiceProvider = minionServiceProvider;
    }

    @Autowired(required = false)
    void setShugoSweepServiceProvider(ObjectProvider<ShugoSweepService> shugoSweepServiceProvider) {
        this.shugoSweepServiceProvider = shugoSweepServiceProvider;
    }

    @Autowired(required = false)
    void setAtreianPassportServiceProvider(ObjectProvider<AtreianPassportService> atreianPassportServiceProvider) {
        this.atreianPassportServiceProvider = atreianPassportServiceProvider;
    }

    @Autowired(required = false)
    void setEventWindowServiceProvider(ObjectProvider<EventWindowService> eventWindowServiceProvider) {
        this.eventWindowServiceProvider = eventWindowServiceProvider;
    }

    public LunaShopService lunaShopService() {
        if (lunaShopServiceProvider == null) {
            return GameEventBootstrapFallbacks.lunaShopService();
        }
        return lunaShopServiceProvider.getIfAvailable(GameEventBootstrapFallbacks::lunaShopService);
    }

    public MinionService minionService() {
        if (minionServiceProvider == null) {
            return GameEventBootstrapFallbacks.minionService();
        }
        return minionServiceProvider.getIfAvailable(GameEventBootstrapFallbacks::minionService);
    }

    public ShugoSweepService shugoSweepService() {
        if (shugoSweepServiceProvider == null) {
            return GameEventBootstrapFallbacks.shugoSweepService();
        }
        return shugoSweepServiceProvider.getIfAvailable(GameEventBootstrapFallbacks::shugoSweepService);
    }

    public AtreianPassportService atreianPassportService() {
        if (atreianPassportServiceProvider == null) {
            return GameEventBootstrapFallbacks.atreianPassportService();
        }
        return atreianPassportServiceProvider.getIfAvailable(GameEventBootstrapFallbacks::atreianPassportService);
    }

    public EventWindowService eventWindowService() {
        if (eventWindowServiceProvider == null) {
            return GameEventBootstrapFallbacks.eventWindowService();
        }
        return eventWindowServiceProvider.getIfAvailable(GameEventBootstrapFallbacks::eventWindowService);
    }
}
