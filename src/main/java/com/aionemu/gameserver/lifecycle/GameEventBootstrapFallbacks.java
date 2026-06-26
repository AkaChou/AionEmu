package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;

final class GameEventBootstrapFallbacks {

    private GameEventBootstrapFallbacks() {
    }

    static LunaShopService lunaShopService() {
        return LunaShopServiceFallback.INSTANCE;
    }

    static MinionService minionService() {
        return MinionServiceFallback.INSTANCE;
    }

    static ShugoSweepService shugoSweepService() {
        return ShugoSweepServiceFallback.INSTANCE;
    }

    static AtreianPassportService atreianPassportService() {
        return AtreianPassportServiceFallback.INSTANCE;
    }

    static EventWindowService eventWindowService() {
        return EventWindowServiceFallback.INSTANCE;
    }

    private static final class LunaShopServiceFallback {
        private static final LunaShopService INSTANCE = LunaShopService.getInstance();
    }

    private static final class MinionServiceFallback {
        private static final MinionService INSTANCE = MinionService.getInstance();
    }

    private static final class ShugoSweepServiceFallback {
        private static final ShugoSweepService INSTANCE = ShugoSweepService.getInstance();
    }

    private static final class AtreianPassportServiceFallback {
        private static final AtreianPassportService INSTANCE = AtreianPassportService.getInstance();
    }

    private static final class EventWindowServiceFallback {
        private static final EventWindowService INSTANCE = EventWindowService.getInstance();
    }
}
