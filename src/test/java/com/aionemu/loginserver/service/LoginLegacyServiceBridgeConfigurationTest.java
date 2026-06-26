package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.network.ServerTransport;
import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.controller.BannedMacManager;
import com.aionemu.loginserver.controller.PremiumController;
import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import com.aionemu.loginserver.utils.BruteForceProtector;
import com.aionemu.loginserver.utils.FloodProtector;
import com.aionemu.loginserver.utils.ThreadPoolManager;
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
    void exposesLoginThreadPoolManagerAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LoginLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("loginThreadPoolManager"));
            assertEquals(ThreadPoolManager.class, context.getType("loginThreadPoolManager"));
            assertTrue(context.getBeanFactory().getBeanDefinition("loginThreadPoolManager").isLazyInit());
        }
    }

    @Test
    void exposesLoginServerTransportAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LoginLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("loginServerTransport"));
            assertEquals(ServerTransport.class, context.getType("loginServerTransport"));
            assertTrue(context.getBeanFactory().getBeanDefinition("loginServerTransport").isLazyInit());
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
    void exposesLoginBannedIpServiceAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LoginLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("loginBannedIpService"));
            assertEquals(LoginBannedIpService.class, context.getType("loginBannedIpService"));
            assertTrue(context.getBeanFactory().getBeanDefinition("loginBannedIpService").isLazyInit());
        }
    }

    @Test
    void exposesLoginProtectionServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LoginLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("loginBruteForceProtector"));
            assertEquals(BruteForceProtector.class, context.getType("loginBruteForceProtector"));
            assertTrue(context.getBeanFactory().getBeanDefinition("loginBruteForceProtector").isLazyInit());

            assertTrue(context.containsBeanDefinition("loginFloodProtector"));
            assertEquals(FloodProtector.class, context.getType("loginFloodProtector"));
            assertTrue(context.getBeanFactory().getBeanDefinition("loginFloodProtector").isLazyInit());
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
    void loginThreadPoolManagerBeanUsesSpringInstantiationInsteadOfSingletonFallback() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginLegacyServiceBridgeConfiguration.java"));
        String managerSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/utils/ThreadPoolManager.java"));

        assertFalse(source.contains("ThreadPoolManager.getInstance()"));
        assertTrue(source.contains("return new ThreadPoolManager();"));
        assertTrue(managerSource.contains("public ThreadPoolManager()"));
        assertTrue(managerSource.contains("@Deprecated(since = \"boot-migration\")"));
    }

    @Test
    void loginServerTransportBeanUsesConnectorLifecycleInsteadOfLegacyAccessor() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginLegacyServiceBridgeConfiguration.java"));
        String connectorSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/NetConnector.java"));

        assertFalse(source.contains("NetConnector.getInstance()"));
        assertTrue(source.contains("return NetConnector.currentTransport();"));
        assertFalse(source.contains("return new NetConnector();"));
        assertTrue(connectorSource.contains("@Deprecated(since = \"boot-migration\")"));
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

    @Test
    void loginBannedIpServiceBeanUsesSpringInstantiationInsteadOfSingletonFallback() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginLegacyServiceBridgeConfiguration.java"));

        assertFalse(source.contains("BannedIpController"));
        assertTrue(source.contains("return new LoginBannedIpService();"));
    }

    @Test
    void loginProtectionBeansUseSpringInstantiationInsteadOfSingletonFallbacks() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginLegacyServiceBridgeConfiguration.java"));
        String bruteForceSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/utils/BruteForceProtector.java"));
        String floodSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/utils/FloodProtector.java"));

        assertFalse(source.contains("BruteForceProtector.getInstance()"));
        assertFalse(source.contains("FloodProtector.getInstance()"));
        assertTrue(source.contains("return new BruteForceProtector();"));
        assertTrue(source.contains("return new FloodProtector();"));
        assertTrue(bruteForceSource.contains("@Deprecated(since = \"boot-migration\")"));
        assertTrue(floodSource.contains("@Deprecated(since = \"boot-migration\")"));
    }
}
