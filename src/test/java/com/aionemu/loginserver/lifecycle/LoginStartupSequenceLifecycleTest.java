package com.aionemu.loginserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.utils.AionRuntimeMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LoginStartupSequenceLifecycleTest {

    @Test
    void startRunsLoginStartupStepsInLegacyOrderAndSkipsShutdownHookInBootMode() {
        List<String> events = new ArrayList<>();
        LoginStartupSequenceLifecycle lifecycle = new LoginStartupSequenceLifecycle(
            new RecordingLoginStartupGateway(events, true)
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(null, lifecycle.getLastFailure());
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(List.of(
            "logger:init",
            "cron:init",
            "timestamp",
            "config:load",
            "database:init",
            "dao:init",
            "deadlock:start",
            "threadpool:init",
            "keygen:init",
            "gameservers:load",
            "bannedIp:start",
            "bannedMac:clean",
            "network:connect",
            "transfer:init",
            "tasks:init",
            "infos:print",
            "premium:init"
        ), events);
    }

    @Test
    void startRegistersShutdownHookOutsideBootMode() {
        List<String> events = new ArrayList<>();
        LoginStartupSequenceLifecycle lifecycle = new LoginStartupSequenceLifecycle(
            new RecordingLoginStartupGateway(events, false)
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "logger:init",
            "cron:init",
            "timestamp",
            "config:load",
            "database:init",
            "dao:init",
            "deadlock:start",
            "threadpool:init",
            "keygen:init",
            "gameservers:load",
            "bannedIp:start",
            "bannedMac:clean",
            "network:connect",
            "transfer:init",
            "tasks:init",
            "shutdownHook",
            "infos:print",
            "premium:init"
        ), events);
    }

    @Test
    void failedKeyGeneratorRecordsFailureAndAllowsRetryInBootMode() {
        List<String> events = new ArrayList<>();
        RecordingLoginStartupGateway gateway = new RecordingLoginStartupGateway(events, true);
        gateway.failKeyGeneratorOnce(new Exception("keygen failed"));
        LoginStartupSequenceLifecycle lifecycle = new LoginStartupSequenceLifecycle(gateway);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertEquals("Failed initializing Key Generator", thrown.getMessage());
        assertSame(thrown, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());
        assertEquals(List.of(
            "logger:init",
            "cron:init",
            "timestamp",
            "config:load",
            "database:init",
            "dao:init",
            "deadlock:start",
            "threadpool:init",
            "keygen:init",
            "keygen:failure"
        ), events);

        events.clear();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(null, lifecycle.getLastFailure());
        assertEquals(List.of(
            "logger:init",
            "cron:init",
            "timestamp",
            "config:load",
            "database:init",
            "dao:init",
            "deadlock:start",
            "threadpool:init",
            "keygen:init",
            "gameservers:load",
            "bannedIp:start",
            "bannedMac:clean",
            "network:connect",
            "transfer:init",
            "tasks:init",
            "infos:print",
            "premium:init"
        ), events);
    }

    @Test
    void resetAllowsStartupSequenceToRunAgain() {
        List<String> events = new ArrayList<>();
        LoginStartupSequenceLifecycle lifecycle = new LoginStartupSequenceLifecycle(
            new RecordingLoginStartupGateway(events, true)
        );

        lifecycle.start();
        lifecycle.reset();
        events.clear();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals("logger:init", events.getFirst());
    }

    @Test
    void usesStartupGatewayCollaborator() {
        assertEquals(LoginStartupGateway.class, fieldType("startupGateway"));
    }

    @Test
    void startupGatewayBridgesPlayerTransferServiceThroughSpringProvider() {
        assertEquals(ObjectProvider.class, fieldType(LoginStartupGateway.class, "playerTransferServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(LoginStartupGateway.class, "runtimeBridgeProvider"));
    }

    @Test
    void startupGatewayBridgesRuntimeStaticServicesThroughSpringProvider() throws Exception {
        String previousBootMode = System.getProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        List<String> events = new ArrayList<>();
        LoginStartupGateway gateway = new LoginStartupGateway();
        gateway.setRuntimeBridgeProvider(provider(
            LoginStartupRuntimeBridge.class,
            new RecordingLoginStartupRuntimeBridge(events)
        ));

        try {
            gateway.initializeLogger();
            gateway.initializeCronService();
            gateway.loadConfig();
            gateway.initializeDatabase();
            gateway.initializeDaos();
            gateway.startDeadlockDetector();
            gateway.initializeThreadPool();
            gateway.initializeKeyGenerator();
            gateway.loadGameServers();
            gateway.startBannedIpController();
            gateway.cleanExpiredMacBans();
            gateway.connectNetwork();
            gateway.initializeTaskManager();
            gateway.registerShutdownHook();
            gateway.printInfos();
            gateway.initializePremiumController();
            gateway.exitWithError();

            assertEquals(List.of(
                "logger:init",
                "cron:init",
                "config:load",
                "database:init",
                "dao:init",
                "deadlock:start:false",
                "threadpool:init",
                "keygen:init",
                "gameservers:load",
                "bannedIp:start",
                "bannedMac:clean",
                "network:connect",
                "tasks:init",
                "shutdownHook",
                "infos:print",
                "premium:init",
                "exit:error"
            ), events);
        } finally {
            if (previousBootMode == null) {
                System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
            } else {
                System.setProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY, previousBootMode);
            }
        }
    }

    private static Class<?> fieldType(String name) {
        return fieldType(LoginStartupSequenceLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingLoginStartupGateway extends LoginStartupGateway {

        private final List<String> events;
        private final boolean bootEmbedded;
        private long currentTimeMillis;
        private Exception keyGeneratorFailure;

        private RecordingLoginStartupGateway(List<String> events, boolean bootEmbedded) {
            this.events = events;
            this.bootEmbedded = bootEmbedded;
        }

        private void failKeyGeneratorOnce(Exception keyGeneratorFailure) {
            this.keyGeneratorFailure = keyGeneratorFailure;
        }

        @Override
        public void initializeLogger() {
            events.add("logger:init");
        }

        @Override
        public void initializeCronService() {
            events.add("cron:init");
        }

        @Override
        public void logStartupTimestamp() {
            events.add("timestamp");
        }

        @Override
        public void loadConfig() {
            events.add("config:load");
        }

        @Override
        public void initializeDatabase() {
            events.add("database:init");
        }

        @Override
        public void initializeDaos() {
            events.add("dao:init");
        }

        @Override
        public void startDeadlockDetector() {
            events.add("deadlock:start");
        }

        @Override
        public void initializeThreadPool() {
            events.add("threadpool:init");
        }

        @Override
        public void initializeKeyGenerator() throws Exception {
            events.add("keygen:init");
            if (keyGeneratorFailure != null) {
                Exception failure = keyGeneratorFailure;
                keyGeneratorFailure = null;
                throw failure;
            }
        }

        @Override
        public void logKeyGeneratorFailure(Exception e) {
            events.add("keygen:failure");
        }

        @Override
        public void loadGameServers() {
            events.add("gameservers:load");
        }

        @Override
        public void startBannedIpController() {
            events.add("bannedIp:start");
        }

        @Override
        public void cleanExpiredMacBans() {
            events.add("bannedMac:clean");
        }

        @Override
        public void connectNetwork() {
            events.add("network:connect");
        }

        @Override
        public void initializePlayerTransferService() {
            events.add("transfer:init");
        }

        @Override
        public void initializeTaskManager() {
            events.add("tasks:init");
        }

        @Override
        public boolean isBootEmbedded() {
            return bootEmbedded;
        }

        @Override
        public void registerShutdownHook() {
            events.add("shutdownHook");
        }

        @Override
        public void printInfos() {
            events.add("infos:print");
        }

        @Override
        public void initializePremiumController() {
            events.add("premium:init");
        }

        @Override
        public void exitWithError() {
            events.add("exit:error");
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeMillis++;
        }
    }

    private static final class RecordingLoginStartupRuntimeBridge extends LoginStartupRuntimeBridge {

        private final List<String> events;

        private RecordingLoginStartupRuntimeBridge(List<String> events) {
            this.events = events;
        }

        @Override
        public void initializeLogger() {
            events.add("logger:init");
        }

        @Override
        public void initializeCronService() {
            events.add("cron:init");
        }

        @Override
        public void loadConfig() {
            events.add("config:load");
        }

        @Override
        public void initializeDatabase() {
            events.add("database:init");
        }

        @Override
        public void initializeDaos() {
            events.add("dao:init");
        }

        @Override
        public void startDeadlockDetector(boolean bootEmbedded) {
            events.add("deadlock:start:" + bootEmbedded);
        }

        @Override
        public void initializeThreadPool() {
            events.add("threadpool:init");
        }

        @Override
        public void initializeKeyGenerator() {
            events.add("keygen:init");
        }

        @Override
        public void loadGameServers() {
            events.add("gameservers:load");
        }

        @Override
        public void startBannedIpController() {
            events.add("bannedIp:start");
        }

        @Override
        public void cleanExpiredMacBans() {
            events.add("bannedMac:clean");
        }

        @Override
        public void connectNetwork() {
            events.add("network:connect");
        }

        @Override
        public void initializeTaskManager() {
            events.add("tasks:init");
        }

        @Override
        public void registerShutdownHook() {
            events.add("shutdownHook");
        }

        @Override
        public void printInfos() {
            events.add("infos:print");
        }

        @Override
        public void initializePremiumController() {
            events.add("premium:init");
        }

        @Override
        public void exitWithError() {
            events.add("exit:error");
        }
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
