package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

class GameWorldBootstrapRuntimeBridgeTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        IDFactory idFactory = instance(IDFactory.class);
        ZoneService zoneService = instance(ZoneService.class);
        HotspotTeleportService hotspotTeleportService = instance(HotspotTeleportService.class);
        RoadService roadService = instance(RoadService.class);
        World world = instance(World.class);
        GameWorldBootstrapRuntimeBridge runtimeBridge = new GameWorldBootstrapRuntimeBridge();

        runtimeBridge.setIdFactoryProvider(provider(IDFactory.class, idFactory));
        runtimeBridge.setZoneServiceProvider(provider(ZoneService.class, zoneService));
        runtimeBridge.setHotspotTeleportServiceProvider(provider(HotspotTeleportService.class, hotspotTeleportService));
        runtimeBridge.setRoadServiceProvider(provider(RoadService.class, roadService));
        runtimeBridge.setWorldProvider(provider(World.class, world));

        assertSame(idFactory, runtimeBridge.idFactory());
        assertSame(zoneService, runtimeBridge.zoneService());
        assertSame(hotspotTeleportService, runtimeBridge.hotspotTeleportService());
        assertSame(roadService, runtimeBridge.roadService());
        assertSame(world, runtimeBridge.world());
    }

    @Test
    void idFactorySingletonAccessorUsesSpringProviderBeforeLegacyFallback() {
        IDFactory idFactory = instance(IDFactory.class);

        try {
            IDFactory.setInstanceProvider(provider(IDFactory.class, idFactory));

            assertSame(idFactory, IDFactory.getInstance());
        } finally {
            IDFactory.setInstanceProvider(null);
        }
    }

    @Test
    void worldBootstrapServicesIdFactoryAccessorUsesSpringProviderBeforeLegacyFallback() {
        IDFactory idFactory = instance(IDFactory.class);
        ZoneService zoneService = instance(ZoneService.class);
        HotspotTeleportService hotspotTeleportService = instance(HotspotTeleportService.class);
        World world = instance(World.class);
        GameWorldBootstrapServices worldBootstrapServices = new GameWorldBootstrapServices(
            provider(IDFactory.class, idFactory),
            provider(ZoneService.class, zoneService),
            provider(HotspotTeleportService.class, hotspotTeleportService),
            provider(RoadService.class, instance(RoadService.class)),
            provider(World.class, world)
        );

        try {
            assertSame(idFactory, GameWorldBootstrapServices.idFactory());
            assertSame(zoneService, GameWorldBootstrapServices.zoneService());
            assertSame(hotspotTeleportService, GameWorldBootstrapServices.hotspotTeleportService());
            assertSame(world, GameWorldBootstrapServices.world());
        } finally {
            worldBootstrapServices.destroy();
        }
    }

    @Test
    void worldBootstrapServicesCacheResolvedSpringServices() {
        GameWorldBootstrapServices services = new GameWorldBootstrapServices(
            prototypeProvider(IDFactory.class),
            prototypeProvider(ZoneService.class),
            prototypeProvider(HotspotTeleportService.class),
            prototypeProvider(RoadService.class),
            prototypeProvider(World.class)
        );

        try {
            assertSame(GameWorldBootstrapServices.idFactory(), GameWorldBootstrapServices.idFactory());
            assertSame(GameWorldBootstrapServices.zoneService(), GameWorldBootstrapServices.zoneService());
            assertSame(GameWorldBootstrapServices.hotspotTeleportService(), GameWorldBootstrapServices.hotspotTeleportService());
            assertSame(GameWorldBootstrapServices.world(), GameWorldBootstrapServices.world());
        } finally {
            services.destroy();
        }
    }

    @Test
    void gameServerCodeUsesWorldBootstrapIdFactoryBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("world/zone/ZoneService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameWorldBootstrapFallbacks.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameWorldBootstrapServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("IDFactory.getInstance()"), source.toString());
            assertFalse(content.contains("ZoneService.getInstance()"), source.toString());
            assertFalse(content.contains("HotspotTeleportService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesWorldBootstrapBridgeInsteadOfDirectWorldSingleton() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            for (Path source : stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("world/World.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameWorldBootstrapFallbacks.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameWorldBootstrapServices.java")))
                .toList()) {
                String content = Files.readString(source);

                assertFalse(content.contains("World.getInstance()"), source.toString());
            }
        }
    }

    @Test
    void worldSingletonAccessorUsesSpringProviderBeforeLegacyFallback() {
        World world = instance(World.class);

        try {
            World.setInstanceProvider(provider(World.class, world));

            assertSame(world, World.getInstance());
        } finally {
            World.setInstanceProvider(null);
        }
    }

    @Test
    void runtimeBridgeDoesNotCallLegacySingletonsDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameWorldBootstrapRuntimeBridge.java"));

        assertFalse(source.contains("IDFactory.getInstance()"));
        assertFalse(source.contains("ZoneService.getInstance()"));
        assertFalse(source.contains("HotspotTeleportService.getInstance()"));
        assertFalse(source.contains("RoadService.getInstance()"));
        assertFalse(source.contains("World.getInstance()"));
    }

    private <T> T instance(Class<T> type) {
        return objenesis.newInstance(type);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }

    private <T> ObjectProvider<T> prototypeProvider(Class<T> type) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        RootBeanDefinition definition = new RootBeanDefinition(type);
        definition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
        definition.setInstanceSupplier(() -> instance(type));
        beanFactory.registerBeanDefinition(type.getName(), definition);
        return beanFactory.getBeanProvider(type);
    }
}
