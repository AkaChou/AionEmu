package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;

final class GameWorldBootstrapFallbacks {

    private GameWorldBootstrapFallbacks() {
    }

    static IDFactory idFactory() {
        return IdFactoryFallback.INSTANCE;
    }

    static ZoneService zoneService() {
        return ZoneServiceFallback.INSTANCE;
    }

    static HotspotTeleportService hotspotTeleportService() {
        return HotspotTeleportServiceFallback.INSTANCE;
    }

    static RoadService roadService() {
        return RoadServiceFallback.INSTANCE;
    }

    static World world() {
        return WorldFallback.INSTANCE;
    }

    private static final class IdFactoryFallback {
        private static final IDFactory INSTANCE = IDFactory.getInstance();
    }

    private static final class ZoneServiceFallback {
        private static final ZoneService INSTANCE = ZoneService.getInstance();
    }

    private static final class HotspotTeleportServiceFallback {
        private static final HotspotTeleportService INSTANCE = HotspotTeleportService.getInstance();
    }

    private static final class RoadServiceFallback {
        private static final RoadService INSTANCE = RoadService.getInstance();
    }

    private static final class WorldFallback {
        private static final World INSTANCE = World.getInstance();
    }
}
