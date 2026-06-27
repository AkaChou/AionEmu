package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LoginCronServicesTest {

    @Test
    void startupAndShutdownUseCronBridgeInsteadOfDirectCronSingleton() throws IOException {
        String startupBridgeSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/lifecycle/LoginStartupRuntimeBridge.java"));
        String shutdownSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/Shutdown.java"));
        String servicesSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginCronServices.java"));

        assertFalse(startupBridgeSource.contains("CronService.initSingleton"));
        assertFalse(shutdownSource.contains("CronService.getInstance().shutdown()"));
        assertFalse(shutdownSource.contains("CronService.isInitialized()"));
        assertTrue(startupBridgeSource.contains("LoginCronServices.initialize()"));
        assertTrue(shutdownSource.contains("LoginCronServices.shutdownIfInitialized()"));
        assertTrue(servicesSource.contains("CronService.initSingleton(ThreadPoolManagerRunnableRunner.class)"));
        assertTrue(servicesSource.contains("resolvedCronService = CronService.getInstance()"));
        assertTrue(servicesSource.contains("CronService cronService = resolvedCronService"));
        assertTrue(servicesSource.contains("CronService.shutdownCurrentIfInitialized()"));
        assertFalse(servicesSource.contains("CronService.getInstance().shutdown()"));
    }
}
