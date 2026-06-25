package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.instance.EngulfedOphidanBridgeService;
import com.aionemu.gameserver.services.instance.GrandArenaTrainingCampService;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;
import com.aionemu.gameserver.services.instance.IDRunService;
import com.aionemu.gameserver.services.instance.IdgelDomeLandmarkService;
import com.aionemu.gameserver.services.instance.IdgelDomeService;
import com.aionemu.gameserver.services.instance.IronWallWarfrontService;
import com.aionemu.gameserver.services.instance.KamarBattlefieldService;
import com.aionemu.gameserver.services.instance.SuspiciousOphidanBridgeService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.aionemu.gameserver.ShutdownHook;

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
    void exposesCustomEventServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("ffaService"));
            assertTrue(context.containsBeanDefinition("ladderService"));
            assertTrue(context.containsBeanDefinition("bgService"));
            assertTrue(context.containsBeanDefinition("banditService"));
            assertEquals(FFAService.class, context.getType("ffaService"));
            assertEquals(LadderService.class, context.getType("ladderService"));
            assertEquals(BGService.class, context.getType("bgService"));
            assertEquals(BanditService.class, context.getType("banditService"));
            assertLazy(context.getBeanFactory(), "ffaService");
            assertLazy(context.getBeanFactory(), "ladderService");
            assertLazy(context.getBeanFactory(), "bgService");
            assertLazy(context.getBeanFactory(), "banditService");
        }
    }

    @Test
    void exposesOptionalServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("playerLimitService"));
            assertTrue(context.containsBeanDefinition("npcShoutsService"));
            assertTrue(context.containsBeanDefinition("shieldService"));
            assertEquals(PlayerLimitService.class, context.getType("playerLimitService"));
            assertEquals(NpcShoutsService.class, context.getType("npcShoutsService"));
            assertEquals(ShieldService.class, context.getType("shieldService"));
            assertLazy(context.getBeanFactory(), "playerLimitService");
            assertLazy(context.getBeanFactory(), "npcShoutsService");
            assertLazy(context.getBeanFactory(), "shieldService");
        }
    }

    @Test
    void exposesHousingServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("housingBidService"));
            assertTrue(context.containsBeanDefinition("maintenanceTask"));
            assertTrue(context.containsBeanDefinition("townService"));
            assertTrue(context.containsBeanDefinition("challengeTaskService"));
            assertEquals(HousingBidService.class, context.getType("housingBidService"));
            assertEquals(MaintenanceTask.class, context.getType("maintenanceTask"));
            assertEquals(TownService.class, context.getType("townService"));
            assertEquals(ChallengeTaskService.class, context.getType("challengeTaskService"));
            assertLazy(context.getBeanFactory(), "housingBidService");
            assertLazy(context.getBeanFactory(), "maintenanceTask");
            assertLazy(context.getBeanFactory(), "townService");
            assertLazy(context.getBeanFactory(), "challengeTaskService");
        }
    }

    @Test
    void exposesBattlefieldServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("kamarBattlefieldService"));
            assertTrue(context.containsBeanDefinition("engulfedOphidanBridgeService"));
            assertTrue(context.containsBeanDefinition("suspiciousOphidanBridgeService"));
            assertTrue(context.containsBeanDefinition("ironWallWarfrontService"));
            assertTrue(context.containsBeanDefinition("idgelDomeService"));
            assertTrue(context.containsBeanDefinition("idgelDomeLandmarkService"));
            assertTrue(context.containsBeanDefinition("hallOfTenacityService"));
            assertTrue(context.containsBeanDefinition("grandArenaTrainingCampService"));
            assertTrue(context.containsBeanDefinition("idRunService"));
            assertEquals(KamarBattlefieldService.class, context.getType("kamarBattlefieldService"));
            assertEquals(EngulfedOphidanBridgeService.class, context.getType("engulfedOphidanBridgeService"));
            assertEquals(SuspiciousOphidanBridgeService.class, context.getType("suspiciousOphidanBridgeService"));
            assertEquals(IronWallWarfrontService.class, context.getType("ironWallWarfrontService"));
            assertEquals(IdgelDomeService.class, context.getType("idgelDomeService"));
            assertEquals(IdgelDomeLandmarkService.class, context.getType("idgelDomeLandmarkService"));
            assertEquals(HallOfTenacityService.class, context.getType("hallOfTenacityService"));
            assertEquals(GrandArenaTrainingCampService.class, context.getType("grandArenaTrainingCampService"));
            assertEquals(IDRunService.class, context.getType("idRunService"));
            assertLazy(context.getBeanFactory(), "kamarBattlefieldService");
            assertLazy(context.getBeanFactory(), "engulfedOphidanBridgeService");
            assertLazy(context.getBeanFactory(), "suspiciousOphidanBridgeService");
            assertLazy(context.getBeanFactory(), "ironWallWarfrontService");
            assertLazy(context.getBeanFactory(), "idgelDomeService");
            assertLazy(context.getBeanFactory(), "idgelDomeLandmarkService");
            assertLazy(context.getBeanFactory(), "hallOfTenacityService");
            assertLazy(context.getBeanFactory(), "grandArenaTrainingCampService");
            assertLazy(context.getBeanFactory(), "idRunService");
        }
    }

    @Test
    void exposesThreadPoolManagerAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("threadPoolManager"));
            assertEquals(ThreadPoolManager.class, context.getType("threadPoolManager"));
            assertLazy(context.getBeanFactory(), "threadPoolManager");
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

    @Test
    void exposesNetworkStartupServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("shutdownHook"));
            assertEquals(ShutdownHook.class, context.getType("shutdownHook"));
            assertLazy(context.getBeanFactory(), "shutdownHook");
        }
    }

    @Test
    void exposesNetworkPeerServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("bannedMacManager"));
            assertTrue(context.containsBeanDefinition("loginServer"));
            assertTrue(context.containsBeanDefinition("chatServer"));
            assertEquals(BannedMacManager.class, context.getType("bannedMacManager"));
            assertEquals(LoginServer.class, context.getType("loginServer"));
            assertEquals(ChatServer.class, context.getType("chatServer"));
            assertLazy(context.getBeanFactory(), "bannedMacManager");
            assertLazy(context.getBeanFactory(), "loginServer");
            assertLazy(context.getBeanFactory(), "chatServer");
        }
    }

    @Test
    void exposesSiegeScheduleServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("siegeService"));
            assertTrue(context.containsBeanDefinition("baseService"));
            assertEquals(SiegeService.class, context.getType("siegeService"));
            assertEquals(BaseService.class, context.getType("baseService"));
            assertLazy(context.getBeanFactory(), "siegeService");
            assertLazy(context.getBeanFactory(), "baseService");
        }
    }

    private static void assertLazy(ConfigurableListableBeanFactory beanFactory, String beanName) {
        assertTrue(beanFactory.getBeanDefinition(beanName).isLazyInit());
    }
}
