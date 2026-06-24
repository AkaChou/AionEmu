package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.springframework.stereotype.Component;

@Component
public class GameGeoNavGateway {

    public void initialize() {
        Util.printSection(" *** Geodata *** ");
        GeoService.getInstance().initializeGeo();
        NavService.getInstance().initializeNav();
    }
}
