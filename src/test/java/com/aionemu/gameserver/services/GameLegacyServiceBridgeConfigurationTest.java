package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class GameLegacyServiceBridgeConfigurationTest {

    @Test
    void exposesLegacyGameServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("adminService"));
            assertTrue(context.containsBeanDefinition("gamePlayerTransferService"));
            assertEquals(AdminService.class, context.getType("adminService"));
            assertEquals(PlayerTransferService.class, context.getType("gamePlayerTransferService"));
            assertLazy(context.getBeanFactory(), "adminService");
            assertLazy(context.getBeanFactory(), "gamePlayerTransferService");
        }
    }

    @Test
    void exposesEventRuntimeServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("eventService"));
            assertTrue(context.containsBeanDefinition("playerEventService"));
            assertTrue(context.containsBeanDefinition("crazyDaevaService"));
            assertTrue(context.containsBeanDefinition("abyssRankUpdateService"));
            assertTrue(context.containsBeanDefinition("packetBroadcaster"));
            assertEquals(EventService.class, context.getType("eventService"));
            assertEquals(PlayerEventService.class, context.getType("playerEventService"));
            assertEquals(CrazyDaevaService.class, context.getType("crazyDaevaService"));
            assertEquals(AbyssRankUpdateService.class, context.getType("abyssRankUpdateService"));
            assertEquals(PacketBroadcaster.class, context.getType("packetBroadcaster"));
            assertLazy(context.getBeanFactory(), "eventService");
            assertLazy(context.getBeanFactory(), "playerEventService");
            assertLazy(context.getBeanFactory(), "crazyDaevaService");
            assertLazy(context.getBeanFactory(), "abyssRankUpdateService");
            assertLazy(context.getBeanFactory(), "packetBroadcaster");
        }
    }

    private static void assertLazy(ConfigurableListableBeanFactory beanFactory, String beanName) {
        assertTrue(beanFactory.getBeanDefinition(beanName).isLazyInit());
    }
}
