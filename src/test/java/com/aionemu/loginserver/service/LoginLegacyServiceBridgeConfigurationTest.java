package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.Shutdown;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class LoginLegacyServiceBridgeConfigurationTest {

    @Test
    void exposesLoginPlayerTransferServiceAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LoginLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("loginPlayerTransferService"));
            assertEquals(PlayerTransferService.class, context.getType("loginPlayerTransferService"));
            assertTrue(context.getBeanFactory().getBeanDefinition("loginPlayerTransferService").isLazyInit());
        }
    }

    @Test
    void exposesLoginShutdownAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LoginLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("loginShutdown"));
            assertEquals(Shutdown.class, context.getType("loginShutdown"));
            assertTrue(context.getBeanFactory().getBeanDefinition("loginShutdown").isLazyInit());
            assertNotSame(Shutdown.getInstance(), context.getBean(Shutdown.class));
        }
    }

    @Test
    void loginPlayerTransferBeanUsesSpringInstantiationInsteadOfSingletonFallback() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginLegacyServiceBridgeConfiguration.java"));
        String serviceSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/PlayerTransferService.java"));

        assertFalse(source.contains("PlayerTransferService.getInstance()"));
        assertTrue(source.contains("return new PlayerTransferService();"));
        assertTrue(serviceSource.contains("SingletonHolder"));
        assertTrue(serviceSource.contains("@Deprecated(since = \"boot-migration\")"));
    }
}
