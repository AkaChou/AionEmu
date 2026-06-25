package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.controller.BannedMacManager;
import com.aionemu.loginserver.controller.PremiumController;
import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
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
    void exposesLoginPremiumControllerAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LoginLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("loginPremiumController"));
            assertEquals(PremiumController.class, context.getType("loginPremiumController"));
            assertTrue(context.getBeanFactory().getBeanDefinition("loginPremiumController").isLazyInit());
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
    void exposesLoginTaskFromDBManagerAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LoginLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("loginTaskFromDBManager"));
            assertEquals(TaskFromDBManager.class, context.getType("loginTaskFromDBManager"));
            assertTrue(context.getBeanFactory().getBeanDefinition("loginTaskFromDBManager").isLazyInit());
        }
    }

    @Test
    void exposesLoginBannedMacManagerAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LoginLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("loginBannedMacManager"));
            assertEquals(BannedMacManager.class, context.getType("loginBannedMacManager"));
            assertTrue(context.getBeanFactory().getBeanDefinition("loginBannedMacManager").isLazyInit());
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

    @Test
    void loginPremiumControllerBeanUsesSpringInstantiationInsteadOfSingletonFallback() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginLegacyServiceBridgeConfiguration.java"));
        String controllerSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/controller/PremiumController.java"));

        assertFalse(source.contains("PremiumController.getController()"));
        assertTrue(source.contains("return new PremiumController();"));
        assertTrue(controllerSource.contains("SingletonHolder"));
        assertTrue(controllerSource.contains("@Deprecated(since = \"boot-migration\")"));
    }

    @Test
    void loginTaskFromDBManagerBeanUsesSpringInstantiationInsteadOfSingletonFallback() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginLegacyServiceBridgeConfiguration.java"));
        String managerSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/taskmanager/TaskFromDBManager.java"));

        assertFalse(source.contains("TaskFromDBManager.getInstance()"));
        assertTrue(source.contains("return new TaskFromDBManager();"));
        assertTrue(managerSource.contains("SingletonHolder"));
        assertTrue(managerSource.contains("@Deprecated(since = \"boot-migration\")"));
    }

    @Test
    void loginBannedMacManagerBeanUsesSpringInstantiationInsteadOfSingletonFallback() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginLegacyServiceBridgeConfiguration.java"));
        String managerSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/controller/BannedMacManager.java"));

        assertFalse(source.contains("BannedMacManager.getInstance()"));
        assertTrue(source.contains("return new BannedMacManager();"));
        assertTrue(managerSource.contains("SingletonHolder"));
        assertTrue(managerSource.contains("@Deprecated(since = \"boot-migration\")"));
        assertFalse(managerSource.contains("private static BannedMacManager manager = new BannedMacManager();"));
    }
}
