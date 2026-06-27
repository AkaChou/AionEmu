package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavData;
import com.aionemu.gameserver.world.geo.nav.NavService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void runtimeBridgeDoesNotCallLegacySingletonsDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameWorldServicesRuntimeBridge.java"));

        assertFalse(source.contains("GeoService.getInstance()"));
        assertFalse(source.contains("NavService.getInstance()"));
        assertFalse(source.contains("DropRegistrationService.getInstance()"));
    }

    @Test
    void worldServicesDropRegistrationAccessorUsesSpringProviderBeforeLegacyFallback() {
        DropRegistrationService dropRegistrationService = instance(DropRegistrationService.class);
        GameWorldServices worldServices = new GameWorldServices(
            provider(GeoService.class, instance(GeoService.class)),
            provider(NavService.class, instance(NavService.class)),
            provider(NavData.class, instance(NavData.class)),
            provider(DropRegistrationService.class, dropRegistrationService)
        );

        try {
            assertSame(dropRegistrationService, GameWorldServices.dropRegistrationService());
        } finally {
            worldServices.destroy();
        }
    }

    @Test
    void dropServiceUsesWorldServicesBridgeForDropRegistration() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/drop/DropService.java"));

        assertFalse(source.contains("DropRegistrationService.getInstance()"));
        assertTrue(source.contains("GameWorldServices.dropRegistrationService()"));
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
