package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

class GameThreadPoolLifecycleTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void threadPoolLifecycleBridgesLegacyManagerThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameThreadPoolLifecycle.class, "threadPoolManagerProvider"));
    }

    @Test
    void threadPoolSingletonAccessorUsesSpringProviderBeforeLegacyFallback() {
        ThreadPoolManager threadPoolManager = objenesis.newInstance(ThreadPoolManager.class);

        try {
            ThreadPoolManager.setInstanceProvider(provider(ThreadPoolManager.class, threadPoolManager));

            assertSame(threadPoolManager, ThreadPoolManager.getInstance());
        } finally {
            ThreadPoolManager.setInstanceProvider(null);
        }
    }

    @Test
    void gameCronRunnerUsesThreadPoolBridgeInsteadOfDirectSingleton() throws IOException {
        String cronRunnerSource = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/utils/cron/ThreadPoolManagerRunnableRunner.java"));

        assertFalse(cronRunnerSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(cronRunnerSource.contains("GameThreadPoolServices.threadPoolManager().execute(r)"));
        assertTrue(cronRunnerSource.contains("GameThreadPoolServices.threadPoolManager().executeLongRunning(r)"));
    }

    @Test
    void threadPoolServicesFailFastOutsideSpringContainerInsteadOfLocalFallback() throws IOException {
        String servicesSource = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameThreadPoolServices.java"));

        assertFalse(servicesSource.contains("ThreadPoolManager.getInstance()"));
        assertFalse(servicesSource.contains("new ThreadPoolManager()"));

        new GameThreadPoolServices(null).destroy();
        assertThrows(IllegalStateException.class, GameThreadPoolServices::threadPoolManager);
    }

    @Test
    void gameThreadPoolServicesCachesResolvedSpringManager() {
        GameThreadPoolServices services = new GameThreadPoolServices(prototypeProvider(ThreadPoolManager.class));

        try {
            assertSame(GameThreadPoolServices.threadPoolManager(), GameThreadPoolServices.threadPoolManager());
        } finally {
            services.destroy();
        }
    }

    @Test
    void gameServicesUseLifecycleSchedulerBridgesInsteadOfDirectSingletons() throws IOException {
        List<Path> sources;
        try (Stream<Path> stream = Stream.concat(
            Files.walk(Path.of("src/main/java/com/aionemu/gameserver/services")),
            Files.walk(Path.of("src/main/java/com/aionemu/gameserver/spawnengine"))
        )) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("ThreadPoolManager.getInstance()"), source.toString());
            assertFalse(content.contains("CronService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameAiUsesLifecycleSchedulerBridgeInsteadOfDirectThreadPoolSingleton() throws IOException {
        List<Path> sources;
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver/ai"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("ThreadPoolManager.getInstance()"), source.toString());
        }
    }

    @Test
    void gameInstancesUseLifecycleSchedulerBridgeInsteadOfDirectThreadPoolSingleton() throws IOException {
        List<Path> sources;
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver/instance"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("ThreadPoolManager.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesLifecycleSchedulerBridgesInsteadOfDirectSingletons() throws IOException {
        List<Path> sources;
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("ThreadPoolManager.getInstance()"), source.toString());
            assertFalse(content.contains("CronService.getInstance()"), source.toString());
        }
    }

    @Test
    void startAndStopAreIdempotent() {
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger stops = new AtomicInteger();
        GameThreadPoolLifecycle lifecycle = new RecordingGameThreadPoolLifecycle(starts, stops);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isStarted());
        assertEquals(1, starts.get());

        lifecycle.stop();
        lifecycle.stop();

        assertFalse(lifecycle.isStarted());
        assertEquals(1, stops.get());
    }

    @Test
    void stopBeforeStartDoesNotRunShutdown() {
        AtomicInteger stops = new AtomicInteger();
        GameThreadPoolLifecycle lifecycle = new RecordingGameThreadPoolLifecycle(new AtomicInteger(), stops);

        lifecycle.stop();

        assertFalse(lifecycle.isStarted());
        assertEquals(0, stops.get());
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
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
        definition.setInstanceSupplier(() -> objenesis.newInstance(type));
        beanFactory.registerBeanDefinition(type.getName(), definition);
        return beanFactory.getBeanProvider(type);
    }

    private static final class RecordingGameThreadPoolLifecycle extends GameThreadPoolLifecycle {

        private final AtomicInteger starts;
        private final AtomicInteger stops;

        private RecordingGameThreadPoolLifecycle(AtomicInteger starts, AtomicInteger stops) {
            this.starts = starts;
            this.stops = stops;
        }

        @Override
        protected void startThreadPool() {
            starts.incrementAndGet();
        }

        @Override
        protected void stopThreadPool() {
            stops.incrementAndGet();
        }
    }
}
