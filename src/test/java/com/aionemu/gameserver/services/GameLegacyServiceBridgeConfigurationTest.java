package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.lifecycle.GameRuntimeServiceBridge;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.services.abysslandingservice.LandingUpdateService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.services.craft.RelinquishCraftStatus;
import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.ArcadeUpgradeService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.BoostEventService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
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
import com.aionemu.gameserver.services.item.CoalescenceService;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.services.player.AtreianBestiaryService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityEssenceService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativitySkillService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityStatsService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityTransfoService;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Accuracy;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Agility;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Health;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Knowledge;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Power;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Precision;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Will;
import com.aionemu.gameserver.services.player.GrowthEnergy;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.drop.DropDistributionService;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.reward.BonusService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.rift.RiftManager;
import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.services.toypet.PetService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.taskmanager.tasks.TeamEffectUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TeamMoveUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TemporaryTradeTimeTask;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import com.aionemu.gameserver.world.zone.ZoneService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    void createsSpringManagedAdminServiceInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(AdminService.getInstance(), context.getBean(AdminService.class));
        }
    }

    @Test
    void createsSpringManagedPlayerTransferServiceInsteadOfLegacySingleton() {
        assertConfigurationCreatesNew(PlayerTransferService.class);
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
    void createsSpringManagedFfaServiceInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(FFAService.getInstance(), context.getBean(FFAService.class));
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
    void createsSpringManagedShieldServiceInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(ShieldService.getInstance(), context.getBean(ShieldService.class));
        }
    }

    @Test
    void createsSpringManagedPlayerLimitServiceInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(PlayerLimitService.getInstance(), context.getBean(PlayerLimitService.class));
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
    void housingBidServiceReadsRegisterEndCronFromCurrentHousingConfig() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/HousingBidService.java"));

        assertFalse(source.contains("static final String registerEndExpression = HousingConfig.HOUSE_REGISTER_END"));
        assertTrue(source.contains("new CronExpression(HousingConfig.HOUSE_REGISTER_END)"));
    }

    @Test
    void createsSpringManagedChallengeTaskServiceInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(ChallengeTaskService.getInstance(), context.getBean(ChallengeTaskService.class));
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
    void createsSpringManagedBattlefieldServicesInsteadOfLegacySingletons() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(KamarBattlefieldService.getInstance(), context.getBean(KamarBattlefieldService.class));
            assertNotSame(EngulfedOphidanBridgeService.getInstance(), context.getBean(EngulfedOphidanBridgeService.class));
            assertNotSame(SuspiciousOphidanBridgeService.getInstance(), context.getBean(SuspiciousOphidanBridgeService.class));
            assertNotSame(IronWallWarfrontService.getInstance(), context.getBean(IronWallWarfrontService.class));
            assertNotSame(IdgelDomeService.getInstance(), context.getBean(IdgelDomeService.class));
            assertNotSame(IdgelDomeLandmarkService.getInstance(), context.getBean(IdgelDomeLandmarkService.class));
            assertNotSame(HallOfTenacityService.getInstance(), context.getBean(HallOfTenacityService.class));
            assertNotSame(GrandArenaTrainingCampService.getInstance(), context.getBean(GrandArenaTrainingCampService.class));
            assertNotSame(IDRunService.getInstance(), context.getBean(IDRunService.class));
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
    void exposesGameEnginesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("questEngine"));
            assertTrue(context.containsBeanDefinition("instanceEngine"));
            assertTrue(context.containsBeanDefinition("ai2Engine"));
            assertTrue(context.containsBeanDefinition("chatProcessor"));
            assertEquals(QuestEngine.class, context.getType("questEngine"));
            assertEquals(InstanceEngine.class, context.getType("instanceEngine"));
            assertEquals(AI2Engine.class, context.getType("ai2Engine"));
            assertEquals(ChatProcessor.class, context.getType("chatProcessor"));
            assertLazy(context.getBeanFactory(), "questEngine");
            assertLazy(context.getBeanFactory(), "instanceEngine");
            assertLazy(context.getBeanFactory(), "ai2Engine");
            assertLazy(context.getBeanFactory(), "chatProcessor");
        }
    }

    @Test
    void createsSpringManagedGameEnginesInsteadOfLegacySingletons() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(QuestEngine.getInstance(), context.getBean(QuestEngine.class));
            assertNotSame(InstanceEngine.getInstance(), context.getBean(InstanceEngine.class));
            assertNotSame(AI2Engine.getInstance(), context.getBean(AI2Engine.class));
            assertNotSame(ChatProcessor.getInstance(), context.getBean(ChatProcessor.class));
        }
    }

    @Test
    void exposesEventBootstrapServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("lunaShopService"));
            assertTrue(context.containsBeanDefinition("minionService"));
            assertTrue(context.containsBeanDefinition("shugoSweepService"));
            assertTrue(context.containsBeanDefinition("atreianPassportService"));
            assertTrue(context.containsBeanDefinition("eventWindowService"));
            assertEquals(LunaShopService.class, context.getType("lunaShopService"));
            assertEquals(MinionService.class, context.getType("minionService"));
            assertEquals(ShugoSweepService.class, context.getType("shugoSweepService"));
            assertEquals(AtreianPassportService.class, context.getType("atreianPassportService"));
            assertEquals(EventWindowService.class, context.getType("eventWindowService"));
            assertLazy(context.getBeanFactory(), "lunaShopService");
            assertLazy(context.getBeanFactory(), "minionService");
            assertLazy(context.getBeanFactory(), "shugoSweepService");
            assertLazy(context.getBeanFactory(), "atreianPassportService");
            assertLazy(context.getBeanFactory(), "eventWindowService");
        }
    }

    @Test
    void createsSpringManagedLightweightEventBootstrapServicesInsteadOfLegacySingletons() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(MinionService.getInstance(), context.getBean(MinionService.class));
            assertNotSame(ShugoSweepService.getInstance(), context.getBean(ShugoSweepService.class));
            assertNotSame(AtreianPassportService.getInstance(), context.getBean(AtreianPassportService.class));
        }
    }

    @Test
    void exposesRuntimeServicesAsSpringBeansWithEagerBridgeOnly() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("periodicSaveService"));
            assertTrue(context.containsBeanDefinition("territoryService"));
            assertTrue(context.containsBeanDefinition("gameTimeService"));
            assertTrue(context.containsBeanDefinition("announcementService"));
            assertTrue(context.containsBeanDefinition("debugService"));
            assertTrue(context.containsBeanDefinition("weatherService"));
            assertTrue(context.containsBeanDefinition("brokerService"));
            assertTrue(context.containsBeanDefinition("influence"));
            assertTrue(context.containsBeanDefinition("exchangeService"));
            assertTrue(context.containsBeanDefinition("petitionService"));
            assertTrue(context.containsBeanDefinition("flyRingService"));
            assertTrue(context.containsBeanDefinition("curingZoneService"));
            assertTrue(context.containsBeanDefinition("springZoneService"));
            assertTrue(context.containsBeanDefinition("boostEventService"));
            assertTrue(context.containsBeanDefinition("taskManagerFromDB"));
            assertTrue(context.containsBeanDefinition("limitedItemTradeService"));
            assertTrue(context.containsBeanDefinition("gameRuntimeServiceBridge"));
            assertEquals(PeriodicSaveService.class, context.getType("periodicSaveService"));
            assertEquals(TerritoryService.class, context.getType("territoryService"));
            assertEquals(GameTimeService.class, context.getType("gameTimeService"));
            assertEquals(AnnouncementService.class, context.getType("announcementService"));
            assertEquals(DebugService.class, context.getType("debugService"));
            assertEquals(WeatherService.class, context.getType("weatherService"));
            assertEquals(BrokerService.class, context.getType("brokerService"));
            assertEquals(Influence.class, context.getType("influence"));
            assertEquals(ExchangeService.class, context.getType("exchangeService"));
            assertEquals(PetitionService.class, context.getType("petitionService"));
            assertEquals(FlyRingService.class, context.getType("flyRingService"));
            assertEquals(CuringZoneService.class, context.getType("curingZoneService"));
            assertEquals(SpringZoneService.class, context.getType("springZoneService"));
            assertEquals(BoostEventService.class, context.getType("boostEventService"));
            assertEquals(TaskManagerFromDB.class, context.getType("taskManagerFromDB"));
            assertEquals(LimitedItemTradeService.class, context.getType("limitedItemTradeService"));
            assertEquals(GameRuntimeServiceBridge.class, context.getType("gameRuntimeServiceBridge"));
            assertLazy(context.getBeanFactory(), "periodicSaveService");
            assertLazy(context.getBeanFactory(), "territoryService");
            assertLazy(context.getBeanFactory(), "gameTimeService");
            assertLazy(context.getBeanFactory(), "announcementService");
            assertLazy(context.getBeanFactory(), "debugService");
            assertLazy(context.getBeanFactory(), "weatherService");
            assertLazy(context.getBeanFactory(), "brokerService");
            assertLazy(context.getBeanFactory(), "influence");
            assertLazy(context.getBeanFactory(), "exchangeService");
            assertLazy(context.getBeanFactory(), "petitionService");
            assertLazy(context.getBeanFactory(), "flyRingService");
            assertLazy(context.getBeanFactory(), "curingZoneService");
            assertLazy(context.getBeanFactory(), "springZoneService");
            assertLazy(context.getBeanFactory(), "boostEventService");
            assertLazy(context.getBeanFactory(), "taskManagerFromDB");
            assertLazy(context.getBeanFactory(), "limitedItemTradeService");
            assertEager(context.getBeanFactory(), "gameRuntimeServiceBridge");
        }
    }

    @Test
    void createsSpringManagedTerritoryAndLimitedTradeServicesInsteadOfLegacySingletons() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(TerritoryService.getInstance(), context.getBean(TerritoryService.class));
            assertNotSame(LimitedItemTradeService.getInstance(), context.getBean(LimitedItemTradeService.class));
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
    void createsSpringManagedWeddingServiceInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(WeddingService.getInstance(), context.getBean(WeddingService.class));
        }
    }

    @Test
    void createsSpringManagedRewardServiceInsteadOfLegacySingleton() {
        assertConfigurationCreatesNew(RewardService.class);
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
    void createsSpringManagedGeoNavServicesInsteadOfLegacySingletons() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(GeoService.getInstance(), context.getBean(GeoService.class));
            assertNotSame(NavService.getInstance(), context.getBean(NavService.class));
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
    void createsSpringManagedOutpostServiceInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(OutpostService.getInstance(), context.getBean(OutpostService.class));
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
    void createsSpringManagedDredgionServicesInsteadOfLegacySingletons() {
        assertConfigurationCreatesNew(DredgionService2.class);
        assertConfigurationCreatesNew(AsyunatarService.class);
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
    void createsSpringManagedSeasonRankingUpdateServiceInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(SeasonRankingUpdateService.getInstance(), context.getBean(SeasonRankingUpdateService.class));
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
    void createsSpringManagedProtectorConquerorServiceInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(ProtectorConquerorService.getInstance(), context.getBean(ProtectorConquerorService.class));
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
    void exposesWorldBootstrapServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("gameIdFactory"));
            assertTrue(context.containsBeanDefinition("zoneService"));
            assertTrue(context.containsBeanDefinition("hotspotTeleportService"));
            assertTrue(context.containsBeanDefinition("roadService"));
            assertTrue(context.containsBeanDefinition("world"));
            assertEquals(IDFactory.class, context.getType("gameIdFactory"));
            assertEquals(ZoneService.class, context.getType("zoneService"));
            assertEquals(HotspotTeleportService.class, context.getType("hotspotTeleportService"));
            assertEquals(RoadService.class, context.getType("roadService"));
            assertEquals(World.class, context.getType("world"));
            assertLazy(context.getBeanFactory(), "gameIdFactory");
            assertLazy(context.getBeanFactory(), "zoneService");
            assertLazy(context.getBeanFactory(), "hotspotTeleportService");
            assertLazy(context.getBeanFactory(), "roadService");
            assertLazy(context.getBeanFactory(), "world");
        }
    }

    @Test
    void exposesPlayerEntryCompatibilityServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("aStationService"));
            assertTrue(context.containsBeanDefinition("f2pService"));
            assertTrue(context.containsBeanDefinition("windyGorgeService"));
            assertTrue(context.containsBeanDefinition("motionLoggingService"));
            assertTrue(context.containsBeanDefinition("staticDoorService"));
            assertTrue(context.containsBeanDefinition("kiskService"));
            assertTrue(context.containsBeanDefinition("repurchaseService"));
            assertTrue(context.containsBeanDefinition("dropDistributionService"));
            assertTrue(context.containsBeanDefinition("systemMailService"));
            assertEquals(AStationService.class, context.getType("aStationService"));
            assertEquals(F2pService.class, context.getType("f2pService"));
            assertEquals(WindyGorgeService.class, context.getType("windyGorgeService"));
            assertEquals(MotionLoggingService.class, context.getType("motionLoggingService"));
            assertEquals(StaticDoorService.class, context.getType("staticDoorService"));
            assertEquals(KiskService.class, context.getType("kiskService"));
            assertEquals(RepurchaseService.class, context.getType("repurchaseService"));
            assertEquals(DropDistributionService.class, context.getType("dropDistributionService"));
            assertEquals(SystemMailService.class, context.getType("systemMailService"));
            assertLazy(context.getBeanFactory(), "aStationService");
            assertLazy(context.getBeanFactory(), "f2pService");
            assertLazy(context.getBeanFactory(), "windyGorgeService");
            assertLazy(context.getBeanFactory(), "motionLoggingService");
            assertLazy(context.getBeanFactory(), "staticDoorService");
            assertLazy(context.getBeanFactory(), "kiskService");
            assertLazy(context.getBeanFactory(), "repurchaseService");
            assertLazy(context.getBeanFactory(), "dropDistributionService");
            assertLazy(context.getBeanFactory(), "systemMailService");
        }
    }

    @Test
    void exposesPlayerActionCompatibilityServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("bonusService"));
            assertTrue(context.containsBeanDefinition("petService"));
            assertTrue(context.containsBeanDefinition("arcadeUpgradeService"));
            assertTrue(context.containsBeanDefinition("atreianBestiaryService"));
            assertTrue(context.containsBeanDefinition("coalescenceService"));
            assertTrue(context.containsBeanDefinition("growthEnergy"));
            assertEquals(BonusService.class, context.getType("bonusService"));
            assertEquals(PetService.class, context.getType("petService"));
            assertEquals(ArcadeUpgradeService.class, context.getType("arcadeUpgradeService"));
            assertEquals(AtreianBestiaryService.class, context.getType("atreianBestiaryService"));
            assertEquals(CoalescenceService.class, context.getType("coalescenceService"));
            assertEquals(GrowthEnergy.class, context.getType("growthEnergy"));
            assertLazy(context.getBeanFactory(), "bonusService");
            assertLazy(context.getBeanFactory(), "petService");
            assertLazy(context.getBeanFactory(), "arcadeUpgradeService");
            assertLazy(context.getBeanFactory(), "atreianBestiaryService");
            assertLazy(context.getBeanFactory(), "coalescenceService");
            assertLazy(context.getBeanFactory(), "growthEnergy");
        }
    }

    @Test
    void exposesTaskManagerCompatibilityServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("expireTimerTask"));
            assertTrue(context.containsBeanDefinition("teamEffectUpdater"));
            assertTrue(context.containsBeanDefinition("teamMoveUpdater"));
            assertTrue(context.containsBeanDefinition("temporaryTradeTimeTask"));
            assertEquals(ExpireTimerTask.class, context.getType("expireTimerTask"));
            assertEquals(TeamEffectUpdater.class, context.getType("teamEffectUpdater"));
            assertEquals(TeamMoveUpdater.class, context.getType("teamMoveUpdater"));
            assertEquals(TemporaryTradeTimeTask.class, context.getType("temporaryTradeTimeTask"));
            assertLazy(context.getBeanFactory(), "expireTimerTask");
            assertLazy(context.getBeanFactory(), "teamEffectUpdater");
            assertLazy(context.getBeanFactory(), "teamMoveUpdater");
            assertLazy(context.getBeanFactory(), "temporaryTradeTimeTask");
        }
    }

    @Test
    void exposesCreativityCompatibilityServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("creativityEssenceService"));
            assertTrue(context.containsBeanDefinition("creativitySkillService"));
            assertTrue(context.containsBeanDefinition("creativityStatsService"));
            assertTrue(context.containsBeanDefinition("creativityTransfoService"));
            assertTrue(context.containsBeanDefinition("accuracy"));
            assertTrue(context.containsBeanDefinition("agility"));
            assertTrue(context.containsBeanDefinition("health"));
            assertTrue(context.containsBeanDefinition("knowledge"));
            assertTrue(context.containsBeanDefinition("power"));
            assertTrue(context.containsBeanDefinition("precision"));
            assertTrue(context.containsBeanDefinition("will"));
            assertEquals(CreativityEssenceService.class, context.getType("creativityEssenceService"));
            assertEquals(CreativitySkillService.class, context.getType("creativitySkillService"));
            assertEquals(CreativityStatsService.class, context.getType("creativityStatsService"));
            assertEquals(CreativityTransfoService.class, context.getType("creativityTransfoService"));
            assertEquals(Accuracy.class, context.getType("accuracy"));
            assertEquals(Agility.class, context.getType("agility"));
            assertEquals(Health.class, context.getType("health"));
            assertEquals(Knowledge.class, context.getType("knowledge"));
            assertEquals(Power.class, context.getType("power"));
            assertEquals(Precision.class, context.getType("precision"));
            assertEquals(Will.class, context.getType("will"));
            assertLazy(context.getBeanFactory(), "creativityEssenceService");
            assertLazy(context.getBeanFactory(), "creativitySkillService");
            assertLazy(context.getBeanFactory(), "creativityStatsService");
            assertLazy(context.getBeanFactory(), "creativityTransfoService");
            assertLazy(context.getBeanFactory(), "accuracy");
            assertLazy(context.getBeanFactory(), "agility");
            assertLazy(context.getBeanFactory(), "health");
            assertLazy(context.getBeanFactory(), "knowledge");
            assertLazy(context.getBeanFactory(), "power");
            assertLazy(context.getBeanFactory(), "precision");
            assertLazy(context.getBeanFactory(), "will");
        }
    }

    @Test
    void exposesCraftCompatibilityServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("craftSkillUpdateService"));
            assertTrue(context.containsBeanDefinition("relinquishCraftStatus"));
            assertEquals(CraftSkillUpdateService.class, context.getType("craftSkillUpdateService"));
            assertEquals(RelinquishCraftStatus.class, context.getType("relinquishCraftStatus"));
            assertLazy(context.getBeanFactory(), "craftSkillUpdateService");
            assertLazy(context.getBeanFactory(), "relinquishCraftStatus");
        }
    }

    @Test
    void exposesRuntimeCompatibilityServicesWithCoreRestorationEager() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("duelService"));
            assertTrue(context.containsBeanDefinition("lifeStatsRestoreService"));
            assertTrue(context.containsBeanDefinition("seasonRankingService"));
            assertTrue(context.containsBeanDefinition("riftManager"));
            assertEquals(DuelService.class, context.getType("duelService"));
            assertEquals(LifeStatsRestoreService.class, context.getType("lifeStatsRestoreService"));
            assertEquals(SeasonRankingService.class, context.getType("seasonRankingService"));
            assertEquals(RiftManager.class, context.getType("riftManager"));
            assertLazy(context.getBeanFactory(), "duelService");
            assertEager(context.getBeanFactory(), "lifeStatsRestoreService");
            assertLazy(context.getBeanFactory(), "seasonRankingService");
            assertLazy(context.getBeanFactory(), "riftManager");
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
    void createsSpringManagedShutdownHookInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(ShutdownHook.getInstance(), context.getBean(ShutdownHook.class));
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
    void createsSpringManagedBannedMacManagerInsteadOfLegacySingleton() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(BannedMacManager.getInstance(), context.getBean(BannedMacManager.class));
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

    @Test
    void createsSpringManagedSiegeScheduleServicesInsteadOfLegacySingletons() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(SiegeService.getInstance(), context.getBean(SiegeService.class));
            assertNotSame(BaseService.getInstance(), context.getBean(BaseService.class));
        }
    }

    @Test
    void exposesLocationBootstrapServicesAsLazySpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("vortexService"));
            assertTrue(context.containsBeanDefinition("beritraService"));
            assertTrue(context.containsBeanDefinition("agentService"));
            assertTrue(context.containsBeanDefinition("anohaService"));
            assertTrue(context.containsBeanDefinition("svsService"));
            assertTrue(context.containsBeanDefinition("rvrService"));
            assertTrue(context.containsBeanDefinition("iuService"));
            assertTrue(context.containsBeanDefinition("nightmareCircusService"));
            assertTrue(context.containsBeanDefinition("dynamicRiftService"));
            assertTrue(context.containsBeanDefinition("instanceRiftService"));
            assertTrue(context.containsBeanDefinition("zorshivDredgionService"));
            assertTrue(context.containsBeanDefinition("moltenusService"));
            assertTrue(context.containsBeanDefinition("riftService"));
            assertTrue(context.containsBeanDefinition("conquestService"));
            assertTrue(context.containsBeanDefinition("idianDepthsService"));
            assertTrue(context.containsBeanDefinition("towerOfEternityService"));
            assertTrue(context.containsBeanDefinition("abyssLandingService"));
            assertTrue(context.containsBeanDefinition("landingUpdateService"));
            assertTrue(context.containsBeanDefinition("abyssLandingSpecialService"));
            assertEquals(VortexService.class, context.getType("vortexService"));
            assertEquals(BeritraService.class, context.getType("beritraService"));
            assertEquals(AgentService.class, context.getType("agentService"));
            assertEquals(AnohaService.class, context.getType("anohaService"));
            assertEquals(SvsService.class, context.getType("svsService"));
            assertEquals(RvrService.class, context.getType("rvrService"));
            assertEquals(IuService.class, context.getType("iuService"));
            assertEquals(NightmareCircusService.class, context.getType("nightmareCircusService"));
            assertEquals(DynamicRiftService.class, context.getType("dynamicRiftService"));
            assertEquals(InstanceRiftService.class, context.getType("instanceRiftService"));
            assertEquals(ZorshivDredgionService.class, context.getType("zorshivDredgionService"));
            assertEquals(MoltenusService.class, context.getType("moltenusService"));
            assertEquals(RiftService.class, context.getType("riftService"));
            assertEquals(ConquestService.class, context.getType("conquestService"));
            assertEquals(IdianDepthsService.class, context.getType("idianDepthsService"));
            assertEquals(TowerOfEternityService.class, context.getType("towerOfEternityService"));
            assertEquals(AbyssLandingService.class, context.getType("abyssLandingService"));
            assertEquals(LandingUpdateService.class, context.getType("landingUpdateService"));
            assertEquals(AbyssLandingSpecialService.class, context.getType("abyssLandingSpecialService"));
            assertLazy(context.getBeanFactory(), "vortexService");
            assertLazy(context.getBeanFactory(), "beritraService");
            assertLazy(context.getBeanFactory(), "agentService");
            assertLazy(context.getBeanFactory(), "anohaService");
            assertLazy(context.getBeanFactory(), "svsService");
            assertLazy(context.getBeanFactory(), "rvrService");
            assertLazy(context.getBeanFactory(), "iuService");
            assertLazy(context.getBeanFactory(), "nightmareCircusService");
            assertLazy(context.getBeanFactory(), "dynamicRiftService");
            assertLazy(context.getBeanFactory(), "instanceRiftService");
            assertLazy(context.getBeanFactory(), "zorshivDredgionService");
            assertLazy(context.getBeanFactory(), "moltenusService");
            assertLazy(context.getBeanFactory(), "riftService");
            assertLazy(context.getBeanFactory(), "conquestService");
            assertLazy(context.getBeanFactory(), "idianDepthsService");
            assertLazy(context.getBeanFactory(), "towerOfEternityService");
            assertLazy(context.getBeanFactory(), "abyssLandingService");
            assertLazy(context.getBeanFactory(), "landingUpdateService");
            assertLazy(context.getBeanFactory(), "abyssLandingSpecialService");
        }
    }

    @Test
    void createsSpringManagedLocationBootstrapServicesInsteadOfLegacySingletons() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(VortexService.getInstance(), context.getBean(VortexService.class));
            assertNotSame(BeritraService.getInstance(), context.getBean(BeritraService.class));
            assertNotSame(AgentService.getInstance(), context.getBean(AgentService.class));
            assertNotSame(AnohaService.getInstance(), context.getBean(AnohaService.class));
            assertNotSame(SvsService.getInstance(), context.getBean(SvsService.class));
            assertNotSame(RvrService.getInstance(), context.getBean(RvrService.class));
            assertNotSame(IuService.getInstance(), context.getBean(IuService.class));
        }
    }

    @Test
    void createsSpringManagedRiftAndLandingServicesInsteadOfLegacySingletons() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GameLegacyServiceBridgeConfiguration.class)) {
            assertNotSame(NightmareCircusService.getInstance(), context.getBean(NightmareCircusService.class));
            assertNotSame(DynamicRiftService.getInstance(), context.getBean(DynamicRiftService.class));
            assertNotSame(InstanceRiftService.getInstance(), context.getBean(InstanceRiftService.class));
            assertNotSame(ZorshivDredgionService.getInstance(), context.getBean(ZorshivDredgionService.class));
            assertNotSame(MoltenusService.getInstance(), context.getBean(MoltenusService.class));
            assertNotSame(RiftService.getInstance(), context.getBean(RiftService.class));
            assertNotSame(ConquestService.getInstance(), context.getBean(ConquestService.class));
            assertNotSame(IdianDepthsService.getInstance(), context.getBean(IdianDepthsService.class));
            assertNotSame(TowerOfEternityService.getInstance(), context.getBean(TowerOfEternityService.class));
            assertNotSame(AbyssLandingService.getInstance(), context.getBean(AbyssLandingService.class));
        }
    }

    private static void assertLazy(ConfigurableListableBeanFactory beanFactory, String beanName) {
        assertTrue(beanFactory.getBeanDefinition(beanName).isLazyInit());
    }

    private static void assertEager(ConfigurableListableBeanFactory beanFactory, String beanName) {
        assertFalse(beanFactory.getBeanDefinition(beanName).isLazyInit());
    }

    private static void assertConfigurationCreatesNew(Class<?> type) {
        try {
            String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/GameLegacyServiceBridgeConfiguration.java"));

            assertTrue(source.contains("return new " + type.getSimpleName() + "();"));
        } catch (IOException e) {
            throw new AssertionError("Unable to read GameLegacyServiceBridgeConfiguration source", e);
        }
    }
}
