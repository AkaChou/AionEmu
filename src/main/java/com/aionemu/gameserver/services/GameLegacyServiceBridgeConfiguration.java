package com.aionemu.gameserver.services;

import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
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
}
