package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.services.AdminService;
import com.aionemu.gameserver.services.AnnouncementService;
import com.aionemu.gameserver.services.AStationService;
import com.aionemu.gameserver.services.BrokerService;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.CuringZoneService;
import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.DebugService;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.ExchangeService;
import com.aionemu.gameserver.services.F2pService;
import com.aionemu.gameserver.services.FlyRingService;
import com.aionemu.gameserver.services.GameTimeService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.KiskService;
import com.aionemu.gameserver.services.LimitedItemTradeService;
import com.aionemu.gameserver.services.MotionLoggingService;
import com.aionemu.gameserver.services.PeriodicSaveService;
import com.aionemu.gameserver.services.PetitionService;
import com.aionemu.gameserver.services.RepurchaseService;
import com.aionemu.gameserver.services.SpringZoneService;
import com.aionemu.gameserver.services.StaticDoorService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.services.AbyssLandingSpecialService;
import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.WeatherService;
import com.aionemu.gameserver.services.WeddingService;
import com.aionemu.gameserver.services.WindyGorgeService;
import com.aionemu.gameserver.services.abysslandingservice.LandingUpdateService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.drop.DropDistributionService;
import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.ArcadeUpgradeService;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.BoostEventService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
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
import com.aionemu.gameserver.services.player.GrowthEnergy;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.reward.BonusService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.services.toypet.PetService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.taskmanager.tasks.TeamEffectUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TeamMoveUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TemporaryTradeTimeTask;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class GameServiceProviderCompatibilityTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void singletonAccessorsUseSpringProvidersBeforeLegacyFallbacks() {
        GeoService geoService = instance(GeoService.class);
        NavService navService = instance(NavService.class);
        DropRegistrationService dropRegistrationService = instance(DropRegistrationService.class);
        LandingUpdateService landingUpdateService = instance(LandingUpdateService.class);
        AbyssLandingSpecialService abyssLandingSpecialService = instance(AbyssLandingSpecialService.class);
        DisputeLandService disputeLandService = instance(DisputeLandService.class);
        DredgionService2 dredgionService = instance(DredgionService2.class);
        AsyunatarService asyunatarService = instance(AsyunatarService.class);
        ShugoImperialTombSpawnManager shugoImperialTombSpawnManager = instance(ShugoImperialTombSpawnManager.class);
        SeasonRankingUpdateService seasonRankingUpdateService = instance(SeasonRankingUpdateService.class);
        PeriodicSaveService periodicSaveService = instance(PeriodicSaveService.class);
        AdminService adminService = instance(AdminService.class);
        PlayerTransferService playerTransferService = instance(PlayerTransferService.class);
        TerritoryService territoryService = instance(TerritoryService.class);
        GameTimeService gameTimeService = instance(GameTimeService.class);
        AnnouncementService announcementService = instance(AnnouncementService.class);
        DebugService debugService = instance(DebugService.class);
        WeatherService weatherService = instance(WeatherService.class);
        BrokerService brokerService = instance(BrokerService.class);
        Influence influence = instance(Influence.class);
        ExchangeService exchangeService = instance(ExchangeService.class);
        PetitionService petitionService = instance(PetitionService.class);
        FlyRingService flyRingService = instance(FlyRingService.class);
        CuringZoneService curingZoneService = instance(CuringZoneService.class);
        SpringZoneService springZoneService = instance(SpringZoneService.class);
        BoostEventService boostEventService = instance(BoostEventService.class);
        TaskManagerFromDB taskManagerFromDB = instance(TaskManagerFromDB.class);
        LimitedItemTradeService limitedItemTradeService = instance(LimitedItemTradeService.class);
        PlayerLimitService playerLimitService = instance(PlayerLimitService.class);
        NpcShoutsService npcShoutsService = instance(NpcShoutsService.class);
        ShieldService shieldService = instance(ShieldService.class);
        RewardService rewardService = instance(RewardService.class);
        WeddingService weddingService = instance(WeddingService.class);
        VeteranRewardsService veteranRewardsService = instance(VeteranRewardsService.class);
        ProtectorConquerorService protectorConquerorService = instance(ProtectorConquerorService.class);
        FFAService ffaService = instance(FFAService.class);
        LadderService ladderService = instance(LadderService.class);
        BGService bgService = instance(BGService.class);
        BanditService banditService = instance(BanditService.class);
        LunaShopService lunaShopService = instance(LunaShopService.class);
        MinionService minionService = instance(MinionService.class);
        ShugoSweepService shugoSweepService = instance(ShugoSweepService.class);
        AtreianPassportService atreianPassportService = instance(AtreianPassportService.class);
        EventWindowService eventWindowService = instance(EventWindowService.class);
        QuestEngine questEngine = instance(QuestEngine.class);
        InstanceEngine instanceEngine = instance(InstanceEngine.class);
        AI2Engine ai2Engine = instance(AI2Engine.class);
        ChatProcessor chatProcessor = instance(ChatProcessor.class);
        HTMLCache htmlCache = instance(HTMLCache.class);
        EventService eventService = instance(EventService.class);
        PlayerEventService playerEventService = instance(PlayerEventService.class);
        CrazyDaevaService crazyDaevaService = instance(CrazyDaevaService.class);
        AbyssRankUpdateService abyssRankUpdateService = instance(AbyssRankUpdateService.class);
        PacketBroadcaster packetBroadcaster = instance(PacketBroadcaster.class);
        DatabaseCleaningService databaseCleaningService = instance(DatabaseCleaningService.class);
        AbyssRankCleaningService abyssRankCleaningService = instance(AbyssRankCleaningService.class);
        ZoneService zoneService = instance(ZoneService.class);
        HotspotTeleportService hotspotTeleportService = instance(HotspotTeleportService.class);
        RoadService roadService = instance(RoadService.class);
        AStationService aStationService = instance(AStationService.class);
        F2pService f2pService = instance(F2pService.class);
        WindyGorgeService windyGorgeService = instance(WindyGorgeService.class);
        MotionLoggingService motionLoggingService = instance(MotionLoggingService.class);
        StaticDoorService staticDoorService = instance(StaticDoorService.class);
        KiskService kiskService = instance(KiskService.class);
        RepurchaseService repurchaseService = instance(RepurchaseService.class);
        DropDistributionService dropDistributionService = instance(DropDistributionService.class);
        SystemMailService systemMailService = instance(SystemMailService.class);
        BonusService bonusService = instance(BonusService.class);
        PetService petService = instance(PetService.class);
        ArcadeUpgradeService arcadeUpgradeService = instance(ArcadeUpgradeService.class);
        AtreianBestiaryService atreianBestiaryService = instance(AtreianBestiaryService.class);
        CoalescenceService coalescenceService = instance(CoalescenceService.class);
        GrowthEnergy growthEnergy = instance(GrowthEnergy.class);
        ExpireTimerTask expireTimerTask = instance(ExpireTimerTask.class);
        TeamEffectUpdater teamEffectUpdater = instance(TeamEffectUpdater.class);
        TeamMoveUpdater teamMoveUpdater = instance(TeamMoveUpdater.class);
        TemporaryTradeTimeTask temporaryTradeTimeTask = instance(TemporaryTradeTimeTask.class);
        HousingBidService housingBidService = instance(HousingBidService.class);
        MaintenanceTask maintenanceTask = instance(MaintenanceTask.class);
        TownService townService = instance(TownService.class);
        ChallengeTaskService challengeTaskService = instance(ChallengeTaskService.class);
        KamarBattlefieldService kamarBattlefieldService = instance(KamarBattlefieldService.class);
        EngulfedOphidanBridgeService engulfedOphidanBridgeService = instance(EngulfedOphidanBridgeService.class);
        SuspiciousOphidanBridgeService suspiciousOphidanBridgeService = instance(SuspiciousOphidanBridgeService.class);
        IronWallWarfrontService ironWallWarfrontService = instance(IronWallWarfrontService.class);
        IdgelDomeService idgelDomeService = instance(IdgelDomeService.class);
        IdgelDomeLandmarkService idgelDomeLandmarkService = instance(IdgelDomeLandmarkService.class);
        HallOfTenacityService hallOfTenacityService = instance(HallOfTenacityService.class);
        GrandArenaTrainingCampService grandArenaTrainingCampService = instance(GrandArenaTrainingCampService.class);
        IDRunService idRunService = instance(IDRunService.class);

        try {
            GeoService.setInstanceProvider(provider(GeoService.class, geoService));
            NavService.setInstanceProvider(provider(NavService.class, navService));
            DropRegistrationService.setInstanceProvider(provider(DropRegistrationService.class, dropRegistrationService));
            LandingUpdateService.setInstanceProvider(provider(LandingUpdateService.class, landingUpdateService));
            AbyssLandingSpecialService.setInstanceProvider(provider(AbyssLandingSpecialService.class, abyssLandingSpecialService));
            DisputeLandService.setInstanceProvider(provider(DisputeLandService.class, disputeLandService));
            DredgionService2.setInstanceProvider(provider(DredgionService2.class, dredgionService));
            AsyunatarService.setInstanceProvider(provider(AsyunatarService.class, asyunatarService));
            ShugoImperialTombSpawnManager.setInstanceProvider(provider(ShugoImperialTombSpawnManager.class, shugoImperialTombSpawnManager));
            SeasonRankingUpdateService.setInstanceProvider(provider(SeasonRankingUpdateService.class, seasonRankingUpdateService));
            PeriodicSaveService.setInstanceProvider(provider(PeriodicSaveService.class, periodicSaveService));
            AdminService.setInstanceProvider(provider(AdminService.class, adminService));
            PlayerTransferService.setInstanceProvider(provider(PlayerTransferService.class, playerTransferService));
            TerritoryService.setInstanceProvider(provider(TerritoryService.class, territoryService));
            GameTimeService.setInstanceProvider(provider(GameTimeService.class, gameTimeService));
            AnnouncementService.setInstanceProvider(provider(AnnouncementService.class, announcementService));
            DebugService.setInstanceProvider(provider(DebugService.class, debugService));
            WeatherService.setInstanceProvider(provider(WeatherService.class, weatherService));
            BrokerService.setInstanceProvider(provider(BrokerService.class, brokerService));
            Influence.setInstanceProvider(provider(Influence.class, influence));
            ExchangeService.setInstanceProvider(provider(ExchangeService.class, exchangeService));
            PetitionService.setInstanceProvider(provider(PetitionService.class, petitionService));
            FlyRingService.setInstanceProvider(provider(FlyRingService.class, flyRingService));
            CuringZoneService.setInstanceProvider(provider(CuringZoneService.class, curingZoneService));
            SpringZoneService.setInstanceProvider(provider(SpringZoneService.class, springZoneService));
            BoostEventService.setInstanceProvider(provider(BoostEventService.class, boostEventService));
            TaskManagerFromDB.setInstanceProvider(provider(TaskManagerFromDB.class, taskManagerFromDB));
            LimitedItemTradeService.setInstanceProvider(provider(LimitedItemTradeService.class, limitedItemTradeService));
            PlayerLimitService.setInstanceProvider(provider(PlayerLimitService.class, playerLimitService));
            NpcShoutsService.setInstanceProvider(provider(NpcShoutsService.class, npcShoutsService));
            ShieldService.setInstanceProvider(provider(ShieldService.class, shieldService));
            RewardService.setInstanceProvider(provider(RewardService.class, rewardService));
            WeddingService.setInstanceProvider(provider(WeddingService.class, weddingService));
            VeteranRewardsService.setInstanceProvider(provider(VeteranRewardsService.class, veteranRewardsService));
            ProtectorConquerorService.setInstanceProvider(provider(ProtectorConquerorService.class, protectorConquerorService));
            FFAService.setInstanceProvider(provider(FFAService.class, ffaService));
            LadderService.setInstanceProvider(provider(LadderService.class, ladderService));
            BGService.setInstanceProvider(provider(BGService.class, bgService));
            BanditService.setInstanceProvider(provider(BanditService.class, banditService));
            LunaShopService.setInstanceProvider(provider(LunaShopService.class, lunaShopService));
            MinionService.setInstanceProvider(provider(MinionService.class, minionService));
            ShugoSweepService.setInstanceProvider(provider(ShugoSweepService.class, shugoSweepService));
            AtreianPassportService.setInstanceProvider(provider(AtreianPassportService.class, atreianPassportService));
            EventWindowService.setInstanceProvider(provider(EventWindowService.class, eventWindowService));
            QuestEngine.setInstanceProvider(provider(QuestEngine.class, questEngine));
            InstanceEngine.setInstanceProvider(provider(InstanceEngine.class, instanceEngine));
            AI2Engine.setInstanceProvider(provider(AI2Engine.class, ai2Engine));
            ChatProcessor.setInstanceProvider(provider(ChatProcessor.class, chatProcessor));
            HTMLCache.setInstanceProvider(provider(HTMLCache.class, htmlCache));
            EventService.setInstanceProvider(provider(EventService.class, eventService));
            PlayerEventService.setInstanceProvider(provider(PlayerEventService.class, playerEventService));
            CrazyDaevaService.setInstanceProvider(provider(CrazyDaevaService.class, crazyDaevaService));
            AbyssRankUpdateService.setInstanceProvider(provider(AbyssRankUpdateService.class, abyssRankUpdateService));
            PacketBroadcaster.setInstanceProvider(provider(PacketBroadcaster.class, packetBroadcaster));
            DatabaseCleaningService.setInstanceProvider(provider(DatabaseCleaningService.class, databaseCleaningService));
            AbyssRankCleaningService.setInstanceProvider(provider(AbyssRankCleaningService.class, abyssRankCleaningService));
            ZoneService.setInstanceProvider(provider(ZoneService.class, zoneService));
            HotspotTeleportService.setInstanceProvider(provider(HotspotTeleportService.class, hotspotTeleportService));
            RoadService.setInstanceProvider(provider(RoadService.class, roadService));
            AStationService.setInstanceProvider(provider(AStationService.class, aStationService));
            F2pService.setInstanceProvider(provider(F2pService.class, f2pService));
            WindyGorgeService.setInstanceProvider(provider(WindyGorgeService.class, windyGorgeService));
            MotionLoggingService.setInstanceProvider(provider(MotionLoggingService.class, motionLoggingService));
            StaticDoorService.setInstanceProvider(provider(StaticDoorService.class, staticDoorService));
            KiskService.setInstanceProvider(provider(KiskService.class, kiskService));
            RepurchaseService.setInstanceProvider(provider(RepurchaseService.class, repurchaseService));
            DropDistributionService.setInstanceProvider(provider(DropDistributionService.class, dropDistributionService));
            SystemMailService.setInstanceProvider(provider(SystemMailService.class, systemMailService));
            BonusService.setInstanceProvider(provider(BonusService.class, bonusService));
            PetService.setInstanceProvider(provider(PetService.class, petService));
            ArcadeUpgradeService.setInstanceProvider(provider(ArcadeUpgradeService.class, arcadeUpgradeService));
            AtreianBestiaryService.setInstanceProvider(provider(AtreianBestiaryService.class, atreianBestiaryService));
            CoalescenceService.setInstanceProvider(provider(CoalescenceService.class, coalescenceService));
            GrowthEnergy.setInstanceProvider(provider(GrowthEnergy.class, growthEnergy));
            ExpireTimerTask.setInstanceProvider(provider(ExpireTimerTask.class, expireTimerTask));
            TeamEffectUpdater.setInstanceProvider(provider(TeamEffectUpdater.class, teamEffectUpdater));
            TeamMoveUpdater.setInstanceProvider(provider(TeamMoveUpdater.class, teamMoveUpdater));
            TemporaryTradeTimeTask.setInstanceProvider(provider(TemporaryTradeTimeTask.class, temporaryTradeTimeTask));
            HousingBidService.setInstanceProvider(provider(HousingBidService.class, housingBidService));
            MaintenanceTask.setInstanceProvider(provider(MaintenanceTask.class, maintenanceTask));
            TownService.setInstanceProvider(provider(TownService.class, townService));
            ChallengeTaskService.setInstanceProvider(provider(ChallengeTaskService.class, challengeTaskService));
            KamarBattlefieldService.setInstanceProvider(provider(KamarBattlefieldService.class, kamarBattlefieldService));
            EngulfedOphidanBridgeService.setInstanceProvider(provider(EngulfedOphidanBridgeService.class, engulfedOphidanBridgeService));
            SuspiciousOphidanBridgeService.setInstanceProvider(provider(SuspiciousOphidanBridgeService.class, suspiciousOphidanBridgeService));
            IronWallWarfrontService.setInstanceProvider(provider(IronWallWarfrontService.class, ironWallWarfrontService));
            IdgelDomeService.setInstanceProvider(provider(IdgelDomeService.class, idgelDomeService));
            IdgelDomeLandmarkService.setInstanceProvider(provider(IdgelDomeLandmarkService.class, idgelDomeLandmarkService));
            HallOfTenacityService.setInstanceProvider(provider(HallOfTenacityService.class, hallOfTenacityService));
            GrandArenaTrainingCampService.setInstanceProvider(provider(GrandArenaTrainingCampService.class, grandArenaTrainingCampService));
            IDRunService.setInstanceProvider(provider(IDRunService.class, idRunService));

            assertSame(geoService, GeoService.getInstance());
            assertSame(navService, NavService.getInstance());
            assertSame(dropRegistrationService, DropRegistrationService.getInstance());
            assertSame(landingUpdateService, LandingUpdateService.getInstance());
            assertSame(abyssLandingSpecialService, AbyssLandingSpecialService.getInstance());
            assertSame(disputeLandService, DisputeLandService.getInstance());
            assertSame(dredgionService, DredgionService2.getInstance());
            assertSame(asyunatarService, AsyunatarService.getInstance());
            assertSame(shugoImperialTombSpawnManager, ShugoImperialTombSpawnManager.getInstance());
            assertSame(seasonRankingUpdateService, SeasonRankingUpdateService.getInstance());
            assertSame(periodicSaveService, PeriodicSaveService.getInstance());
            assertSame(adminService, AdminService.getInstance());
            assertSame(playerTransferService, PlayerTransferService.getInstance());
            assertSame(territoryService, TerritoryService.getInstance());
            assertSame(gameTimeService, GameTimeService.getInstance());
            assertSame(announcementService, AnnouncementService.getInstance());
            assertSame(debugService, DebugService.getInstance());
            assertSame(weatherService, WeatherService.getInstance());
            assertSame(brokerService, BrokerService.getInstance());
            assertSame(influence, Influence.getInstance());
            assertSame(exchangeService, ExchangeService.getInstance());
            assertSame(petitionService, PetitionService.getInstance());
            assertSame(flyRingService, FlyRingService.getInstance());
            assertSame(curingZoneService, CuringZoneService.getInstance());
            assertSame(springZoneService, SpringZoneService.getInstance());
            assertSame(boostEventService, BoostEventService.getInstance());
            assertSame(taskManagerFromDB, TaskManagerFromDB.getInstance());
            assertSame(limitedItemTradeService, LimitedItemTradeService.getInstance());
            assertSame(playerLimitService, PlayerLimitService.getInstance());
            assertSame(npcShoutsService, NpcShoutsService.getInstance());
            assertSame(shieldService, ShieldService.getInstance());
            assertSame(rewardService, RewardService.getInstance());
            assertSame(weddingService, WeddingService.getInstance());
            assertSame(veteranRewardsService, VeteranRewardsService.getInstance());
            assertSame(protectorConquerorService, ProtectorConquerorService.getInstance());
            assertSame(ffaService, FFAService.getInstance());
            assertSame(ladderService, LadderService.getInstance());
            assertSame(bgService, BGService.getInstance());
            assertSame(banditService, BanditService.getInstance());
            assertSame(lunaShopService, LunaShopService.getInstance());
            assertSame(minionService, MinionService.getInstance());
            assertSame(shugoSweepService, ShugoSweepService.getInstance());
            assertSame(atreianPassportService, AtreianPassportService.getInstance());
            assertSame(eventWindowService, EventWindowService.getInstance());
            assertSame(questEngine, QuestEngine.getInstance());
            assertSame(instanceEngine, InstanceEngine.getInstance());
            assertSame(ai2Engine, AI2Engine.getInstance());
            assertSame(chatProcessor, ChatProcessor.getInstance());
            assertSame(htmlCache, HTMLCache.getInstance());
            assertSame(eventService, EventService.getInstance());
            assertSame(playerEventService, PlayerEventService.getInstance());
            assertSame(crazyDaevaService, CrazyDaevaService.getInstance());
            assertSame(abyssRankUpdateService, AbyssRankUpdateService.getInstance());
            assertSame(packetBroadcaster, PacketBroadcaster.getInstance());
            assertSame(databaseCleaningService, DatabaseCleaningService.getInstance());
            assertSame(abyssRankCleaningService, AbyssRankCleaningService.getInstance());
            assertSame(zoneService, ZoneService.getInstance());
            assertSame(hotspotTeleportService, HotspotTeleportService.getInstance());
            assertSame(roadService, RoadService.getInstance());
            assertSame(aStationService, AStationService.getInstance());
            assertSame(f2pService, F2pService.getInstance());
            assertSame(windyGorgeService, WindyGorgeService.getInstance());
            assertSame(motionLoggingService, MotionLoggingService.getInstance());
            assertSame(staticDoorService, StaticDoorService.getInstance());
            assertSame(kiskService, KiskService.getInstance());
            assertSame(repurchaseService, RepurchaseService.getInstance());
            assertSame(dropDistributionService, DropDistributionService.getInstance());
            assertSame(systemMailService, SystemMailService.getInstance());
            assertSame(bonusService, BonusService.getInstance());
            assertSame(petService, PetService.getInstance());
            assertSame(arcadeUpgradeService, ArcadeUpgradeService.getInstance());
            assertSame(atreianBestiaryService, AtreianBestiaryService.getInstance());
            assertSame(coalescenceService, CoalescenceService.getInstance());
            assertSame(growthEnergy, GrowthEnergy.getInstance());
            assertSame(expireTimerTask, ExpireTimerTask.getInstance());
            assertSame(teamEffectUpdater, TeamEffectUpdater.getInstance());
            assertSame(teamMoveUpdater, TeamMoveUpdater.getInstance());
            assertSame(temporaryTradeTimeTask, TemporaryTradeTimeTask.getInstance());
            assertSame(housingBidService, HousingBidService.getInstance());
            assertSame(maintenanceTask, MaintenanceTask.getInstance());
            assertSame(townService, TownService.getInstance());
            assertSame(challengeTaskService, ChallengeTaskService.getInstance());
            assertSame(kamarBattlefieldService, KamarBattlefieldService.getInstance());
            assertSame(engulfedOphidanBridgeService, EngulfedOphidanBridgeService.getInstance());
            assertSame(suspiciousOphidanBridgeService, SuspiciousOphidanBridgeService.getInstance());
            assertSame(ironWallWarfrontService, IronWallWarfrontService.getInstance());
            assertSame(idgelDomeService, IdgelDomeService.getInstance());
            assertSame(idgelDomeLandmarkService, IdgelDomeLandmarkService.getInstance());
            assertSame(hallOfTenacityService, HallOfTenacityService.getInstance());
            assertSame(grandArenaTrainingCampService, GrandArenaTrainingCampService.getInstance());
            assertSame(idRunService, IDRunService.getInstance());
        } finally {
            GeoService.setInstanceProvider(null);
            NavService.setInstanceProvider(null);
            DropRegistrationService.setInstanceProvider(null);
            LandingUpdateService.setInstanceProvider(null);
            AbyssLandingSpecialService.setInstanceProvider(null);
            DisputeLandService.setInstanceProvider(null);
            DredgionService2.setInstanceProvider(null);
            AsyunatarService.setInstanceProvider(null);
            ShugoImperialTombSpawnManager.setInstanceProvider(null);
            SeasonRankingUpdateService.setInstanceProvider(null);
            PeriodicSaveService.setInstanceProvider(null);
            AdminService.setInstanceProvider(null);
            PlayerTransferService.setInstanceProvider(null);
            TerritoryService.setInstanceProvider(null);
            GameTimeService.setInstanceProvider(null);
            AnnouncementService.setInstanceProvider(null);
            DebugService.setInstanceProvider(null);
            WeatherService.setInstanceProvider(null);
            BrokerService.setInstanceProvider(null);
            Influence.setInstanceProvider(null);
            ExchangeService.setInstanceProvider(null);
            PetitionService.setInstanceProvider(null);
            FlyRingService.setInstanceProvider(null);
            CuringZoneService.setInstanceProvider(null);
            SpringZoneService.setInstanceProvider(null);
            BoostEventService.setInstanceProvider(null);
            TaskManagerFromDB.setInstanceProvider(null);
            LimitedItemTradeService.setInstanceProvider(null);
            PlayerLimitService.setInstanceProvider(null);
            NpcShoutsService.setInstanceProvider(null);
            ShieldService.setInstanceProvider(null);
            RewardService.setInstanceProvider(null);
            WeddingService.setInstanceProvider(null);
            VeteranRewardsService.setInstanceProvider(null);
            ProtectorConquerorService.setInstanceProvider(null);
            FFAService.setInstanceProvider(null);
            LadderService.setInstanceProvider(null);
            BGService.setInstanceProvider(null);
            BanditService.setInstanceProvider(null);
            LunaShopService.setInstanceProvider(null);
            MinionService.setInstanceProvider(null);
            ShugoSweepService.setInstanceProvider(null);
            AtreianPassportService.setInstanceProvider(null);
            EventWindowService.setInstanceProvider(null);
            QuestEngine.setInstanceProvider(null);
            InstanceEngine.setInstanceProvider(null);
            AI2Engine.setInstanceProvider(null);
            ChatProcessor.setInstanceProvider(null);
            HTMLCache.setInstanceProvider(null);
            EventService.setInstanceProvider(null);
            PlayerEventService.setInstanceProvider(null);
            CrazyDaevaService.setInstanceProvider(null);
            AbyssRankUpdateService.setInstanceProvider(null);
            PacketBroadcaster.setInstanceProvider(null);
            DatabaseCleaningService.setInstanceProvider(null);
            AbyssRankCleaningService.setInstanceProvider(null);
            ZoneService.setInstanceProvider(null);
            HotspotTeleportService.setInstanceProvider(null);
            RoadService.setInstanceProvider(null);
            AStationService.setInstanceProvider(null);
            F2pService.setInstanceProvider(null);
            WindyGorgeService.setInstanceProvider(null);
            MotionLoggingService.setInstanceProvider(null);
            StaticDoorService.setInstanceProvider(null);
            KiskService.setInstanceProvider(null);
            RepurchaseService.setInstanceProvider(null);
            DropDistributionService.setInstanceProvider(null);
            SystemMailService.setInstanceProvider(null);
            BonusService.setInstanceProvider(null);
            PetService.setInstanceProvider(null);
            ArcadeUpgradeService.setInstanceProvider(null);
            AtreianBestiaryService.setInstanceProvider(null);
            CoalescenceService.setInstanceProvider(null);
            GrowthEnergy.setInstanceProvider(null);
            ExpireTimerTask.setInstanceProvider(null);
            TeamEffectUpdater.setInstanceProvider(null);
            TeamMoveUpdater.setInstanceProvider(null);
            TemporaryTradeTimeTask.setInstanceProvider(null);
            HousingBidService.setInstanceProvider(null);
            MaintenanceTask.setInstanceProvider(null);
            TownService.setInstanceProvider(null);
            ChallengeTaskService.setInstanceProvider(null);
            KamarBattlefieldService.setInstanceProvider(null);
            EngulfedOphidanBridgeService.setInstanceProvider(null);
            SuspiciousOphidanBridgeService.setInstanceProvider(null);
            IronWallWarfrontService.setInstanceProvider(null);
            IdgelDomeService.setInstanceProvider(null);
            IdgelDomeLandmarkService.setInstanceProvider(null);
            HallOfTenacityService.setInstanceProvider(null);
            GrandArenaTrainingCampService.setInstanceProvider(null);
            IDRunService.setInstanceProvider(null);
        }
    }

    @Test
    void gameFeatureServicesRegistersAndClearsPlayerActionProviders() throws Exception {
        BonusService bonusService = instance(BonusService.class);
        PetService petService = instance(PetService.class);
        ArcadeUpgradeService arcadeUpgradeService = instance(ArcadeUpgradeService.class);
        AtreianBestiaryService atreianBestiaryService = instance(AtreianBestiaryService.class);
        CoalescenceService coalescenceService = instance(CoalescenceService.class);
        GrowthEnergy growthEnergy = instance(GrowthEnergy.class);

        GameFeatureServices featureServices = new GameFeatureServices(
                provider(DisputeLandService.class, instance(DisputeLandService.class)),
                provider(DredgionService2.class, instance(DredgionService2.class)),
                provider(AsyunatarService.class, instance(AsyunatarService.class)),
                provider(PlayerLimitService.class, instance(PlayerLimitService.class)),
                provider(NpcShoutsService.class, instance(NpcShoutsService.class)),
                provider(ShieldService.class, instance(ShieldService.class)),
                provider(RewardService.class, instance(RewardService.class)),
                provider(WeddingService.class, instance(WeddingService.class)),
                provider(VeteranRewardsService.class, instance(VeteranRewardsService.class)),
                provider(ProtectorConquerorService.class, instance(ProtectorConquerorService.class)),
                provider(FFAService.class, instance(FFAService.class)),
                provider(LadderService.class, instance(LadderService.class)),
                provider(BGService.class, instance(BGService.class)),
                provider(BanditService.class, instance(BanditService.class)),
                provider(AStationService.class, instance(AStationService.class)),
                provider(F2pService.class, instance(F2pService.class)),
                provider(WindyGorgeService.class, instance(WindyGorgeService.class)),
                provider(MotionLoggingService.class, instance(MotionLoggingService.class)),
                provider(StaticDoorService.class, instance(StaticDoorService.class)),
                provider(KiskService.class, instance(KiskService.class)),
                provider(RepurchaseService.class, instance(RepurchaseService.class)),
                provider(DropDistributionService.class, instance(DropDistributionService.class)),
                provider(SystemMailService.class, instance(SystemMailService.class)),
                provider(BonusService.class, bonusService),
                provider(PetService.class, petService),
                provider(ArcadeUpgradeService.class, arcadeUpgradeService),
                provider(AtreianBestiaryService.class, atreianBestiaryService),
                provider(CoalescenceService.class, coalescenceService),
                provider(GrowthEnergy.class, growthEnergy));

        try {
            assertSame(bonusService, BonusService.getInstance());
            assertSame(petService, PetService.getInstance());
            assertSame(arcadeUpgradeService, ArcadeUpgradeService.getInstance());
            assertSame(atreianBestiaryService, AtreianBestiaryService.getInstance());
            assertSame(coalescenceService, CoalescenceService.getInstance());
            assertSame(growthEnergy, GrowthEnergy.getInstance());

            featureServices.destroy();
            featureServices = null;

            assertNotSame(bonusService, BonusService.getInstance());
            assertNotSame(petService, PetService.getInstance());
            assertNotSame(arcadeUpgradeService, ArcadeUpgradeService.getInstance());
            assertNotSame(atreianBestiaryService, AtreianBestiaryService.getInstance());
            assertNotSame(coalescenceService, CoalescenceService.getInstance());
            assertNotSame(growthEnergy, GrowthEnergy.getInstance());
        } finally {
            if (featureServices != null) {
                featureServices.destroy();
            }
            BonusService.setInstanceProvider(null);
            PetService.setInstanceProvider(null);
            ArcadeUpgradeService.setInstanceProvider(null);
            AtreianBestiaryService.setInstanceProvider(null);
            CoalescenceService.setInstanceProvider(null);
            GrowthEnergy.setInstanceProvider(null);
        }
    }

    @Test
    void gameTaskManagerServicesRegistersAndClearsTaskProviders() throws Exception {
        ExpireTimerTask expireTimerTask = instance(ExpireTimerTask.class);
        TeamEffectUpdater teamEffectUpdater = instance(TeamEffectUpdater.class);
        TeamMoveUpdater teamMoveUpdater = instance(TeamMoveUpdater.class);
        TemporaryTradeTimeTask temporaryTradeTimeTask = instance(TemporaryTradeTimeTask.class);
        GameTaskManagerServices taskManagerServices = new GameTaskManagerServices(
                provider(ExpireTimerTask.class, expireTimerTask),
                provider(TeamEffectUpdater.class, teamEffectUpdater),
                provider(TeamMoveUpdater.class, teamMoveUpdater),
                provider(TemporaryTradeTimeTask.class, temporaryTradeTimeTask));

        try {
            assertSame(expireTimerTask, ExpireTimerTask.getInstance());
            assertSame(teamEffectUpdater, TeamEffectUpdater.getInstance());
            assertSame(teamMoveUpdater, TeamMoveUpdater.getInstance());
            assertSame(temporaryTradeTimeTask, TemporaryTradeTimeTask.getInstance());

            taskManagerServices.destroy();
            taskManagerServices = null;

            assertProviderCleared(ExpireTimerTask.class);
            assertProviderCleared(TeamEffectUpdater.class);
            assertProviderCleared(TeamMoveUpdater.class);
            assertProviderCleared(TemporaryTradeTimeTask.class);
        } finally {
            if (taskManagerServices != null) {
                taskManagerServices.destroy();
            }
            ExpireTimerTask.setInstanceProvider(null);
            TeamEffectUpdater.setInstanceProvider(null);
            TeamMoveUpdater.setInstanceProvider(null);
            TemporaryTradeTimeTask.setInstanceProvider(null);
        }
    }

    private <T> T instance(Class<T> type) {
        return objenesis.newInstance(type);
    }

    private static void assertProviderCleared(Class<?> type) throws ReflectiveOperationException {
        Field field = type.getDeclaredField("instanceProvider");
        field.setAccessible(true);
        assertNull(field.get(null));
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
