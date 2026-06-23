package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.boot.config.AionServicesProperties;
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
    void startupFailureRunsChatShutdown() {
        System.setProperty("aion.chat.config.dir", chatConfig.toString());
        List<String> events = new ArrayList<>();
        ChatServiceLifecycle lifecycle = new ChatServiceLifecycle(
            new AionServicesProperties(),
            args -> {
                events.add("start");
                throw new IllegalStateException("chat failed");
            },
            () -> events.add("stop")
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
        ChatServiceLifecycle lifecycle = new ChatServiceLifecycle(
            new AionServicesProperties(),
            args -> events.add("start:" + args.length),
            () -> events.add("stop")
        );

        lifecycle.start(new DefaultApplicationArguments("--chat=true"));
        lifecycle.stop();
        lifecycle.stop();

        assertEquals(List.of("start:1", "stop"), events);
    }
}
