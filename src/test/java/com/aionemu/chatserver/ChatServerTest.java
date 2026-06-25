package com.aionemu.chatserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class ChatServerTest {

    @Test
    void startUsesStartupBridgeAndDependenciesInLegacyOrder() {
        List<String> events = new ArrayList<>();

        ChatServer.start(new String[] {"--chat=true"}, new RecordingChatServerDependencies(events, true));

        assertEquals(List.of(
            "time",
            "logger:init",
            "config:load",
            "infos:print",
            "idFactory",
            "gameServerService",
            "broadcastService",
            "chatService",
            "nettyServer",
            "restartService",
            "bootEmbedded",
            "time"
        ), events);
    }

    @Test
    void startRegistersShutdownHookOutsideBootMode() {
        List<String> events = new ArrayList<>();

        ChatServer.start(new String[] {"--chat=true"}, new RecordingChatServerDependencies(events, false));

        assertEquals(List.of(
            "time",
            "logger:init",
            "config:load",
            "infos:print",
            "idFactory",
            "gameServerService",
            "broadcastService",
            "chatService",
            "nettyServer",
            "restartService",
            "bootEmbedded",
            "shutdownHook",
            "time"
        ), events);
    }

    @Test
    void startupBridgeBridgesProcessCallsThroughSpringProvider() {
        List<String> events = new ArrayList<>();
        ChatServerStartupBridge startupBridge = new ChatServerStartupBridge();
        startupBridge.setProcessBridgeProvider(provider(
            ChatProcessRuntimeBridge.class,
            new RecordingChatProcessRuntimeBridge(events)
        ));

        startupBridge.registerShutdownHook();

        assertEquals(List.of("shutdownHook:get", "shutdownHook:register"), events);
    }

    @Test
    void shutdownHookDelegatesJvmHaltToProcessBridge() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/chatserver/ShutdownHook.java"));

        assertFalse(source.contains("Runtime.getRuntime().halt"));
        assertTrue(source.contains("processBridge.halt(ExitCode.CODE_RESTART)"));
        assertTrue(source.contains("processBridge.halt(ExitCode.CODE_NORMAL)"));
    }

    private static final class RecordingChatServerDependencies implements ChatServerDependencies {

        private final List<String> events;
        private final ChatServerStartupBridge startupBridge;

        private RecordingChatServerDependencies(List<String> events, boolean bootEmbedded) {
            this.events = events;
            this.startupBridge = new RecordingChatServerStartupBridge(events, bootEmbedded);
        }

        @Override
        public ChatServerStartupBridge startupBridge() {
            return startupBridge;
        }

        @Override
        public IdFactory idFactory() {
            events.add("idFactory");
            return null;
        }

        @Override
        public GameServerService gameServerService() {
            events.add("gameServerService");
            return null;
        }

        @Override
        public BroadcastService broadcastService() {
            events.add("broadcastService");
            return null;
        }

        @Override
        public ChatService chatService() {
            events.add("chatService");
            return null;
        }

        @Override
        public NettyServer nettyServer() {
            events.add("nettyServer");
            return null;
        }

        @Override
        public RestartService restartService() {
            events.add("restartService");
            return null;
        }
    }

    private static final class RecordingChatServerStartupBridge extends ChatServerStartupBridge {

        private final List<String> events;
        private final boolean bootEmbedded;

        private RecordingChatServerStartupBridge(List<String> events, boolean bootEmbedded) {
            this.events = events;
            this.bootEmbedded = bootEmbedded;
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
        public boolean isBootEmbedded() {
            events.add("bootEmbedded");
            return bootEmbedded;
        }

        @Override
        public void registerShutdownHook() {
            events.add("shutdownHook");
        }

        @Override
        public long currentTimeMillis() {
            events.add("time");
            return events.size();
        }
    }

    private static final class RecordingChatProcessRuntimeBridge extends ChatProcessRuntimeBridge {

        private final List<String> events;

        private RecordingChatProcessRuntimeBridge(List<String> events) {
            this.events = events;
        }

        @Override
        public Thread shutdownHook() {
            events.add("shutdownHook:get");
            return new Thread("recording-chat-shutdown");
        }

        @Override
        public void registerShutdownHook(Thread shutdownHook) {
            events.add("shutdownHook:register");
        }
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
