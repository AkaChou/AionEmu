package com.aionemu.chatserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChatShutdownHooksTest {

    @Test
    void returnsLocalFallbackWithoutLegacySingletonAccessor() {
        ChatProcessRuntimeBridge processBridge = new ChatProcessRuntimeBridge();

        assertSame(ChatShutdownHooks.shutdownHook(processBridge), ChatShutdownHooks.shutdownHook(processBridge));
    }

    @Test
    void runtimeAndSpringConfigurationUseShutdownHookBridgeInsteadOfDirectSingleton() throws IOException, NoSuchMethodException {
        String bridgeSource = Files.readString(Path.of("src/main/java/com/aionemu/chatserver/ChatProcessRuntimeBridge.java"));
        String configurationSource = Files.readString(Path.of("src/main/java/com/aionemu/chatserver/configs/ChatServerSpringConfiguration.java"));

        assertFalse(bridgeSource.contains("ShutdownHook.getInstance("));
        assertFalse(configurationSource.contains("ShutdownHook.getInstance("));
        assertTrue(bridgeSource.contains("ChatShutdownHooks.shutdownHook(this)"));
        assertTrue(configurationSource.contains("return new ShutdownHook(processBridge, restartServiceProvider, gameServerServiceProvider);"));
        assertTrue(ShutdownHook.class.getMethod("getInstance").isAnnotationPresent(Deprecated.class));
    }
}
