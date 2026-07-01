package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class GameStaticDataLifecycleTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void usesStaticDataGatewayCollaborator() {
        assertEquals(GameStaticDataGateway.class, fieldType("staticDataGateway"));
    }

    @Test
    void staticDataGatewayBridgesLegacyDataManagerThroughSpringProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataGateway.class, "dataManagerProvider"));
    }

    @Test
    void staticDataGatewayBridgesLegacyFallbackThroughRuntimeBridgeProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataGateway.class, "runtimeBridgeProvider"));
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
    void staticDataLifecycleInitializesMovementLoopsAfterStaticData() {
        AtomicInteger loads = new AtomicInteger();
        AtomicInteger movementLoops = new AtomicInteger();
        GameStaticDataLifecycle lifecycle = new GameStaticDataLifecycle(new RecordingGameStaticDataGateway(loads, null));
        lifecycle.setMovementLoopGatewayProvider(provider(GameMovementLoopGateway.class, new RecordingGameMovementLoopGateway(movementLoops)));

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
                    context.register(GameLegacyServiceBridgeConfiguration.class, GameMovementLoopServices.class);
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
        GameStaticDataLifecycle lifecycle = new GameStaticDataLifecycle(new RecordingGameStaticDataGateway(loads, null));
        lifecycle.setMovementLoopGatewayProvider(provider(GameMovementLoopGateway.class, new RecordingGameMovementLoopGateway(new AtomicInteger())));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(1, loads.get());
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        AtomicInteger loads = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("static data failed");
        GameStaticDataLifecycle lifecycle = new GameStaticDataLifecycle(new RecordingGameStaticDataGateway(loads, failure));
        lifecycle.setMovementLoopGatewayProvider(provider(GameMovementLoopGateway.class, new RecordingGameMovementLoopGateway(new AtomicInteger())));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, loads.get());
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static final class RecordingGameStaticDataGateway extends GameStaticDataGateway {

        private final AtomicInteger loads;
        private final RuntimeException firstFailure;

        private RecordingGameStaticDataGateway(AtomicInteger loads, RuntimeException firstFailure) {
            this.loads = loads;
            this.firstFailure = firstFailure;
        }

        @Override
        public void load() {
            if (loads.incrementAndGet() == 1 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }

    private static final class RecordingGameMovementLoopGateway extends GameMovementLoopGateway {

        private final AtomicInteger starts;

        private RecordingGameMovementLoopGateway(AtomicInteger starts) {
            this.starts = starts;
        }

        @Override
        public void initialize() {
            starts.incrementAndGet();
        }
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameStaticDataLifecycle.class, name);
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
