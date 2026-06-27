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

    private static volatile ObjectProvider<GeoService> geoServiceProvider;
    private static volatile ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider;

    public GameWorldServices(ObjectProvider<GeoService> geoServiceProvider, ObjectProvider<NavService> navServiceProvider,
            ObjectProvider<NavData> navDataProvider,
            ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        GameWorldServices.geoServiceProvider = geoServiceProvider;
        GameWorldServices.dropRegistrationServiceProvider = dropRegistrationServiceProvider;
        GeoService.setInstanceProvider(geoServiceProvider);
        NavService.setInstanceProvider(navServiceProvider);
        NavData.setInstanceProvider(navDataProvider);
        DropRegistrationService.setInstanceProvider(dropRegistrationServiceProvider);
    }

    public static GeoService geoService() {
        ObjectProvider<GeoService> provider = geoServiceProvider;
        if (provider == null) {
            return GameWorldServiceFallbacks.geoService();
        }
        return provider.getIfAvailable(GameWorldServiceFallbacks::geoService);
    }

    public static DropRegistrationService dropRegistrationService() {
        ObjectProvider<DropRegistrationService> provider = dropRegistrationServiceProvider;
        if (provider == null) {
            return GameWorldServiceFallbacks.dropRegistrationService();
        }
        return provider.getIfAvailable(GameWorldServiceFallbacks::dropRegistrationService);
    }

    @Override
    public void destroy() {
        geoServiceProvider = null;
        dropRegistrationServiceProvider = null;
        GeoService.setInstanceProvider(null);
        NavService.setInstanceProvider(null);
        NavData.setInstanceProvider(null);
        DropRegistrationService.setInstanceProvider(null);
    }
}
