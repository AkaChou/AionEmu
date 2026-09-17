package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameRuntimeServiceBridge;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.services.events.BoostEventService;
import com.aionemu.gameserver.services.events.ThievesGuildService;
import com.aionemu.gameserver.services.mail.MailService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.rift.RiftManager;
import com.aionemu.gameserver.services.siegeservice.BalaurAssaultService;
import com.aionemu.gameserver.services.siegeservice.BattlefieldUnionService;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.utils.audit.GMService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * CoreRuntimeServiceBeans：从 GameLegacyServiceBridgeConfiguration 拆出的域内 Bean 装配。
 * CoreRuntimeServiceBeans: domain-scoped bean wiring split out of GameLegacyServiceBridgeConfiguration.
 */
@Configuration(proxyBeanMethods = false)
public class CoreRuntimeServiceBeans {

    @Bean
    @Lazy
    public AdminService adminService() {
        return new AdminService();
    }

    @Bean
    @Lazy
    public PlayerTransferService gamePlayerTransferService() {
        return new PlayerTransferService();
    }

    @Bean
    @Lazy
    public PeriodicSaveService periodicSaveService() {
        return new PeriodicSaveService();
    }

    @Bean
    @Lazy
    public TerritoryService territoryService() {
        return new TerritoryService();
    }

    @Bean
    @Lazy
    public GameTimeService gameTimeService() {
        return new GameTimeService();
    }

    @Bean
    @Lazy
    public AnnouncementService announcementService() {
        return new AnnouncementService();
    }

    @Bean
    @Lazy
    public DebugService debugService() {
        return new DebugService();
    }

    @Bean
    @Lazy
    public WeatherService weatherService() {
        return new WeatherService();
    }

    @Bean
    @Lazy
    public BrokerService brokerService() {
        return new BrokerService();
    }

    @Bean
    @Lazy
    public Influence influence() {
        return new Influence();
    }

    @Bean
    @Lazy
    public ExchangeService exchangeService() {
        return new ExchangeService();
    }

    @Bean
    @Lazy
    public PetitionService petitionService() {
        return new PetitionService();
    }

    @Bean
    @Lazy
    public FlyRingService flyRingService() {
        return new FlyRingService();
    }

    @Bean
    @Lazy
    public CuringZoneService curingZoneService() {
        return new CuringZoneService();
    }

    @Bean
    @Lazy
    public SpringZoneService springZoneService() {
        return new SpringZoneService();
    }

    @Bean
    @Lazy
    public BoostEventService boostEventService() {
        return new BoostEventService();
    }

    @Bean
    @Lazy
    public TaskManagerFromDB taskManagerFromDB() {
        return new TaskManagerFromDB();
    }

    @Bean
    @Lazy
    public LimitedItemTradeService limitedItemTradeService() {
        return new LimitedItemTradeService();
    }

    @Bean
    @Lazy
    public GMService gmService() {
        return new GMService();
    }

    @Bean
    public GameRuntimeServiceBridge gameRuntimeServiceBridge() {
        return new GameRuntimeServiceBridge();
    }

    @Bean
    @Lazy
    public RewardService rewardService() {
        return new RewardService();
    }

    @Bean
    @Lazy
    public VeteranRewardsService veteranRewardsService() {
        return new VeteranRewardsService();
    }

    @Bean
    @Lazy
    public HousingService housingService() {
        return new HousingService();
    }

    @Bean
    @Lazy
    public SurveyService surveyService() {
        return new SurveyService();
    }

    @Bean
    @Lazy
    public FindGroupService findGroupService() {
        return new FindGroupService();
    }

    @Bean
    @Lazy
    public WebshopService webshopService() {
        return new WebshopService();
    }

    @Bean
    @Lazy
    public InGameShopEn inGameShopEn() {
        return new InGameShopEn();
    }

    @Bean
    @Lazy
    public LegionService legionService() {
        return new LegionService();
    }

    @Bean
    public BalaurAssaultService balaurAssaultService() {
        return new BalaurAssaultService();
    }

    @Bean
    public BattlefieldUnionService battlefieldUnionService() {
        return new BattlefieldUnionService();
    }

    @Bean
    public ThievesGuildService thievesGuildService() {
        return new ThievesGuildService();
    }

    @Bean
    public PvpService pvpService() {
        return new PvpService();
    }

    @Bean
    public AutoGroupService autoGroupService() {
        return new AutoGroupService();
    }

    @Bean
    public AbyssRankingCache abyssRankingCache() {
        return new AbyssRankingCache();
    }

    @Bean
    public MailService mailService() {
        return new MailService();
    }

    @Bean
    public DropService dropService() {
        return new DropService();
    }

    @Bean
    @Lazy
    public RiftManager riftManager() {
        return new RiftManager();
    }

    @Bean
    @Lazy
    public SeasonRankingService seasonRankingService() {
        return new SeasonRankingService();
    }

    @Bean
    public LifeStatsRestoreService lifeStatsRestoreService() {
        return new LifeStatsRestoreService();
    }

    @Bean
    @Lazy
    public DuelService duelService() {
        return new DuelService();
    }
}
