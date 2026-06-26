package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;

final class GameWorldServiceFallbacks {

    private GameWorldServiceFallbacks() {
    }

    static GeoService geoService() {
        return GeoServiceFallback.INSTANCE;
    }

    static NavService navService() {
        return NavServiceFallback.INSTANCE;
    }

    static DropRegistrationService dropRegistrationService() {
        return DropRegistrationServiceFallback.INSTANCE;
    }

    private static final class GeoServiceFallback {
        private static final GeoService INSTANCE = GeoService.getInstance();
    }

    private static final class NavServiceFallback {
        private static final NavService INSTANCE = NavService.getInstance();
    }

    private static final class DropRegistrationServiceFallback {
        private static final DropRegistrationService INSTANCE = DropRegistrationService.getInstance();
    }
}
