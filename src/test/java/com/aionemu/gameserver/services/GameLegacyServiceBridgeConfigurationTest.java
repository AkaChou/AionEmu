package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
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

    @Test
    void exposesRewardServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("rewardService"));
            assertTrue(context.containsBeanDefinition("weddingService"));
            assertTrue(context.containsBeanDefinition("veteranRewardsService"));
            assertEquals(RewardService.class, context.getType("rewardService"));
            assertEquals(WeddingService.class, context.getType("weddingService"));
            assertEquals(VeteranRewardsService.class, context.getType("veteranRewardsService"));
            assertLazy(context.getBeanFactory(), "rewardService");
            assertLazy(context.getBeanFactory(), "weddingService");
            assertLazy(context.getBeanFactory(), "veteranRewardsService");
        }
    }

    @Test
    void exposesCleaningServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("databaseCleaningService"));
            assertTrue(context.containsBeanDefinition("abyssRankCleaningService"));
            assertEquals(DatabaseCleaningService.class, context.getType("databaseCleaningService"));
            assertEquals(AbyssRankCleaningService.class, context.getType("abyssRankCleaningService"));
            assertLazy(context.getBeanFactory(), "databaseCleaningService");
            assertLazy(context.getBeanFactory(), "abyssRankCleaningService");
        }
    }

    @Test
    void exposesGeoNavServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("geoService"));
            assertTrue(context.containsBeanDefinition("navService"));
            assertEquals(GeoService.class, context.getType("geoService"));
            assertEquals(NavService.class, context.getType("navService"));
            assertLazy(context.getBeanFactory(), "geoService");
            assertLazy(context.getBeanFactory(), "navService");
        }
    }

    @Test
    void exposesStaticResourceServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("dataManager"));
            assertTrue(context.containsBeanDefinition("htmlCache"));
            assertEquals(DataManager.class, context.getType("dataManager"));
            assertEquals(HTMLCache.class, context.getType("htmlCache"));
            assertLazy(context.getBeanFactory(), "dataManager");
            assertLazy(context.getBeanFactory(), "htmlCache");
        }
    }

    @Test
    void exposesDisputeLandServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("disputeLandService"));
            assertTrue(context.containsBeanDefinition("outpostService"));
            assertEquals(DisputeLandService.class, context.getType("disputeLandService"));
            assertEquals(OutpostService.class, context.getType("outpostService"));
            assertLazy(context.getBeanFactory(), "disputeLandService");
            assertLazy(context.getBeanFactory(), "outpostService");
        }
    }

    @Test
    void exposesDredgionServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("dredgionService"));
            assertTrue(context.containsBeanDefinition("asyunatarService"));
            assertEquals(DredgionService2.class, context.getType("dredgionService"));
            assertEquals(AsyunatarService.class, context.getType("asyunatarService"));
            assertLazy(context.getBeanFactory(), "dredgionService");
            assertLazy(context.getBeanFactory(), "asyunatarService");
        }
    }

    @Test
    void exposesScheduledServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("shugoImperialTombSpawnManager"));
            assertEquals(ShugoImperialTombSpawnManager.class, context.getType("shugoImperialTombSpawnManager"));
            assertLazy(context.getBeanFactory(), "shugoImperialTombSpawnManager");
        }
    }

    @Test
    void exposesSeasonRankingServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("seasonRankingUpdateService"));
            assertEquals(SeasonRankingUpdateService.class, context.getType("seasonRankingUpdateService"));
            assertLazy(context.getBeanFactory(), "seasonRankingUpdateService");
        }
    }

    @Test
    void exposesProtectorConquerorServiceAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("protectorConquerorService"));
            assertEquals(ProtectorConquerorService.class, context.getType("protectorConquerorService"));
            assertLazy(context.getBeanFactory(), "protectorConquerorService");
        }
    }

    @Test
    void exposesWorldActivationServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("dropRegistrationService"));
            assertEquals(DropRegistrationService.class, context.getType("dropRegistrationService"));
            assertLazy(context.getBeanFactory(), "dropRegistrationService");
        }
    }

    private static void assertLazy(ConfigurableListableBeanFactory beanFactory, String beanName) {
        assertTrue(beanFactory.getBeanDefinition(beanName).isLazyInit());
    }
}
