package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
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
            return LunaShopService.getInstance();
        }
        return lunaShopServiceProvider.getIfAvailable(LunaShopService::getInstance);
    }

    public MinionService minionService() {
        if (minionServiceProvider == null) {
            return MinionService.getInstance();
        }
        return minionServiceProvider.getIfAvailable(MinionService::getInstance);
    }

    public ShugoSweepService shugoSweepService() {
        if (shugoSweepServiceProvider == null) {
            return ShugoSweepService.getInstance();
        }
        return shugoSweepServiceProvider.getIfAvailable(ShugoSweepService::getInstance);
    }

    public AtreianPassportService atreianPassportService() {
        if (atreianPassportServiceProvider == null) {
            return AtreianPassportService.getInstance();
        }
        return atreianPassportServiceProvider.getIfAvailable(AtreianPassportService::getInstance);
    }

    public EventWindowService eventWindowService() {
        if (eventWindowServiceProvider == null) {
            return EventWindowService.getInstance();
        }
        return eventWindowServiceProvider.getIfAvailable(EventWindowService::getInstance);
    }
}
