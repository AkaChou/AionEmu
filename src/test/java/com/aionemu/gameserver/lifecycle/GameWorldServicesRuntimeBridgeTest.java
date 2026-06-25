package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class GameWorldServicesRuntimeBridgeTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        GeoService geoService = instance(GeoService.class);
        NavService navService = instance(NavService.class);
        DropRegistrationService dropRegistrationService = instance(DropRegistrationService.class);
        GameWorldServicesRuntimeBridge runtimeBridge = new GameWorldServicesRuntimeBridge();

        runtimeBridge.setGeoServiceProvider(provider(GeoService.class, geoService));
        runtimeBridge.setNavServiceProvider(provider(NavService.class, navService));
        runtimeBridge.setDropRegistrationServiceProvider(provider(DropRegistrationService.class, dropRegistrationService));

        assertSame(geoService, runtimeBridge.geoService());
        assertSame(navService, runtimeBridge.navService());
        assertSame(dropRegistrationService, runtimeBridge.dropRegistrationService());
    }

    private <T> T instance(Class<T> type) {
        return objenesis.newInstance(type);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
