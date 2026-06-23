package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.springframework.stereotype.Component;

@Component
public class GameWorldBootstrapGateway {

    public void bootstrap() {
        Util.printSection(" *** IDFactory *** ");
        IDFactory.getInstance();
        Util.printSection(" *** Zone *** ");
        ZoneService.getInstance().load(null);
        HotspotTeleportService.getInstance();
        RoadService.getInstance();
        World.getInstance();
    }
}
