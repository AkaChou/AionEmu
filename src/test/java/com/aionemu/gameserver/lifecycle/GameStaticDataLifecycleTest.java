package com.aionemu.gameserver.lifecycle;

import aionemu.testkit.StubPersistenceDaos;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.services.GameLegacyServiceBridgeConfiguration;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class GameStaticDataLifecycleTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void staticDataLifecycleBridgesLegacyDataManagerAndMovementLoopsThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataLifecycle.class, "dataManagerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataLifecycle.class, "movementNotifyTaskProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataLifecycle.class, "moveTaskManagerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataLifecycle.class, "playerMoveTaskManagerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataLifecycle.class, "zoneUpdateServiceProvider"));
    }


    @Test
    void staticDataServicesXmlDataLoaderAccessorUsesSpringProviderBeforeLegacyFallback() {
        XmlDataLoader xmlDataLoader = objenesis.newInstance(XmlDataLoader.class);
        GameStaticDataServices staticDataServices = new GameStaticDataServices(
            provider(DataManager.class, objenesis.newInstance(DataManager.class)),
            provider(HTMLCache.class, objenesis.newInstance(HTMLCache.class)),
            provider(XmlDataLoader.class, xmlDataLoader)
        );

        try {
            assertSame(xmlDataLoader, GameStaticDataServices.xmlDataLoader());
            assertSame(xmlDataLoader, XmlDataLoader.getInstance());
        } finally {
            staticDataServices.destroy();
            XmlDataLoader.setInstanceProvider(null);
        }
    }

    @Test
    void dataManagerUsesStaticDataServicesBridgeForXmlDataLoader() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/dataholders/DataManager.java"));

        assertFalse(source.contains("XmlDataLoader.getInstance()"));
        assertTrue(source.contains("GameStaticDataServices.xmlDataLoader()"));
    }

    @Test
    void adminReloadUsesStaticDataServicesBridgeForXmlDataLoader() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/commands/admin/Reload.java"));

        assertFalse(source.contains("XmlDataLoader.getInstance()"));
        assertTrue(source.contains("GameStaticDataServices.xmlDataLoader()"));
    }

    @Test
    void staticDataLifecycleInitializesMovementLoopsAfterStaticData() {
        AtomicInteger loads = new AtomicInteger();
        AtomicInteger movementLoops = new AtomicInteger();
        GameStaticDataLifecycle lifecycle = new RecordingGameStaticDataLifecycle(loads, movementLoops, null);

        lifecycle.start();
        lifecycle.start();

        assertEquals(1, loads.get());
        assertEquals(1, movementLoops.get());
    }

    @Test
    void movementLoopProviderRegistrationDoesNotReadWorldMapsBeforeStaticDataLoads() {
        WorldMapsData oldWorldMapsData = DataManager.WORLD_MAPS_DATA;
        DataManager.WORLD_MAPS_DATA = null;
        try {
            assertDoesNotThrow(() -> {
                try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                    context.register(GameLegacyServiceBridgeConfiguration.class,
                            StubPersistenceDaos.class, GameMovementLoopServices.class);
                    context.refresh();
                }
            });
        } finally {
            DataManager.WORLD_MAPS_DATA = oldWorldMapsData;
        }
    }

    @Test
    void startLoadsStaticDataOnceAndRecordsLoadTime() {
        AtomicInteger loads = new AtomicInteger();
        GameStaticDataLifecycle lifecycle = new RecordingGameStaticDataLifecycle(loads, new AtomicInteger(), null);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(1, loads.get());
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        AtomicInteger loads = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("static data failed");
        GameStaticDataLifecycle lifecycle = new RecordingGameStaticDataLifecycle(loads, new AtomicInteger(), failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, loads.get());
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameStaticDataLifecycle extends GameStaticDataLifecycle {

        private final AtomicInteger loads;
        private final AtomicInteger movementLoops;
        private final RuntimeException firstFailure;

        private RecordingGameStaticDataLifecycle(AtomicInteger loads, AtomicInteger movementLoops, RuntimeException firstFailure) {
            this.loads = loads;
            this.movementLoops = movementLoops;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void loadStaticData() {
            if (loads.incrementAndGet() == 1 && firstFailure != null) {
                throw firstFailure;
            }
        }

        @Override
        protected void initializeMovementLoops() {
            movementLoops.incrementAndGet();
        }
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }
}
