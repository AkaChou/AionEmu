package com.aionemu.gameserver.services;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
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
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public class GameLegacyServiceBridgeConfiguration {

    @Bean
    @Lazy
    public AdminService adminService() {
        return AdminService.getInstance();
    }

    @Bean
    @Lazy
    public PlayerTransferService gamePlayerTransferService() {
        return PlayerTransferService.getInstance();
    }

    @Bean
    @Lazy
    public EventService eventService() {
        return EventService.getInstance();
    }

    @Bean
    @Lazy
    public PlayerEventService playerEventService() {
        return PlayerEventService.getInstance();
    }

    @Bean
    @Lazy
    public CrazyDaevaService crazyDaevaService() {
        return CrazyDaevaService.getInstance();
    }

    @Bean
    @Lazy
    public AbyssRankUpdateService abyssRankUpdateService() {
        return AbyssRankUpdateService.getInstance();
    }

    @Bean
    @Lazy
    public PacketBroadcaster packetBroadcaster() {
        return PacketBroadcaster.getInstance();
    }

    @Bean
    @Lazy
    public FFAService ffaService() {
        return FFAService.getInstance();
    }

    @Bean
    @Lazy
    public LadderService ladderService() {
        return LadderService.getInstance();
    }

    @Bean
    @Lazy
    public BGService bgService() {
        return BGService.getInstance();
    }

    @Bean
    @Lazy
    public BanditService banditService() {
        return BanditService.getInstance();
    }

    @Bean
    @Lazy
    public PlayerLimitService playerLimitService() {
        return PlayerLimitService.getInstance();
    }

    @Bean
    @Lazy
    public NpcShoutsService npcShoutsService() {
        return NpcShoutsService.getInstance();
    }

    @Bean
    @Lazy
    public ShieldService shieldService() {
        return ShieldService.getInstance();
    }

    @Bean
    @Lazy
    public HousingBidService housingBidService() {
        return HousingBidService.getInstance();
    }

    @Bean
    @Lazy
    public MaintenanceTask maintenanceTask() {
        return MaintenanceTask.getInstance();
    }

    @Bean
    @Lazy
    public TownService townService() {
        return TownService.getInstance();
    }

    @Bean
    @Lazy
    public ChallengeTaskService challengeTaskService() {
        return ChallengeTaskService.getInstance();
    }

    @Bean
    @Lazy
    public KamarBattlefieldService kamarBattlefieldService() {
        return KamarBattlefieldService.getInstance();
    }

    @Bean
    @Lazy
    public EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        return EngulfedOphidanBridgeService.getInstance();
    }

    @Bean
    @Lazy
    public SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        return SuspiciousOphidanBridgeService.getInstance();
    }

    @Bean
    @Lazy
    public IronWallWarfrontService ironWallWarfrontService() {
        return IronWallWarfrontService.getInstance();
    }

    @Bean
    @Lazy
    public IdgelDomeService idgelDomeService() {
        return IdgelDomeService.getInstance();
    }

    @Bean
    @Lazy
    public IdgelDomeLandmarkService idgelDomeLandmarkService() {
        return IdgelDomeLandmarkService.getInstance();
    }

    @Bean
    @Lazy
    public HallOfTenacityService hallOfTenacityService() {
        return HallOfTenacityService.getInstance();
    }

    @Bean
    @Lazy
    public GrandArenaTrainingCampService grandArenaTrainingCampService() {
        return GrandArenaTrainingCampService.getInstance();
    }

    @Bean
    @Lazy
    public IDRunService idRunService() {
        return IDRunService.getInstance();
    }

    @Bean
    @Lazy
    public ThreadPoolManager threadPoolManager() {
        return ThreadPoolManager.getInstance();
    }

    @Bean
    @Lazy
    public RewardService rewardService() {
        return RewardService.getInstance();
    }

    @Bean
    @Lazy
    public WeddingService weddingService() {
        return WeddingService.getInstance();
    }

    @Bean
    @Lazy
    public VeteranRewardsService veteranRewardsService() {
        return VeteranRewardsService.getInstance();
    }

    @Bean
    @Lazy
    public DatabaseCleaningService databaseCleaningService() {
        return DatabaseCleaningService.getInstance();
    }

    @Bean
    @Lazy
    public AbyssRankCleaningService abyssRankCleaningService() {
        return AbyssRankCleaningService.getInstance();
    }

    @Bean
    @Lazy
    public GeoService geoService() {
        return GeoService.getInstance();
    }

    @Bean
    @Lazy
    public NavService navService() {
        return NavService.getInstance();
    }

    @Bean
    @Lazy
    public DataManager dataManager() {
        return DataManager.getInstance();
    }

    @Bean
    @Lazy
    public HTMLCache htmlCache() {
        return HTMLCache.getInstance();
    }

    @Bean
    @Lazy
    public DisputeLandService disputeLandService() {
        return DisputeLandService.getInstance();
    }

    @Bean
    @Lazy
    public OutpostService outpostService() {
        return OutpostService.getInstance();
    }

    @Bean
    @Lazy
    public DredgionService2 dredgionService() {
        return DredgionService2.getInstance();
    }

    @Bean
    @Lazy
    public AsyunatarService asyunatarService() {
        return AsyunatarService.getInstance();
    }

    @Bean
    @Lazy
    public ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        return ShugoImperialTombSpawnManager.getInstance();
    }

    @Bean
    @Lazy
    public SeasonRankingUpdateService seasonRankingUpdateService() {
        return SeasonRankingUpdateService.getInstance();
    }

    @Bean
    @Lazy
    public ProtectorConquerorService protectorConquerorService() {
        return ProtectorConquerorService.getInstance();
    }

    @Bean
    @Lazy
    public DropRegistrationService dropRegistrationService() {
        return DropRegistrationService.getInstance();
    }

    @Bean
    @Lazy
    public ShutdownHook shutdownHook() {
        return ShutdownHook.getInstance();
    }

    @Bean
    @Lazy
    public BannedMacManager bannedMacManager() {
        return BannedMacManager.getInstance();
    }

    @Bean
    @Lazy
    public LoginServer loginServer() {
        return LoginServer.getInstance();
    }

    @Bean
    @Lazy
    public ChatServer chatServer() {
        return ChatServer.getInstance();
    }

    @Bean
    @Lazy
    public SiegeService siegeService() {
        return SiegeService.getInstance();
    }

    @Bean
    @Lazy
    public BaseService baseService() {
        return BaseService.getInstance();
    }
}
