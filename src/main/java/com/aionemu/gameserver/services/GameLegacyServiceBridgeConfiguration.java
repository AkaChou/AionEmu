package com.aionemu.gameserver.services;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
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
}
