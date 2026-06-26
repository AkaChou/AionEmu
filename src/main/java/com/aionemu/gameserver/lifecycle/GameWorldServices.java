package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameWorldServices implements DisposableBean {

    public GameWorldServices(ObjectProvider<GeoService> geoServiceProvider, ObjectProvider<NavService> navServiceProvider,
            ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        GeoService.setInstanceProvider(geoServiceProvider);
        NavService.setInstanceProvider(navServiceProvider);
        DropRegistrationService.setInstanceProvider(dropRegistrationServiceProvider);
    }

    @Override
    public void destroy() {
        GeoService.setInstanceProvider(null);
        NavService.setInstanceProvider(null);
        DropRegistrationService.setInstanceProvider(null);
    }
}
