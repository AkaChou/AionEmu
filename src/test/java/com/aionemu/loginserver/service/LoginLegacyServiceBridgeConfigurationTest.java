package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.Shutdown;
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
}
