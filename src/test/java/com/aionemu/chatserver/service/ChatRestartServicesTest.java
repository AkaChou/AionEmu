package com.aionemu.chatserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.chatserver.configs.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class ChatRestartServicesTest {

    private String restartFrequency;
    private String restartTime;

    @BeforeEach
    void forceRestartDisabled() {
        restartFrequency = Config.CHATSERVER_RESTART_FREQUENCY;
        restartTime = Config.CHATSERVER_RESTART_TIME;
        Config.CHATSERVER_RESTART_FREQUENCY = "NEVER";
        Config.CHATSERVER_RESTART_TIME = "5:00";
    }

    @AfterEach
    void restoreConfig() {
        Config.CHATSERVER_RESTART_FREQUENCY = restartFrequency;
        Config.CHATSERVER_RESTART_TIME = restartTime;
    }

    @Test
    void usesSpringProviderBeforeLocalFallback() {
        RestartService restartService = new RestartService();
        ChatRestartServices services = new ChatRestartServices(provider(RestartService.class, restartService));

        try {
            assertSame(restartService, ChatRestartServices.restartService());
        } finally {
            restartService.shutdown();
            services.destroy();
        }
    }

    @Test
    void legacyAndShutdownPathsUseRestartBridgeInsteadOfDirectSingleton() throws IOException {
        String dependenciesSource = Files.readString(Path.of("src/main/java/com/aionemu/chatserver/ChatServerLegacyDependencies.java"));
        String shutdownHookSource = Files.readString(Path.of("src/main/java/com/aionemu/chatserver/ShutdownHook.java"));

        assertFalse(dependenciesSource.contains("RestartService.getInstance()"));
        assertFalse(shutdownHookSource.contains("return RestartService.getInstance();"));
        assertTrue(dependenciesSource.contains("ChatRestartServices.restartService()"));
        assertTrue(shutdownHookSource.contains("ChatRestartServices.restartService()"));
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
