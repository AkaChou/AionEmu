package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyChatConfigOverrides;
import com.aionemu.boot.config.LegacyChatProperties;
import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ChatServerRuntime;
import com.aionemu.chatserver.ChatServerStartupBridge;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.DefaultApplicationArguments;

class ChatServiceLifecycleTest {

    @TempDir
    Path chatConfig;

    @AfterEach
    void clearProperties() {
        System.clearProperty("aion.chat.config.dir");
    }

    @Test
    void usesChatServerLifecycleGatewayInsteadOfActionAdapters() {
        assertEquals(ChatServerLifecycleGateway.class, fieldType("chatServerLifecycleGateway"));
        assertEquals(null, findFieldType("startAction"));
        assertEquals(null, findFieldType("stopAction"));
        assertEquals(ObjectProvider.class, fieldType(ChatServerLifecycleGateway.class, "chatServerRuntimeProvider"));
        assertEquals(ObjectProvider.class, fieldType(ChatServerLifecycleGateway.class, "runtimeBridgeProvider"));
        assertEquals(ObjectProvider.class, fieldType(ChatServerRuntimeBridge.class, "chatServerRuntimeProvider"));
        assertEquals(ObjectProvider.class, fieldType(ChatServerRuntimeBridge.class, "processBridgeProvider"));
        assertEquals(null, findFieldType(ChatServerLifecycleGateway.class, "chatServerRuntime"));
    }

    @Test
    void startupFailureRunsChatShutdown() {
        System.setProperty("aion.chat.config.dir", chatConfig.toString());
        List<String> events = new ArrayList<>();
        ChatServerLifecycleGateway gateway = new RecordingChatServerLifecycleGateway(events, true);
        ChatServiceLifecycle lifecycle = new ChatServiceLifecycle(
            new AionServicesProperties(),
            new RecordingLegacyChatConfigOverrides(events),
            gateway
        );

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> lifecycle.start(new DefaultApplicationArguments())
        );

        assertEquals("chat failed", thrown.getMessage());
        assertEquals(List.of("apply", "start", "stop"), events);

        lifecycle.stop();

        assertEquals(List.of("apply", "start", "stop"), events);
    }

    @Test
    void stopRunsOnceAfterSuccessfulStartup() {
        System.setProperty("aion.chat.config.dir", chatConfig.toString());
        List<String> events = new ArrayList<>();
        ChatServerLifecycleGateway gateway = new RecordingChatServerLifecycleGateway(events, false);
        ChatServiceLifecycle lifecycle = new ChatServiceLifecycle(
            new AionServicesProperties(),
            new RecordingLegacyChatConfigOverrides(events),
            gateway
        );

        lifecycle.start(new DefaultApplicationArguments("--chat=true"));
        lifecycle.stop();
        lifecycle.stop();

        assertEquals(List.of("apply", "start:1", "stop"), events);
    }

    @Test
    void startAppliesLegacyConfigOverridesBeforeStartingChatServer() {
        System.setProperty("aion.chat.config.dir", chatConfig.toString());
        List<String> events = new ArrayList<>();
        ChatServiceLifecycle lifecycle = new ChatServiceLifecycle(
            new AionServicesProperties(),
            new RecordingLegacyChatConfigOverrides(events),
            new RecordingChatServerLifecycleGateway(events, false)
        );

        lifecycle.start(new DefaultApplicationArguments("--chat=true"));

        assertEquals(List.of("apply", "start:1"), events);
    }

    @Test
    void chatGatewayUsesRuntimeProviderWhenAvailableAndShutdownBridge() {
        List<String> events = new ArrayList<>();
        ChatServerLifecycleGateway gateway = new ChatServerLifecycleGateway();
        gateway.setChatServerRuntimeProvider(provider(
            ChatServerRuntime.class,
            new RecordingChatServerRuntime(events)
        ));
        gateway.setRuntimeBridgeProvider(provider(
            ChatServerRuntimeBridge.class,
            new RecordingChatServerRuntimeBridge(events)
        ));

        gateway.start(new String[] {"--chat=true"});
        gateway.stop();

        assertEquals(List.of("runtime:start:1", "shutdown:false"), events);
    }

    @Test
    void chatGatewayUsesRuntimeBridgeWhenChatRuntimeBeanIsUnavailable() {
        List<String> events = new ArrayList<>();
        ChatServerLifecycleGateway gateway = new ChatServerLifecycleGateway();
        gateway.setChatServerRuntimeProvider(emptyProvider(ChatServerRuntime.class));
        gateway.setRuntimeBridgeProvider(provider(
            ChatServerRuntimeBridge.class,
            new RecordingChatServerRuntimeBridge(events)
        ));

        gateway.start(new String[] {"--chat=true"});
        gateway.stop();

        assertEquals(List.of("bridge:start:1", "shutdown:false"), events);
    }

    @Test
    void chatRuntimeBridgeUsesRuntimeProviderWhenAvailable() {
        List<String> events = new ArrayList<>();
        ChatServerRuntimeBridge runtimeBridge = new ChatServerRuntimeBridge();
        runtimeBridge.setChatServerRuntimeProvider(provider(
            ChatServerRuntime.class,
            new RecordingChatServerRuntime(events)
        ));

        runtimeBridge.start(new String[] {"--chat=true"});

        assertEquals(List.of("runtime:start:1"), events);
    }

    @Test
    void chatRuntimeBridgeRoutesShutdownThroughProcessBridgeProvider() {
        List<String> events = new ArrayList<>();
        ChatServerRuntimeBridge runtimeBridge = new ChatServerRuntimeBridge();
        runtimeBridge.setProcessBridgeProvider(provider(
            ChatProcessRuntimeBridge.class,
            new RecordingChatProcessRuntimeBridge(events)
        ));

        runtimeBridge.shutdown(true);

        assertEquals(List.of("process:shutdown:true"), events);
    }

    @Test
    void chatRuntimeUsesStartupBridgeProvider() {
        List<String> events = new ArrayList<>();
        ChatServerRuntime runtime = new ChatServerRuntime(
            null,
            null,
            null,
            null,
            null,
            null,
            provider(ChatServerStartupBridge.class, new RecordingChatServerStartupBridge(events))
        );

        runtime.startupBridge().initializeLogger();
        runtime.startupBridge().loadConfig();
        runtime.startupBridge().printInfos();
        runtime.startupBridge().registerShutdownHook();

        assertEquals(List.of("logger:init", "config:load", "infos:print", "shutdownHook"), events);
    }

    private static final class RecordingChatServerLifecycleGateway extends ChatServerLifecycleGateway {

        private final List<String> events;
        private final boolean failOnStart;

        private RecordingChatServerLifecycleGateway(List<String> events, boolean failOnStart) {
            this.events = events;
            this.failOnStart = failOnStart;
        }

        @Override
        public void start(String[] args) {
            events.add(failOnStart ? "start" : "start:" + args.length);
            if (failOnStart) {
                throw new IllegalStateException("chat failed");
            }
        }

        @Override
        public void stop() {
            events.add("stop");
        }
    }

    private static final class RecordingLegacyChatConfigOverrides extends LegacyChatConfigOverrides {

        private final List<String> events;

        private RecordingLegacyChatConfigOverrides(List<String> events) {
            super(new LegacyChatProperties());
            this.events = events;
        }

        @Override
        public void applyToChatConfig() {
            events.add("apply");
        }
    }

    private static final class RecordingChatServerRuntime extends ChatServerRuntime {

        private final List<String> events;

        private RecordingChatServerRuntime(List<String> events) {
            super(null, null, null, null, null, null, null);
            this.events = events;
        }

        @Override
        public void start(String[] args) {
            events.add("runtime:start:" + args.length);
        }
    }

    private static final class RecordingChatServerStartupBridge extends ChatServerStartupBridge {

        private final List<String> events;

        private RecordingChatServerStartupBridge(List<String> events) {
            this.events = events;
        }

        @Override
        public void initializeLogger() {
            events.add("logger:init");
        }

        @Override
        public void loadConfig() {
            events.add("config:load");
        }

        @Override
        public void printInfos() {
            events.add("infos:print");
        }

        @Override
        public void registerShutdownHook() {
            events.add("shutdownHook");
        }
    }

    private static final class RecordingChatProcessRuntimeBridge extends ChatProcessRuntimeBridge {

        private final List<String> events;

        private RecordingChatProcessRuntimeBridge(List<String> events) {
            this.events = events;
        }

        @Override
        public void shutdown(boolean restart) {
            events.add("process:shutdown:" + restart);
        }
    }

    private static final class RecordingChatServerRuntimeBridge extends ChatServerRuntimeBridge {

        private final List<String> events;

        private RecordingChatServerRuntimeBridge(List<String> events) {
            this.events = events;
        }

        @Override
        public void start(String[] args) {
            events.add("bridge:start:" + args.length);
        }

        @Override
        public void shutdown(boolean restart) {
            events.add("shutdown:" + restart);
        }
    }

    private static Class<?> fieldType(String name) {
        return fieldType(ChatServiceLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static Class<?> findFieldType(String name) {
        return findFieldType(ChatServiceLifecycle.class, name);
    }

    private static Class<?> findFieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }

    private static <T> ObjectProvider<T> emptyProvider(Class<T> type) {
        return new DefaultListableBeanFactory().getBeanProvider(type);
    }
}
