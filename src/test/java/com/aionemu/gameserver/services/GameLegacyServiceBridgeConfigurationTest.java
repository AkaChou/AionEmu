package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class GameLegacyServiceBridgeConfigurationTest {

    @Test
    void exposesLegacyGameServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("adminService"));
            assertTrue(context.containsBeanDefinition("gamePlayerTransferService"));
            assertEquals(AdminService.class, context.getType("adminService"));
            assertEquals(PlayerTransferService.class, context.getType("gamePlayerTransferService"));
            assertTrue(context.getBeanFactory().getBeanDefinition("adminService").isLazyInit());
            assertTrue(context.getBeanFactory().getBeanDefinition("gamePlayerTransferService").isLazyInit());
        }
    }
}
