package com.aionemu.chatserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ShutdownHook;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class ChatRestartRequestTest {

    @AfterEach
    void resetEmbeddedMode() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedShutdownHandler.clear();
        ShutdownHook.setRestartOnly(false);
        ChatRestartRequest.setProcessBridgeProvider(null);
    }

    @Test
    void embeddedRestartRequestsBootShutdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        ChatRestartRequest.requestRestart();

        assertEquals(AionEmbeddedShutdownMode.RESTART, requestedMode.get());
    }

    @Test
    void directRestartStartsProcessShutdownHookOutsideBoot() {
        List<String> events = new ArrayList<>();

        ChatRestartRequest.requestRestart(new RecordingChatProcessRuntimeBridge(events));

        assertEquals(List.of("hook:start"), events);
    }

    @Test
    void configuredProcessBridgeIsUsedForRestartOutsideBoot() {
        List<String> events = new ArrayList<>();
        ChatRestartRequest.setProcessBridgeProvider(provider(new RecordingChatProcessRuntimeBridge(events)));

        ChatRestartRequest.requestRestart();

        assertEquals(List.of("hook:start"), events);
    }

    @Test
    void embeddedRestartFallsBackToProcessShutdownWhenBootHandlerIsMissing() {
        AionRuntimeMode.enableBootEmbeddedMode();
        List<String> events = new ArrayList<>();

        ChatRestartRequest.requestRestart(new RecordingChatProcessRuntimeBridge(events));

        assertEquals(List.of("shutdown:false"), events);
    }

    private static final class RecordingChatProcessRuntimeBridge extends ChatProcessRuntimeBridge {

        private final List<String> events;

        private RecordingChatProcessRuntimeBridge(List<String> events) {
            this.events = events;
        }

        @Override
        public Thread shutdownHook() {
            return new Thread() {
                @Override
                public synchronized void start() {
                    events.add("hook:start");
                }
            };
        }

        @Override
        public void shutdown(boolean restart) {
            events.add("shutdown:" + restart);
        }
    }

    private static org.springframework.beans.factory.ObjectProvider<ChatProcessRuntimeBridge> provider(ChatProcessRuntimeBridge instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(ChatProcessRuntimeBridge.class.getName(), instance);
        return beanFactory.getBeanProvider(ChatProcessRuntimeBridge.class);
    }
}
