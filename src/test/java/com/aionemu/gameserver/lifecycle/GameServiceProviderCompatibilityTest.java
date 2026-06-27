package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.lang.reflect.Field;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.services.AdminService;
import com.aionemu.gameserver.services.AnnouncementService;
import com.aionemu.gameserver.services.AbyssLandingService;
import com.aionemu.gameserver.services.AgentService;
import com.aionemu.gameserver.services.AnohaService;
import com.aionemu.gameserver.services.AStationService;
import com.aionemu.gameserver.services.AutoGroupService;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.BeritraService;
import com.aionemu.gameserver.services.BrokerService;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.ConquestService;
import com.aionemu.gameserver.services.CuringZoneService;
import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.DebugService;
import com.aionemu.gameserver.services.DynamicRiftService;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.ExchangeService;
import com.aionemu.gameserver.services.F2pService;
import com.aionemu.gameserver.services.FlyRingService;
import com.aionemu.gameserver.services.GameTimeService;
import com.aionemu.gameserver.services.FindGroupService;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.IdianDepthsService;
import com.aionemu.gameserver.services.InstanceRiftService;
import com.aionemu.gameserver.services.IuService;
import com.aionemu.gameserver.services.KiskService;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.DuelService;
import com.aionemu.gameserver.services.LifeStatsRestoreService;
import com.aionemu.gameserver.services.LimitedItemTradeService;
import com.aionemu.gameserver.services.MoltenusService;
import com.aionemu.gameserver.services.MotionLoggingService;
import com.aionemu.gameserver.services.NightmareCircusService;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.services.PeriodicSaveService;
import com.aionemu.gameserver.services.PetitionService;
import com.aionemu.gameserver.services.PvpService;
import com.aionemu.gameserver.services.RepurchaseService;
import com.aionemu.gameserver.services.RiftService;
import com.aionemu.gameserver.services.RvrService;
import com.aionemu.gameserver.services.SpringZoneService;
import com.aionemu.gameserver.services.StaticDoorService;
import com.aionemu.gameserver.services.SvsService;
import com.aionemu.gameserver.services.SurveyService;
import com.aionemu.gameserver.services.TowerOfEternityService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.services.VortexService;
import com.aionemu.gameserver.services.WebshopService;
import com.aionemu.gameserver.services.ZorshivDredgionService;
import com.aionemu.gameserver.services.AbyssLandingSpecialService;
import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.WeatherService;
import com.aionemu.gameserver.services.WeddingService;
import com.aionemu.gameserver.services.WindyGorgeService;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.abysslandingservice.LandingUpdateService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.services.craft.RelinquishCraftStatus;
import com.aionemu.gameserver.services.drop.DropService;
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
import com.aionemu.gameserver.services.events.ThievesGuildService;
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
import com.aionemu.gameserver.services.mail.MailService;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.services.siegeservice.BalaurAssaultService;
import com.aionemu.gameserver.services.siegeservice.BattlefieldUnionService;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
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
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.reward.BonusService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.rift.RiftManager;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.services.toypet.PetService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.TeamEffectUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TeamMoveUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TemporaryTradeTimeTask;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavData;
import com.aionemu.gameserver.world.geo.nav.NavService;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import com.aionemu.commons.utils.collections.IntObjectHashMap;

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
        CreativityEssenceService creativityEssenceService = instance(CreativityEssenceService.class);
        CreativitySkillService creativitySkillService = instance(CreativitySkillService.class);
        CreativityStatsService creativityStatsService = instance(CreativityStatsService.class);
        CreativityTransfoService creativityTransfoService = instance(CreativityTransfoService.class);
        Accuracy accuracy = instance(Accuracy.class);
        Agility agility = instance(Agility.class);
        Health health = instance(Health.class);
        Knowledge knowledge = instance(Knowledge.class);
        Power power = instance(Power.class);
        Precision precision = instance(Precision.class);
        Will will = instance(Will.class);
        CraftSkillUpdateService craftSkillUpdateService = instance(CraftSkillUpdateService.class);
        RelinquishCraftStatus relinquishCraftStatus = instance(RelinquishCraftStatus.class);
        DuelService duelService = instance(DuelService.class);
        LifeStatsRestoreService lifeStatsRestoreService = instance(LifeStatsRestoreService.class);
        SeasonRankingService seasonRankingService = instance(SeasonRankingService.class);
        RiftManager riftManager = instance(RiftManager.class);

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
            CreativityEssenceService.setInstanceProvider(provider(CreativityEssenceService.class, creativityEssenceService));
            CreativitySkillService.setInstanceProvider(provider(CreativitySkillService.class, creativitySkillService));
            CreativityStatsService.setInstanceProvider(provider(CreativityStatsService.class, creativityStatsService));
            CreativityTransfoService.setInstanceProvider(provider(CreativityTransfoService.class, creativityTransfoService));
            Accuracy.setInstanceProvider(provider(Accuracy.class, accuracy));
            Agility.setInstanceProvider(provider(Agility.class, agility));
            Health.setInstanceProvider(provider(Health.class, health));
            Knowledge.setInstanceProvider(provider(Knowledge.class, knowledge));
            Power.setInstanceProvider(provider(Power.class, power));
            Precision.setInstanceProvider(provider(Precision.class, precision));
            Will.setInstanceProvider(provider(Will.class, will));
            CraftSkillUpdateService.setInstanceProvider(provider(CraftSkillUpdateService.class, craftSkillUpdateService));
            RelinquishCraftStatus.setInstanceProvider(provider(RelinquishCraftStatus.class, relinquishCraftStatus));
            DuelService.setInstanceProvider(provider(DuelService.class, duelService));
            LifeStatsRestoreService.setInstanceProvider(provider(LifeStatsRestoreService.class, lifeStatsRestoreService));
            SeasonRankingService.setInstanceProvider(provider(SeasonRankingService.class, seasonRankingService));
            RiftManager.setInstanceProvider(provider(RiftManager.class, riftManager));

            assertSame(geoService, GeoService.getInstance());
            assertSame(geoService, GameWorldServices.geoService());
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
            assertSame(creativityEssenceService, CreativityEssenceService.getInstance());
            assertSame(creativitySkillService, CreativitySkillService.getInstance());
            assertSame(creativityStatsService, CreativityStatsService.getInstance());
            assertSame(creativityTransfoService, CreativityTransfoService.getInstance());
            assertSame(accuracy, Accuracy.getInstance());
            assertSame(agility, Agility.getInstance());
            assertSame(health, Health.getInstance());
            assertSame(knowledge, Knowledge.getInstance());
            assertSame(power, Power.getInstance());
            assertSame(precision, Precision.getInstance());
            assertSame(will, Will.getInstance());
            assertSame(craftSkillUpdateService, CraftSkillUpdateService.getInstance());
            assertSame(relinquishCraftStatus, RelinquishCraftStatus.getInstance());
            assertSame(duelService, DuelService.getInstance());
            assertSame(lifeStatsRestoreService, LifeStatsRestoreService.getInstance());
            assertSame(seasonRankingService, SeasonRankingService.getInstance());
            assertSame(riftManager, RiftManager.getInstance());
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
            CreativityEssenceService.setInstanceProvider(null);
            CreativitySkillService.setInstanceProvider(null);
            CreativityStatsService.setInstanceProvider(null);
            CreativityTransfoService.setInstanceProvider(null);
            Accuracy.setInstanceProvider(null);
            Agility.setInstanceProvider(null);
            Health.setInstanceProvider(null);
            Knowledge.setInstanceProvider(null);
            Power.setInstanceProvider(null);
            Precision.setInstanceProvider(null);
            Will.setInstanceProvider(null);
            CraftSkillUpdateService.setInstanceProvider(null);
            RelinquishCraftStatus.setInstanceProvider(null);
            DuelService.setInstanceProvider(null);
            LifeStatsRestoreService.setInstanceProvider(null);
            SeasonRankingService.setInstanceProvider(null);
            RiftManager.setInstanceProvider(null);
        }
    }

    @Test
    void gameFeatureServicesRegistersAndClearsPlayerActionProviders() throws Exception {
        BonusService bonusService = instance(BonusService.class);
        NpcShoutsService npcShoutsService = instance(NpcShoutsService.class);
        DredgionService2 dredgionService = instance(DredgionService2.class);
        AsyunatarService asyunatarService = instance(AsyunatarService.class);
        ShieldService shieldService = instance(ShieldService.class);
        WeddingService weddingService = instance(WeddingService.class);
        ProtectorConquerorService protectorConquerorService = instance(ProtectorConquerorService.class);
        PetService petService = instance(PetService.class);
        ArcadeUpgradeService arcadeUpgradeService = instance(ArcadeUpgradeService.class);
        AtreianBestiaryService atreianBestiaryService = instance(AtreianBestiaryService.class);
        CoalescenceService coalescenceService = instance(CoalescenceService.class);
        GrowthEnergy growthEnergy = instance(GrowthEnergy.class);
        SiegeService siegeService = instance(SiegeService.class);
        BaseService baseService = instance(BaseService.class);
        FFAService ffaService = instance(FFAService.class);
        LadderService ladderService = instance(LadderService.class);
        AStationService aStationService = instance(AStationService.class);
        MotionLoggingService motionLoggingService = instance(MotionLoggingService.class);
        KiskService kiskService = instance(KiskService.class);
        RepurchaseService repurchaseService = instance(RepurchaseService.class);
        DropDistributionService dropDistributionService = instance(DropDistributionService.class);
        SystemMailService systemMailService = instance(SystemMailService.class);

        GameFeatureServices featureServices = new GameFeatureServices(
                provider(DisputeLandService.class, instance(DisputeLandService.class)),
                provider(DredgionService2.class, dredgionService),
                provider(AsyunatarService.class, asyunatarService),
                provider(PlayerLimitService.class, instance(PlayerLimitService.class)),
                provider(NpcShoutsService.class, npcShoutsService),
                provider(ShieldService.class, shieldService),
                provider(RewardService.class, instance(RewardService.class)),
                provider(WeddingService.class, weddingService),
                provider(VeteranRewardsService.class, instance(VeteranRewardsService.class)),
                provider(ProtectorConquerorService.class, protectorConquerorService),
                provider(FFAService.class, ffaService),
                provider(LadderService.class, ladderService),
                provider(BGService.class, instance(BGService.class)),
                provider(BanditService.class, instance(BanditService.class)),
                provider(SiegeService.class, siegeService),
                provider(BaseService.class, baseService),
                provider(AStationService.class, aStationService),
                provider(F2pService.class, instance(F2pService.class)),
                provider(WindyGorgeService.class, instance(WindyGorgeService.class)),
                provider(MotionLoggingService.class, motionLoggingService),
                provider(StaticDoorService.class, instance(StaticDoorService.class)),
                provider(KiskService.class, kiskService),
                provider(RepurchaseService.class, repurchaseService),
                provider(DropDistributionService.class, dropDistributionService),
                provider(SystemMailService.class, systemMailService),
                provider(BonusService.class, bonusService),
                provider(PetService.class, petService),
                provider(ArcadeUpgradeService.class, arcadeUpgradeService),
                provider(AtreianBestiaryService.class, atreianBestiaryService),
                provider(CoalescenceService.class, coalescenceService),
                provider(GrowthEnergy.class, growthEnergy));

        try {
            assertSame(npcShoutsService, GameFeatureServices.npcShoutsService());
            assertSame(dredgionService, GameFeatureServices.dredgionService());
            assertSame(asyunatarService, GameFeatureServices.asyunatarService());
            assertSame(shieldService, GameFeatureServices.shieldService());
            assertSame(weddingService, GameFeatureServices.weddingService());
            assertSame(protectorConquerorService, GameFeatureServices.protectorConquerorService());
            assertSame(siegeService, GameFeatureServices.siegeService());
            assertSame(baseService, GameFeatureServices.baseService());
            assertSame(ffaService, GameFeatureServices.ffaService());
            assertSame(ladderService, GameFeatureServices.ladderService());
            assertSame(aStationService, GameFeatureServices.aStationService());
            assertSame(motionLoggingService, GameFeatureServices.motionLoggingService());
            assertSame(kiskService, GameFeatureServices.kiskService());
            assertSame(repurchaseService, GameFeatureServices.repurchaseService());
            assertSame(dropDistributionService, GameFeatureServices.dropDistributionService());
            assertSame(systemMailService, GameFeatureServices.systemMailService());
            assertSame(bonusService, BonusService.getInstance());
            assertSame(petService, PetService.getInstance());
            assertSame(petService, GameFeatureServices.petService());
            assertSame(arcadeUpgradeService, ArcadeUpgradeService.getInstance());
            assertSame(arcadeUpgradeService, GameFeatureServices.arcadeUpgradeService());
            assertSame(atreianBestiaryService, AtreianBestiaryService.getInstance());
            assertSame(atreianBestiaryService, GameFeatureServices.atreianBestiaryService());
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
            assertProviderCleared(SystemMailService.class);
            assertProviderCleared(FFAService.class);
            assertProviderCleared(LadderService.class);
            assertProviderCleared(KiskService.class);
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
            SystemMailService.setInstanceProvider(null);
            FFAService.setInstanceProvider(null);
            LadderService.setInstanceProvider(null);
            KiskService.setInstanceProvider(null);
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
            assertSame(expireTimerTask, GameTaskManagerServices.expireTimerTask());
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

    @Test
    void gameEventServicesRegisterAndClearEventAccessors() throws Exception {
        EventService eventService = instance(EventService.class);
        PlayerEventService playerEventService = instance(PlayerEventService.class);
        CrazyDaevaService crazyDaevaService = instance(CrazyDaevaService.class);
        AbyssRankUpdateService abyssRankUpdateService = instance(AbyssRankUpdateService.class);
        PacketBroadcaster packetBroadcaster = instance(PacketBroadcaster.class);
        LunaShopService lunaShopService = instance(LunaShopService.class);
        MinionService minionService = instance(MinionService.class);
        ShugoSweepService shugoSweepService = instance(ShugoSweepService.class);
        AtreianPassportService atreianPassportService = instance(AtreianPassportService.class);
        EventWindowService eventWindowService = instance(EventWindowService.class);
        GameEventServices eventServices = new GameEventServices(
                provider(EventService.class, eventService),
                provider(PlayerEventService.class, playerEventService),
                provider(CrazyDaevaService.class, crazyDaevaService),
                provider(AbyssRankUpdateService.class, abyssRankUpdateService),
                provider(PacketBroadcaster.class, packetBroadcaster));
        GameEventBootstrapServices eventBootstrapServices = new GameEventBootstrapServices(
                provider(LunaShopService.class, lunaShopService),
                provider(MinionService.class, minionService),
                provider(ShugoSweepService.class, shugoSweepService),
                provider(AtreianPassportService.class, atreianPassportService),
                provider(EventWindowService.class, eventWindowService));

        try {
            assertSame(eventService, GameEventServices.eventService());
            assertSame(playerEventService, GameEventServices.playerEventService());
            assertSame(crazyDaevaService, GameEventServices.crazyDaevaService());
            assertSame(abyssRankUpdateService, GameEventServices.abyssRankUpdateService());
            assertSame(packetBroadcaster, GameEventServices.packetBroadcaster());
            assertSame(lunaShopService, GameEventBootstrapServices.lunaShopService());
            assertSame(minionService, GameEventBootstrapServices.minionService());
            assertSame(shugoSweepService, GameEventBootstrapServices.shugoSweepService());
            assertSame(atreianPassportService, GameEventBootstrapServices.atreianPassportService());
            assertSame(eventWindowService, GameEventBootstrapServices.eventWindowService());

            eventServices.destroy();
            eventServices = null;
            eventBootstrapServices.destroy();
            eventBootstrapServices = null;

            assertProviderCleared(EventService.class);
            assertProviderCleared(PlayerEventService.class);
            assertProviderCleared(CrazyDaevaService.class);
            assertProviderCleared(AbyssRankUpdateService.class);
            assertProviderCleared(PacketBroadcaster.class);
            assertProviderCleared(MinionService.class);
            assertProviderCleared(ShugoSweepService.class);
            assertProviderCleared(AtreianPassportService.class);
            assertProviderCleared(EventWindowService.class);
        } finally {
            if (eventServices != null) {
                eventServices.destroy();
            }
            if (eventBootstrapServices != null) {
                eventBootstrapServices.destroy();
            }
            EventService.setInstanceProvider(null);
            PlayerEventService.setInstanceProvider(null);
            CrazyDaevaService.setInstanceProvider(null);
            AbyssRankUpdateService.setInstanceProvider(null);
            PacketBroadcaster.setInstanceProvider(null);
            LunaShopService.setInstanceProvider(null);
            MinionService.setInstanceProvider(null);
            ShugoSweepService.setInstanceProvider(null);
            AtreianPassportService.setInstanceProvider(null);
            EventWindowService.setInstanceProvider(null);
        }
    }

    @Test
    void gameCreativityServicesRegistersAndClearsCreativityProviders() throws Exception {
        CreativityEssenceService creativityEssenceService = instance(CreativityEssenceService.class);
        CreativitySkillService creativitySkillService = instance(CreativitySkillService.class);
        CreativityStatsService creativityStatsService = instance(CreativityStatsService.class);
        CreativityTransfoService creativityTransfoService = instance(CreativityTransfoService.class);
        Accuracy accuracy = instance(Accuracy.class);
        Agility agility = instance(Agility.class);
        Health health = instance(Health.class);
        Knowledge knowledge = instance(Knowledge.class);
        Power power = instance(Power.class);
        Precision precision = instance(Precision.class);
        Will will = instance(Will.class);
        GameCreativityServices creativityServices = new GameCreativityServices(
                provider(CreativityEssenceService.class, creativityEssenceService),
                provider(CreativitySkillService.class, creativitySkillService),
                provider(CreativityStatsService.class, creativityStatsService),
                provider(CreativityTransfoService.class, creativityTransfoService),
                provider(Accuracy.class, accuracy),
                provider(Agility.class, agility),
                provider(Health.class, health),
                provider(Knowledge.class, knowledge),
                provider(Power.class, power),
                provider(Precision.class, precision),
                provider(Will.class, will));

        try {
            assertSame(creativityEssenceService, CreativityEssenceService.getInstance());
            assertSame(creativitySkillService, CreativitySkillService.getInstance());
            assertSame(creativityStatsService, CreativityStatsService.getInstance());
            assertSame(creativityTransfoService, CreativityTransfoService.getInstance());
            assertSame(accuracy, Accuracy.getInstance());
            assertSame(agility, Agility.getInstance());
            assertSame(health, Health.getInstance());
            assertSame(knowledge, Knowledge.getInstance());
            assertSame(power, Power.getInstance());
            assertSame(precision, Precision.getInstance());
            assertSame(will, Will.getInstance());

            creativityServices.destroy();
            creativityServices = null;

            assertProviderCleared(CreativityEssenceService.class);
            assertProviderCleared(CreativitySkillService.class);
            assertProviderCleared(CreativityStatsService.class);
            assertProviderCleared(CreativityTransfoService.class);
            assertProviderCleared(Accuracy.class);
            assertProviderCleared(Agility.class);
            assertProviderCleared(Health.class);
            assertProviderCleared(Knowledge.class);
            assertProviderCleared(Power.class);
            assertProviderCleared(Precision.class);
            assertProviderCleared(Will.class);
        } finally {
            if (creativityServices != null) {
                creativityServices.destroy();
            }
            CreativityEssenceService.setInstanceProvider(null);
            CreativitySkillService.setInstanceProvider(null);
            CreativityStatsService.setInstanceProvider(null);
            CreativityTransfoService.setInstanceProvider(null);
            Accuracy.setInstanceProvider(null);
            Agility.setInstanceProvider(null);
            Health.setInstanceProvider(null);
            Knowledge.setInstanceProvider(null);
            Power.setInstanceProvider(null);
            Precision.setInstanceProvider(null);
            Will.setInstanceProvider(null);
        }
    }

    @Test
    void gameCraftServicesRegistersAndClearsCraftProviders() throws Exception {
        CraftSkillUpdateService craftSkillUpdateService = instance(CraftSkillUpdateService.class);
        RelinquishCraftStatus relinquishCraftStatus = instance(RelinquishCraftStatus.class);
        GameCraftServices craftServices = new GameCraftServices(
                provider(CraftSkillUpdateService.class, craftSkillUpdateService),
                provider(RelinquishCraftStatus.class, relinquishCraftStatus));

        try {
            assertSame(craftSkillUpdateService, CraftSkillUpdateService.getInstance());
            assertSame(relinquishCraftStatus, RelinquishCraftStatus.getInstance());

            craftServices.destroy();
            craftServices = null;

            assertProviderCleared(CraftSkillUpdateService.class);
            assertProviderCleared(RelinquishCraftStatus.class);
        } finally {
            if (craftServices != null) {
                craftServices.destroy();
            }
            CraftSkillUpdateService.setInstanceProvider(null);
            RelinquishCraftStatus.setInstanceProvider(null);
        }
    }

    @Test
    void gameGameplayServicesRegistersAndClearsLightweightRuntimeProviders() throws Exception {
        DuelService duelService = instance(DuelService.class);
        LifeStatsRestoreService lifeStatsRestoreService = instance(LifeStatsRestoreService.class);
        SeasonRankingService seasonRankingService = instance(SeasonRankingService.class);
        RiftManager riftManager = instance(RiftManager.class);
        GameGameplayServices gameplayServices = new GameGameplayServices(
                provider(DuelService.class, duelService),
                provider(LifeStatsRestoreService.class, lifeStatsRestoreService),
                provider(SeasonRankingService.class, seasonRankingService),
                provider(RiftManager.class, riftManager));

        try {
            assertSame(duelService, DuelService.getInstance());
            assertSame(duelService, GameGameplayServices.duelService());
            assertSame(lifeStatsRestoreService, LifeStatsRestoreService.getInstance());
            assertSame(seasonRankingService, SeasonRankingService.getInstance());
            assertSame(riftManager, RiftManager.getInstance());

            gameplayServices.destroy();
            gameplayServices = null;

            assertProviderCleared(DuelService.class);
            assertProviderCleared(LifeStatsRestoreService.class);
            assertProviderCleared(SeasonRankingService.class);
            assertProviderCleared(RiftManager.class);
        } finally {
            if (gameplayServices != null) {
                gameplayServices.destroy();
            }
            DuelService.setInstanceProvider(null);
            LifeStatsRestoreService.setInstanceProvider(null);
            SeasonRankingService.setInstanceProvider(null);
            RiftManager.setInstanceProvider(null);
        }
    }

    @Test
    void gameCoreGameplayServicesRegistersAndClearsCoreOnlineProviders() throws Exception {
        DropService dropService = instance(DropService.class);
        MailService mailService = instance(MailService.class);
        PvpService pvpService = instance(PvpService.class);
        AutoGroupService autoGroupService = instance(AutoGroupService.class);
        AbyssRankingCache abyssRankingCache = instance(AbyssRankingCache.class);
        LegionService legionService = instance(LegionService.class);
        ThievesGuildService thievesGuildService = instance(ThievesGuildService.class);
        BalaurAssaultService balaurAssaultService = instance(BalaurAssaultService.class);
        BattlefieldUnionService battlefieldUnionService = instance(BattlefieldUnionService.class);
        GameCoreGameplayServices coreGameplayServices = new GameCoreGameplayServices(
                provider(DropService.class, dropService),
                provider(MailService.class, mailService),
                provider(PvpService.class, pvpService),
                provider(AutoGroupService.class, autoGroupService),
                provider(AbyssRankingCache.class, abyssRankingCache),
                provider(LegionService.class, legionService),
                provider(ThievesGuildService.class, thievesGuildService),
                provider(BalaurAssaultService.class, balaurAssaultService),
                provider(BattlefieldUnionService.class, battlefieldUnionService));

        try {
            assertSame(dropService, DropService.getInstance());
            assertSame(dropService, GameCoreGameplayServices.dropService());
            assertSame(mailService, MailService.getInstance());
            assertSame(mailService, GameCoreGameplayServices.mailService());
            assertSame(pvpService, PvpService.getInstance());
            assertSame(pvpService, GameCoreGameplayServices.pvpService());
            assertSame(autoGroupService, AutoGroupService.getInstance());
            assertSame(autoGroupService, GameCoreGameplayServices.autoGroupService());
            assertSame(abyssRankingCache, AbyssRankingCache.getInstance());
            assertSame(abyssRankingCache, GameCoreGameplayServices.abyssRankingCache());
            assertSame(legionService, LegionService.getInstance());
            assertSame(legionService, GameCoreGameplayServices.legionService());
            assertSame(thievesGuildService, ThievesGuildService.getInstance());
            assertSame(balaurAssaultService, BalaurAssaultService.getInstance());
            assertSame(balaurAssaultService, GameCoreGameplayServices.balaurAssaultService());
            assertSame(battlefieldUnionService, BattlefieldUnionService.getInstance());
            assertSame(battlefieldUnionService, GameCoreGameplayServices.battlefieldUnionService());

            coreGameplayServices.destroy();
            coreGameplayServices = null;

            assertProviderCleared(DropService.class);
            assertProviderCleared(MailService.class);
            assertProviderCleared(PvpService.class);
            assertProviderCleared(AutoGroupService.class);
            assertProviderCleared(AbyssRankingCache.class);
            assertProviderCleared(LegionService.class);
            assertProviderCleared(ThievesGuildService.class);
            assertProviderCleared(BalaurAssaultService.class);
            assertProviderCleared(BattlefieldUnionService.class);
        } finally {
            if (coreGameplayServices != null) {
                coreGameplayServices.destroy();
            }
            DropService.setInstanceProvider(null);
            MailService.setInstanceProvider(null);
            PvpService.setInstanceProvider(null);
            AutoGroupService.setInstanceProvider(null);
            AbyssRankingCache.setInstanceProvider(null);
            LegionService.setInstanceProvider(null);
            ThievesGuildService.setInstanceProvider(null);
            BalaurAssaultService.setInstanceProvider(null);
            BattlefieldUnionService.setInstanceProvider(null);
        }
    }

    @Test
    void gameLocationBootstrapServicesRegistersAndClearsAbyssLandingProvider() {
        VortexService vortexService = instance(VortexService.class);
        BeritraService beritraService = instance(BeritraService.class);
        AgentService agentService = instance(AgentService.class);
        AnohaService anohaService = instance(AnohaService.class);
        SvsService svsService = instance(SvsService.class);
        RvrService rvrService = instance(RvrService.class);
        IuService iuService = instance(IuService.class);
        NightmareCircusService nightmareCircusService = instance(NightmareCircusService.class);
        DynamicRiftService dynamicRiftService = instance(DynamicRiftService.class);
        InstanceRiftService instanceRiftService = instance(InstanceRiftService.class);
        OutpostService outpostService = instance(OutpostService.class);
        ZorshivDredgionService zorshivDredgionService = instance(ZorshivDredgionService.class);
        MoltenusService moltenusService = instance(MoltenusService.class);
        RiftService riftService = instance(RiftService.class);
        ConquestService conquestService = instance(ConquestService.class);
        IdianDepthsService idianDepthsService = instance(IdianDepthsService.class);
        AbyssLandingService abyssLandingService = instance(AbyssLandingService.class);
        AbyssLandingSpecialService abyssLandingSpecialService = instance(AbyssLandingSpecialService.class);
        GameLocationBootstrapServices locationServices = new GameLocationBootstrapServices(
                provider(VortexService.class, vortexService),
                provider(BeritraService.class, beritraService),
                provider(AgentService.class, agentService),
                provider(AnohaService.class, anohaService),
                provider(SvsService.class, svsService),
                provider(RvrService.class, rvrService),
                provider(IuService.class, iuService),
                provider(NightmareCircusService.class, nightmareCircusService),
                provider(DynamicRiftService.class, dynamicRiftService),
                provider(InstanceRiftService.class, instanceRiftService),
                provider(SiegeService.class, instance(SiegeService.class)),
                provider(BaseService.class, instance(BaseService.class)),
                provider(OutpostService.class, outpostService),
                provider(ZorshivDredgionService.class, zorshivDredgionService),
                provider(MoltenusService.class, moltenusService),
                provider(RiftService.class, riftService),
                provider(ConquestService.class, conquestService),
                provider(IdianDepthsService.class, idianDepthsService),
                provider(TowerOfEternityService.class, instance(TowerOfEternityService.class)),
                provider(AbyssLandingService.class, abyssLandingService),
                provider(LandingUpdateService.class, instance(LandingUpdateService.class)),
                provider(AbyssLandingSpecialService.class, abyssLandingSpecialService));

        try {
            assertSame(vortexService, GameLocationBootstrapServices.vortexService());
            assertSame(beritraService, GameLocationBootstrapServices.beritraService());
            assertSame(agentService, GameLocationBootstrapServices.agentService());
            assertSame(anohaService, GameLocationBootstrapServices.anohaService());
            assertSame(svsService, GameLocationBootstrapServices.svsService());
            assertSame(rvrService, GameLocationBootstrapServices.rvrService());
            assertSame(iuService, GameLocationBootstrapServices.iuService());
            assertSame(nightmareCircusService, GameLocationBootstrapServices.nightmareCircusService());
            assertSame(dynamicRiftService, GameLocationBootstrapServices.dynamicRiftService());
            assertSame(instanceRiftService, GameLocationBootstrapServices.instanceRiftService());
            assertSame(outpostService, GameLocationBootstrapServices.outpostService());
            assertSame(zorshivDredgionService, GameLocationBootstrapServices.zorshivDredgionService());
            assertSame(moltenusService, GameLocationBootstrapServices.moltenusService());
            assertSame(riftService, GameLocationBootstrapServices.riftService());
            assertSame(conquestService, GameLocationBootstrapServices.conquestService());
            assertSame(idianDepthsService, GameLocationBootstrapServices.idianDepthsService());
            assertSame(abyssLandingService, GameLocationBootstrapServices.abyssLandingService());
            assertSame(abyssLandingSpecialService, GameLocationBootstrapServices.abyssLandingSpecialService());
        } finally {
            locationServices.destroy();
            assertNotSame(vortexService, GameLocationBootstrapServices.vortexService());
            assertNotSame(beritraService, GameLocationBootstrapServices.beritraService());
            assertNotSame(agentService, GameLocationBootstrapServices.agentService());
            assertNotSame(anohaService, GameLocationBootstrapServices.anohaService());
            assertNotSame(svsService, GameLocationBootstrapServices.svsService());
            assertNotSame(rvrService, GameLocationBootstrapServices.rvrService());
            assertNotSame(iuService, GameLocationBootstrapServices.iuService());
            assertNotSame(nightmareCircusService, GameLocationBootstrapServices.nightmareCircusService());
            assertNotSame(dynamicRiftService, GameLocationBootstrapServices.dynamicRiftService());
            assertNotSame(instanceRiftService, GameLocationBootstrapServices.instanceRiftService());
            assertNotSame(outpostService, GameLocationBootstrapServices.outpostService());
            assertNotSame(zorshivDredgionService, GameLocationBootstrapServices.zorshivDredgionService());
            assertNotSame(moltenusService, GameLocationBootstrapServices.moltenusService());
            assertNotSame(riftService, GameLocationBootstrapServices.riftService());
            assertNotSame(conquestService, GameLocationBootstrapServices.conquestService());
            assertNotSame(idianDepthsService, GameLocationBootstrapServices.idianDepthsService());
            assertNotSame(abyssLandingService, GameLocationBootstrapServices.abyssLandingService());
            assertNotSame(abyssLandingSpecialService, GameLocationBootstrapServices.abyssLandingSpecialService());
        }
    }

    @Test
    void gameMovementLoopServicesRegistersAndClearsMovementLoopProviders() throws Exception {
        WorldMapsData oldWorldMapsData = DataManager.WORLD_MAPS_DATA;
        GameMovementLoopServices movementLoopServices = null;
        try {
            DataManager.WORLD_MAPS_DATA = worldMaps(1001);
            MovementNotifyTask movementNotifyTask = instance(MovementNotifyTask.class);
            MoveTaskManager moveTaskManager = instance(MoveTaskManager.class);
            PlayerMoveTaskManager playerMoveTaskManager = instance(PlayerMoveTaskManager.class);
            ZoneUpdateService zoneUpdateService = instance(ZoneUpdateService.class);
            movementLoopServices = new GameMovementLoopServices(
                    provider(MovementNotifyTask.class, movementNotifyTask),
                    provider(MoveTaskManager.class, moveTaskManager),
                    provider(PlayerMoveTaskManager.class, playerMoveTaskManager),
                    provider(ZoneUpdateService.class, zoneUpdateService));

            assertSame(movementNotifyTask, MovementNotifyTask.getInstance());
            assertSame(movementNotifyTask, GameMovementLoopServices.movementNotifyTask());
            assertSame(moveTaskManager, MoveTaskManager.getInstance());
            assertSame(moveTaskManager, GameMovementLoopServices.moveTaskManager());
            assertSame(playerMoveTaskManager, PlayerMoveTaskManager.getInstance());
            assertSame(playerMoveTaskManager, GameMovementLoopServices.playerMoveTaskManager());
            assertSame(zoneUpdateService, ZoneUpdateService.getInstance());
            assertSame(zoneUpdateService, GameMovementLoopServices.zoneUpdateService());

            movementLoopServices.destroy();
            movementLoopServices = null;

            assertProviderCleared(MovementNotifyTask.class);
            assertProviderCleared(MoveTaskManager.class);
            assertProviderCleared(PlayerMoveTaskManager.class);
            assertProviderCleared(ZoneUpdateService.class);
        } finally {
            if (movementLoopServices != null) {
                movementLoopServices.destroy();
            }
            MovementNotifyTask.setInstanceProvider(null);
            MoveTaskManager.setInstanceProvider(null);
            PlayerMoveTaskManager.setInstanceProvider(null);
            ZoneUpdateService.setInstanceProvider(null);
            DataManager.WORLD_MAPS_DATA = oldWorldMapsData;
        }
    }

    @Test
    void gameHousingServicesRegistersAndClearsHousingProvider() throws Exception {
        HousingBidService housingBidService = instance(HousingBidService.class);
        MaintenanceTask maintenanceTask = instance(MaintenanceTask.class);
        TownService townService = instance(TownService.class);
        HousingService housingService = instance(HousingService.class);
        GameHousingServices housingServices = new GameHousingServices(
                provider(HousingBidService.class, housingBidService),
                provider(MaintenanceTask.class, maintenanceTask),
                provider(TownService.class, townService),
                provider(HousingService.class, housingService),
                provider(ChallengeTaskService.class, instance(ChallengeTaskService.class)));

        try {
            assertSame(housingBidService, GameHousingServices.housingBidService());
            assertSame(maintenanceTask, GameHousingServices.maintenanceTask());
            assertSame(townService, GameHousingServices.townService());
            assertSame(housingService, GameHousingServices.housingService());
        } finally {
            housingServices.destroy();
            assertProviderCleared(HousingBidService.class);
            assertProviderCleared(MaintenanceTask.class);
            assertProviderCleared(TownService.class);
            assertProviderCleared(HousingService.class);
        }
    }

    @Test
    void gameRuntimeServicesRegistersAndClearsHighTrafficAccessors() throws Exception {
        AdminService adminService = instance(AdminService.class);
        PlayerTransferService playerTransferService = instance(PlayerTransferService.class);
        TerritoryService territoryService = instance(TerritoryService.class);
        WeatherService weatherService = instance(WeatherService.class);
        BrokerService brokerService = instance(BrokerService.class);
        Influence influence = instance(Influence.class);
        ExchangeService exchangeService = instance(ExchangeService.class);
        PetitionService petitionService = instance(PetitionService.class);
        LimitedItemTradeService limitedItemTradeService = instance(LimitedItemTradeService.class);
        SurveyService surveyService = instance(SurveyService.class);
        GameRuntimeServices runtimeServices = new GameRuntimeServices(
                provider(PeriodicSaveService.class, instance(PeriodicSaveService.class)),
                provider(AdminService.class, adminService),
                provider(PlayerTransferService.class, playerTransferService),
                provider(TerritoryService.class, territoryService),
                provider(GameTimeService.class, instance(GameTimeService.class)),
                provider(AnnouncementService.class, instance(AnnouncementService.class)),
                provider(DebugService.class, instance(DebugService.class)),
                provider(WeatherService.class, weatherService),
                provider(BrokerService.class, brokerService),
                provider(Influence.class, influence),
                provider(ExchangeService.class, exchangeService),
                provider(PetitionService.class, petitionService),
                provider(FlyRingService.class, instance(FlyRingService.class)),
                provider(CuringZoneService.class, instance(CuringZoneService.class)),
                provider(SpringZoneService.class, instance(SpringZoneService.class)),
                provider(BoostEventService.class, instance(BoostEventService.class)),
                provider(TaskManagerFromDB.class, instance(TaskManagerFromDB.class)),
                provider(LimitedItemTradeService.class, limitedItemTradeService),
                provider(WebshopService.class, instance(WebshopService.class)),
                provider(SurveyService.class, surveyService),
                provider(FindGroupService.class, instance(FindGroupService.class)),
                provider(InGameShopEn.class, instance(InGameShopEn.class)));

        try {
            assertSame(adminService, GameRuntimeServices.adminService());
            assertSame(playerTransferService, GameRuntimeServices.playerTransferService());
            assertSame(territoryService, GameRuntimeServices.territoryService());
            assertSame(weatherService, GameRuntimeServices.weatherService());
            assertSame(brokerService, GameRuntimeServices.brokerService());
            assertSame(influence, GameRuntimeServices.influence());
            assertSame(exchangeService, GameRuntimeServices.exchangeService());
            assertSame(petitionService, GameRuntimeServices.petitionService());
            assertSame(limitedItemTradeService, GameRuntimeServices.limitedItemTradeService());
            assertSame(surveyService, GameRuntimeServices.surveyService());
        } finally {
            runtimeServices.destroy();
            assertProviderCleared(AdminService.class);
            assertProviderCleared(PlayerTransferService.class);
            assertProviderCleared(TerritoryService.class);
            assertProviderCleared(WeatherService.class);
            assertProviderCleared(BrokerService.class);
            assertProviderCleared(Influence.class);
            assertProviderCleared(ExchangeService.class);
            assertProviderCleared(PetitionService.class);
            assertProviderCleared(LimitedItemTradeService.class);
            assertProviderCleared(SurveyService.class);
        }
    }

    @Test
    void gameBattlefieldServicesRegistersAndClearsBattlefieldAccessors() throws Exception {
        KamarBattlefieldService kamarBattlefieldService = instance(KamarBattlefieldService.class);
        EngulfedOphidanBridgeService engulfedOphidanBridgeService = instance(EngulfedOphidanBridgeService.class);
        SuspiciousOphidanBridgeService suspiciousOphidanBridgeService = instance(SuspiciousOphidanBridgeService.class);
        IronWallWarfrontService ironWallWarfrontService = instance(IronWallWarfrontService.class);
        IdgelDomeService idgelDomeService = instance(IdgelDomeService.class);
        IdgelDomeLandmarkService idgelDomeLandmarkService = instance(IdgelDomeLandmarkService.class);
        HallOfTenacityService hallOfTenacityService = instance(HallOfTenacityService.class);
        GrandArenaTrainingCampService grandArenaTrainingCampService = instance(GrandArenaTrainingCampService.class);
        IDRunService idRunService = instance(IDRunService.class);
        GameBattlefieldServices battlefieldServices = new GameBattlefieldServices(
                provider(KamarBattlefieldService.class, kamarBattlefieldService),
                provider(EngulfedOphidanBridgeService.class, engulfedOphidanBridgeService),
                provider(SuspiciousOphidanBridgeService.class, suspiciousOphidanBridgeService),
                provider(IronWallWarfrontService.class, ironWallWarfrontService),
                provider(IdgelDomeService.class, idgelDomeService),
                provider(IdgelDomeLandmarkService.class, idgelDomeLandmarkService),
                provider(HallOfTenacityService.class, hallOfTenacityService),
                provider(GrandArenaTrainingCampService.class, grandArenaTrainingCampService),
                provider(IDRunService.class, idRunService));

        try {
            assertSame(kamarBattlefieldService, GameBattlefieldServices.kamarBattlefieldService());
            assertSame(engulfedOphidanBridgeService, GameBattlefieldServices.engulfedOphidanBridgeService());
            assertSame(suspiciousOphidanBridgeService, GameBattlefieldServices.suspiciousOphidanBridgeService());
            assertSame(ironWallWarfrontService, GameBattlefieldServices.ironWallWarfrontService());
            assertSame(idgelDomeService, GameBattlefieldServices.idgelDomeService());
            assertSame(idgelDomeLandmarkService, GameBattlefieldServices.idgelDomeLandmarkService());
            assertSame(hallOfTenacityService, GameBattlefieldServices.hallOfTenacityService());
            assertSame(grandArenaTrainingCampService, GameBattlefieldServices.grandArenaTrainingCampService());
            assertSame(idRunService, GameBattlefieldServices.idRunService());
        } finally {
            battlefieldServices.destroy();
            assertProviderCleared(KamarBattlefieldService.class);
            assertProviderCleared(EngulfedOphidanBridgeService.class);
            assertProviderCleared(SuspiciousOphidanBridgeService.class);
            assertProviderCleared(IronWallWarfrontService.class);
            assertProviderCleared(IdgelDomeService.class);
            assertProviderCleared(IdgelDomeLandmarkService.class);
            assertProviderCleared(HallOfTenacityService.class);
            assertProviderCleared(GrandArenaTrainingCampService.class);
            assertProviderCleared(IDRunService.class);
        }
    }

    @Test
    void remainingSingletonAccessorsUseSpringProvidersBeforeLegacyFallbacks() throws Exception {
        LegionService legionService = instance(LegionService.class);
        NavData navData = instance(NavData.class);
        WebshopService webshopService = instance(WebshopService.class);
        ThievesGuildService thievesGuildService = instance(ThievesGuildService.class);
        InGameShopEn inGameShopEn = instance(InGameShopEn.class);
        BalaurAssaultService balaurAssaultService = instance(BalaurAssaultService.class);
        HousingService housingService = instance(HousingService.class);
        BattlefieldUnionService battlefieldUnionService = instance(BattlefieldUnionService.class);
        FindGroupService findGroupService = instance(FindGroupService.class);
        SurveyService surveyService = instance(SurveyService.class);

        try {
            LegionService.setInstanceProvider(provider(LegionService.class, legionService));
            NavData.setInstanceProvider(provider(NavData.class, navData));
            WebshopService.setInstanceProvider(provider(WebshopService.class, webshopService));
            ThievesGuildService.setInstanceProvider(provider(ThievesGuildService.class, thievesGuildService));
            InGameShopEn.setInstanceProvider(provider(InGameShopEn.class, inGameShopEn));
            BalaurAssaultService.setInstanceProvider(provider(BalaurAssaultService.class, balaurAssaultService));
            HousingService.setInstanceProvider(provider(HousingService.class, housingService));
            BattlefieldUnionService.setInstanceProvider(provider(BattlefieldUnionService.class, battlefieldUnionService));
            FindGroupService.setInstanceProvider(provider(FindGroupService.class, findGroupService));
            SurveyService.setInstanceProvider(provider(SurveyService.class, surveyService));

            assertSame(legionService, LegionService.getInstance());
            assertSame(navData, NavData.getInstance());
            assertSame(webshopService, WebshopService.getInstance());
            assertSame(thievesGuildService, ThievesGuildService.getInstance());
            assertSame(inGameShopEn, InGameShopEn.getInstance());
            assertSame(inGameShopEn, GameRuntimeServices.inGameShopEn());
            assertSame(balaurAssaultService, BalaurAssaultService.getInstance());
            assertSame(housingService, HousingService.getInstance());
            assertSame(battlefieldUnionService, BattlefieldUnionService.getInstance());
            assertSame(findGroupService, FindGroupService.getInstance());
            assertSame(findGroupService, GameRuntimeServices.findGroupService());
            assertSame(surveyService, SurveyService.getInstance());
        } finally {
            LegionService.setInstanceProvider(null);
            NavData.setInstanceProvider(null);
            WebshopService.setInstanceProvider(null);
            ThievesGuildService.setInstanceProvider(null);
            InGameShopEn.setInstanceProvider(null);
            BalaurAssaultService.setInstanceProvider(null);
            HousingService.setInstanceProvider(null);
            BattlefieldUnionService.setInstanceProvider(null);
            FindGroupService.setInstanceProvider(null);
            SurveyService.setInstanceProvider(null);
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

    private static WorldMapsData worldMaps(int... mapIds) throws ReflectiveOperationException {
        List<WorldMapTemplate> templates = new java.util.ArrayList<>();
        IntObjectHashMap<WorldMapTemplate> index = new IntObjectHashMap<>();
        for (int mapId : mapIds) {
            WorldMapTemplate template = new WorldMapTemplate();
            setField(template, "mapId", mapId);
            templates.add(template);
            index.put(mapId, template);
        }

        WorldMapsData data = new WorldMapsData();
        setField(data, "worldMaps", templates);
        setField(data, "worldIdMap", index);
        return data;
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
