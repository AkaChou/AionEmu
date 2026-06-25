package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameWorldBootstrapRuntimeBridge {

    public IDFactory idFactory() {
        return IDFactory.getInstance();
    }

    public ZoneService zoneService() {
        return ZoneService.getInstance();
    }

    public HotspotTeleportService hotspotTeleportService() {
        return HotspotTeleportService.getInstance();
    }

    public RoadService roadService() {
        return RoadService.getInstance();
    }

    public World world() {
        return World.getInstance();
    }
}
