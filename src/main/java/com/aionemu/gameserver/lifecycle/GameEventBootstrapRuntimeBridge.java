package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameEventBootstrapRuntimeBridge {

    public LunaShopService lunaShopService() {
        return LunaShopService.getInstance();
    }

    public MinionService minionService() {
        return MinionService.getInstance();
    }

    public ShugoSweepService shugoSweepService() {
        return ShugoSweepService.getInstance();
    }

    public AtreianPassportService atreianPassportService() {
        return AtreianPassportService.getInstance();
    }

    public EventWindowService eventWindowService() {
        return EventWindowService.getInstance();
    }
}
