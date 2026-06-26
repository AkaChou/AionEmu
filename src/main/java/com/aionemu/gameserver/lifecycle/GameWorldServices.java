package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavData;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameWorldServices implements DisposableBean {

    public GameWorldServices(ObjectProvider<GeoService> geoServiceProvider, ObjectProvider<NavService> navServiceProvider,
            ObjectProvider<NavData> navDataProvider,
            ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        GeoService.setInstanceProvider(geoServiceProvider);
        NavService.setInstanceProvider(navServiceProvider);
        NavData.setInstanceProvider(navDataProvider);
        DropRegistrationService.setInstanceProvider(dropRegistrationServiceProvider);
    }

    @Override
    public void destroy() {
        GeoService.setInstanceProvider(null);
        NavService.setInstanceProvider(null);
        NavData.setInstanceProvider(null);
        DropRegistrationService.setInstanceProvider(null);
    }
}
