package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.boot.config.AionServicesProperties;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
    }

    @Test
    void startupFailureRunsChatShutdown() {
        System.setProperty("aion.chat.config.dir", chatConfig.toString());
        List<String> events = new ArrayList<>();
        ChatServerLifecycleGateway gateway = new RecordingChatServerLifecycleGateway(events, true);
        ChatServiceLifecycle lifecycle = new ChatServiceLifecycle(
            new AionServicesProperties(),
            gateway
        );

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> lifecycle.start(new DefaultApplicationArguments())
        );

        assertEquals("chat failed", thrown.getMessage());
        assertEquals(List.of("start", "stop"), events);

        lifecycle.stop();

        assertEquals(List.of("start", "stop"), events);
    }

    @Test
    void stopRunsOnceAfterSuccessfulStartup() {
        System.setProperty("aion.chat.config.dir", chatConfig.toString());
        List<String> events = new ArrayList<>();
        ChatServerLifecycleGateway gateway = new RecordingChatServerLifecycleGateway(events, false);
        ChatServiceLifecycle lifecycle = new ChatServiceLifecycle(
            new AionServicesProperties(),
            gateway
        );

        lifecycle.start(new DefaultApplicationArguments("--chat=true"));
        lifecycle.stop();
        lifecycle.stop();

        assertEquals(List.of("start:1", "stop"), events);
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

    private static Class<?> fieldType(String name) {
        try {
            return ChatServiceLifecycle.class.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static Class<?> findFieldType(String name) {
        try {
            Field field = ChatServiceLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
