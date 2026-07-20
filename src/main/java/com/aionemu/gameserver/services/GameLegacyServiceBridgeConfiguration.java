package com.aionemu.gameserver.services;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.eventEngine.EventScheduler;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.lifecycle.GameRuntimeServiceBridge;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.NetworkController;
import com.aionemu.gameserver.network.PacketFloodFilter;
import com.aionemu.gameserver.network.PacketLoggerService;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.gameserver.network.factories.LsPacketHandlerFactory;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.services.abysslandingservice.LandingUpdateService;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.services.craft.RelinquishCraftStatus;
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
import com.aionemu.gameserver.services.item.CoalescenceService;
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.services.drop.DropDistributionService;
import com.aionemu.gameserver.services.mail.MailService;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.services.siegeservice.BalaurAssaultService;
import com.aionemu.gameserver.services.siegeservice.BattlefieldUnionService;
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
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.reward.BonusService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.rift.RiftManager;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.services.toypet.PetService;
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
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.audit.GMService;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.path.PathService;
import com.aionemu.gameserver.world.zone.ZoneService;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import java.text.ParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 遗留服务 Spring 桥接配置：以懒加载 Bean 暴露原单例服务，供依赖注入与 {@code GameRuntimeServiceBridge} 使用。
 * Spring bridge configuration for legacy services: exposes former singletons as lazy beans for DI and {@code GameRuntimeServiceBridge}.
 */
@Configuration(proxyBeanMethods = false)
public class GameLegacyServiceBridgeConfiguration {

    /**
     * 提供懒加载的 AdminService Bean。
     * Provides a lazy AdminService bean.
     *
     * AdminService instance
     */
    @Bean
    @Lazy
    public AdminService adminService() {
        return new AdminService();
    }

    /**
     * 提供懒加载的 PlayerTransferService Bean。
     * Provides a lazy PlayerTransferService bean.
     *
     * PlayerTransferService instance
     */
    @Bean
    @Lazy
    public PlayerTransferService gamePlayerTransferService() {
        return new PlayerTransferService();
    }

    /**
     * 提供懒加载的 EventService Bean。
     * Provides a lazy EventService bean.
     *
     * EventService instance
     */
    @Bean
    @Lazy
    public EventService eventService() {
        return new EventService();
    }

    /**
     * 提供懒加载的 PlayerEventService Bean。
     * Provides a lazy PlayerEventService bean.
     *
     * PlayerEventService instance
     */
    @Bean
    @Lazy
    public PlayerEventService playerEventService() {
        return new PlayerEventService();
    }

    /**
     * 提供懒加载的 CrazyDaevaService Bean。
     * Provides a lazy CrazyDaevaService bean.
     *
     * CrazyDaevaService instance
     */
    @Bean
    @Lazy
    public CrazyDaevaService crazyDaevaService() {
        return new CrazyDaevaService();
    }

    /**
     * 提供懒加载的 AbyssRankUpdateService Bean。
     * Provides a lazy AbyssRankUpdateService bean.
     *
     * AbyssRankUpdateService instance
     */
    @Bean
    @Lazy
    public AbyssRankUpdateService abyssRankUpdateService() {
        return new AbyssRankUpdateService();
    }

    /**
     * 提供懒加载的 PacketBroadcaster Bean。
     * Provides a lazy PacketBroadcaster bean.
     *
     * PacketBroadcaster instance
     */
    @Bean
    @Lazy
    public PacketBroadcaster packetBroadcaster() {
        return new PacketBroadcaster();
    }

    /**
     * 提供懒加载的 EventScheduler Bean。
     * Provides a lazy EventScheduler bean.
     *
     * EventScheduler instance
     */
    @Bean
    @Lazy
    public EventScheduler eventScheduler() {
        return new EventScheduler();
    }

    /**
     * 提供懒加载的 FFAService Bean。
     * Provides a lazy FFAService bean.
     *
     * FFAService instance
     */
    @Bean
    @Lazy
    public FFAService ffaService() {
        return new FFAService();
    }

    /**
     * 提供懒加载的 LadderService Bean。
     * Provides a lazy LadderService bean.
     *
     * LadderService instance
     */
    @Bean
    @Lazy
    public LadderService ladderService() {
        return new LadderService();
    }

    /**
     * 提供懒加载的 BGService Bean。
     * Provides a lazy BGService bean.
     *
     * BGService instance
     */
    @Bean
    @Lazy
    public BGService bgService() {
        return new BGService();
    }

    /**
     * 提供懒加载的 BanditService Bean。
     * Provides a lazy BanditService bean.
     *
     * BanditService instance
     */
    @Bean
    @Lazy
    public BanditService banditService() {
        return new BanditService();
    }

    /**
     * 提供懒加载的 PlayerLimitService Bean。
     * Provides a lazy PlayerLimitService bean.
     *
     * PlayerLimitService instance
     */
    @Bean
    @Lazy
    public PlayerLimitService playerLimitService() {
        return new PlayerLimitService();
    }

    /**
     * 提供懒加载的 NpcShoutsService Bean。
     * Provides a lazy NpcShoutsService bean.
     *
     * NpcShoutsService instance
     */
    @Bean
    @Lazy
    public NpcShoutsService npcShoutsService() {
        return new NpcShoutsService();
    }

    /**
     * 提供懒加载的 ShieldService Bean。
     * Provides a lazy ShieldService bean.
     *
     * ShieldService instance
     */
    @Bean
    @Lazy
    public ShieldService shieldService() {
        return new ShieldService();
    }

    /**
     * 提供懒加载的 HousingBidService Bean。
     * Provides a lazy HousingBidService bean.
     *
     * HousingBidService instance
     */
    @Bean
    @Lazy
    public HousingBidService housingBidService() throws ParseException {
        return new HousingBidService();
    }

    /**
     * 提供懒加载的 MaintenanceTask Bean。
     * Provides a lazy MaintenanceTask bean.
     *
     * MaintenanceTask instance
     */
    @Bean
    @Lazy
    public MaintenanceTask maintenanceTask() throws ParseException {
        return new MaintenanceTask(HousingConfig.HOUSE_MAINTENANCE_TIME);
    }

    /**
     * 提供懒加载的 TownService Bean。
     * Provides a lazy TownService bean.
     *
     * TownService instance
     */
    @Bean
    @Lazy
    public TownService townService() {
        return new TownService();
    }

    /**
     * 提供懒加载的 ChallengeTaskService Bean。
     * Provides a lazy ChallengeTaskService bean.
     *
     * ChallengeTaskService instance
     */
    @Bean
    @Lazy
    public ChallengeTaskService challengeTaskService() {
        return new ChallengeTaskService();
    }

    /**
     * 提供懒加载的 ThreadPoolManager Bean。
     * Provides a lazy ThreadPoolManager bean.
     *
     * ThreadPoolManager instance
     */
    @Bean
    @Lazy
    public ThreadPoolManager threadPoolManager() {
        return new ThreadPoolManager();
    }

    /**
     * 提供懒加载的 QuestEngine Bean。
     * Provides a lazy QuestEngine bean.
     *
     * QuestEngine instance
     */
    @Bean
    @Lazy
    public QuestEngine questEngine() {
        return new QuestEngine();
    }

    /**
     * 提供懒加载的 InstanceEngine Bean。
     * Provides a lazy InstanceEngine bean.
     *
     * InstanceEngine instance
     */
    @Bean
    @Lazy
    public InstanceEngine instanceEngine() {
        return new InstanceEngine();
    }

    /**
     * 提供懒加载的 AI2Engine Bean。
     * Provides a lazy AI2Engine bean.
     *
     * AI2Engine instance
     */
    @Bean
    @Lazy
    public AI2Engine ai2Engine() {
        return new AI2Engine();
    }

    /**
     * 提供懒加载的 ChatProcessor Bean。
     * Provides a lazy ChatProcessor bean.
     *
     * ChatProcessor instance
     */
    @Bean
    @Lazy
    public ChatProcessor chatProcessor() {
        return new ChatProcessor();
    }

    /**
     * 提供懒加载的 LunaShopService Bean。
     * Provides a lazy LunaShopService bean.
     *
     * LunaShopService instance
     */
    @Bean
    @Lazy
    public LunaShopService lunaShopService() {
        return new LunaShopService();
    }

    /**
     * 提供懒加载的 MinionService Bean。
     * Provides a lazy MinionService bean.
     *
     * MinionService instance
     */
    @Bean
    @Lazy
    public MinionService minionService() {
        return new MinionService();
    }

    /**
     * 提供懒加载的 ShugoSweepService Bean。
     * Provides a lazy ShugoSweepService bean.
     *
     * ShugoSweepService instance
     */
    @Bean
    @Lazy
    public ShugoSweepService shugoSweepService() {
        return new ShugoSweepService();
    }

    /**
     * 提供懒加载的 AtreianPassportService Bean。
     * Provides a lazy AtreianPassportService bean.
     *
     * AtreianPassportService instance
     */
    @Bean
    @Lazy
    public AtreianPassportService atreianPassportService() {
        return new AtreianPassportService();
    }

    /**
     * 提供懒加载的 EventWindowService Bean。
     * Provides a lazy EventWindowService bean.
     *
     * EventWindowService instance
     */
    @Bean
    @Lazy
    public EventWindowService eventWindowService() {
        return new EventWindowService();
    }

    /**
     * 提供懒加载的 PeriodicSaveService Bean。
     * Provides a lazy PeriodicSaveService bean.
     *
     * PeriodicSaveService instance
     */
    @Bean
    @Lazy
    public PeriodicSaveService periodicSaveService() {
        return new PeriodicSaveService();
    }

    /**
     * 提供懒加载的 TerritoryService Bean。
     * Provides a lazy TerritoryService bean.
     *
     * TerritoryService instance
     */
    @Bean
    @Lazy
    public TerritoryService territoryService() {
        return new TerritoryService();
    }

    /**
     * 提供懒加载的 GameTimeService Bean。
     * Provides a lazy GameTimeService bean.
     *
     * GameTimeService instance
     */
    @Bean
    @Lazy
    public GameTimeService gameTimeService() {
        return new GameTimeService();
    }

    /**
     * 提供懒加载的 AnnouncementService Bean。
     * Provides a lazy AnnouncementService bean.
     *
     * AnnouncementService instance
     */
    @Bean
    @Lazy
    public AnnouncementService announcementService() {
        return new AnnouncementService();
    }

    /**
     * 提供懒加载的 DebugService Bean。
     * Provides a lazy DebugService bean.
     *
     * DebugService instance
     */
    @Bean
    @Lazy
    public DebugService debugService() {
        return new DebugService();
    }

    /**
     * 提供懒加载的 WeatherService Bean。
     * Provides a lazy WeatherService bean.
     *
     * WeatherService instance
     */
    @Bean
    @Lazy
    public WeatherService weatherService() {
        return new WeatherService();
    }

    /**
     * 提供懒加载的 BrokerService Bean。
     * Provides a lazy BrokerService bean.
     *
     * BrokerService instance
     */
    @Bean
    @Lazy
    public BrokerService brokerService() {
        return new BrokerService();
    }

    /**
     * 提供懒加载的 Influence Bean。
     * Provides a lazy Influence bean.
     *
     * Influence instance
     */
    @Bean
    @Lazy
    public Influence influence() {
        return new Influence();
    }

    /**
     * 提供懒加载的 ExchangeService Bean。
     * Provides a lazy ExchangeService bean.
     *
     * ExchangeService instance
     */
    @Bean
    @Lazy
    public ExchangeService exchangeService() {
        return new ExchangeService();
    }

    /**
     * 提供懒加载的 PetitionService Bean。
     * Provides a lazy PetitionService bean.
     *
     * PetitionService instance
     */
    @Bean
    @Lazy
    public PetitionService petitionService() {
        return new PetitionService();
    }

    /**
     * 提供懒加载的 FlyRingService Bean。
     * Provides a lazy FlyRingService bean.
     *
     * FlyRingService instance
     */
    @Bean
    @Lazy
    public FlyRingService flyRingService() {
        return new FlyRingService();
    }

    /**
     * 提供懒加载的 CuringZoneService Bean。
     * Provides a lazy CuringZoneService bean.
     *
     * CuringZoneService instance
     */
    @Bean
    @Lazy
    public CuringZoneService curingZoneService() {
        return new CuringZoneService();
    }

    /**
     * 提供懒加载的 SpringZoneService Bean。
     * Provides a lazy SpringZoneService bean.
     *
     * SpringZoneService instance
     */
    @Bean
    @Lazy
    public SpringZoneService springZoneService() {
        return new SpringZoneService();
    }

    /**
     * 提供懒加载的 BoostEventService Bean。
     * Provides a lazy BoostEventService bean.
     *
     * BoostEventService instance
     */
    @Bean
    @Lazy
    public BoostEventService boostEventService() {
        return new BoostEventService();
    }

    /**
     * 提供懒加载的 TaskManagerFromDB Bean。
     * Provides a lazy TaskManagerFromDB bean.
     *
     * TaskManagerFromDB instance
     */
    @Bean
    @Lazy
    public TaskManagerFromDB taskManagerFromDB() {
        return new TaskManagerFromDB();
    }

    /**
     * 提供懒加载的 LimitedItemTradeService Bean。
     * Provides a lazy LimitedItemTradeService bean.
     *
     * LimitedItemTradeService instance
     */
    @Bean
    @Lazy
    public LimitedItemTradeService limitedItemTradeService() {
        return new LimitedItemTradeService();
    }

    /**
     * 提供懒加载的 GMService Bean。
     * Provides a lazy GMService bean.
     *
     * GMService instance
     */
    @Bean
    @Lazy
    public GMService gmService() {
        return new GMService();
    }

    /**
     * 提供懒加载的 GameRuntimeServiceBridge Bean。
     * Provides a lazy GameRuntimeServiceBridge bean.
     *
     * GameRuntimeServiceBridge instance
     */
    @Bean
    public GameRuntimeServiceBridge gameRuntimeServiceBridge() {
        return new GameRuntimeServiceBridge();
    }

    /**
     * 提供懒加载的 RewardService Bean。
     * Provides a lazy RewardService bean.
     *
     * RewardService instance
     */
    @Bean
    @Lazy
    public RewardService rewardService() {
        return new RewardService();
    }

    /**
     * 提供懒加载的 VeteranRewardsService Bean。
     * Provides a lazy VeteranRewardsService bean.
     *
     * VeteranRewardsService instance
     */
    @Bean
    @Lazy
    public VeteranRewardsService veteranRewardsService() {
        return new VeteranRewardsService();
    }

    /**
     * 提供懒加载的 DatabaseCleaningService Bean。
     * Provides a lazy DatabaseCleaningService bean.
     *
     * DatabaseCleaningService instance
     */
    @Bean
    @Lazy
    public DatabaseCleaningService databaseCleaningService() {
        return new DatabaseCleaningService();
    }

    /**
     * 提供懒加载的 AbyssRankCleaningService Bean。
     * Provides a lazy AbyssRankCleaningService bean.
     *
     * AbyssRankCleaningService instance
     */
    @Bean
    @Lazy
    public AbyssRankCleaningService abyssRankCleaningService() {
        return new AbyssRankCleaningService();
    }

    /**
     * 提供懒加载的 GeoService Bean。
     * Provides a lazy GeoService bean.
     *
     * GeoService instance
     */
    @Bean
    @Lazy
    public GeoService geoService() {
        return new GeoService();
    }

    /**
     * 提供懒加载的 PathService Bean。
     * Provides a lazy PathService bean.
     *
     * PathService instance
     */
    @Bean
    @Lazy
    public PathService pathService() {
        return new PathService();
    }

    /**
     * 提供懒加载的 DataManager Bean。
     * Provides a lazy DataManager bean.
     *
     * DataManager instance
     */
    @Bean
    @Lazy
    public DataManager dataManager() {
        return new DataManager();
    }

    /**
     * 提供懒加载的 HTMLCache Bean。
     * Provides a lazy HTMLCache bean.
     *
     * HTMLCache instance
     */
    @Bean
    @Lazy
    public HTMLCache htmlCache() {
        return new HTMLCache();
    }

    /**
     * 提供懒加载的 XmlDataLoader Bean。
     * Provides a lazy XmlDataLoader bean.
     *
     * XmlDataLoader instance
     */
    @Bean
    @Lazy
    public XmlDataLoader xmlDataLoader() {
        return new XmlDataLoader();
    }

    /**
     * 提供懒加载的 DisputeLandService Bean。
     * Provides a lazy DisputeLandService bean.
     *
     * DisputeLandService instance
     */
    @Bean
    @Lazy
    public DisputeLandService disputeLandService() {
        return new DisputeLandService();
    }

    /**
     * 提供懒加载的 OutpostService Bean。
     * Provides a lazy OutpostService bean.
     *
     * OutpostService instance
     */
    @Bean
    @Lazy
    public OutpostService outpostService() {
        return new OutpostService();
    }

    /**
     * 提供懒加载的 ShugoImperialTombSpawnManager Bean。
     * Provides a lazy ShugoImperialTombSpawnManager bean.
     *
     * ShugoImperialTombSpawnManager instance
     */
    @Bean
    @Lazy
    public ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        return new ShugoImperialTombSpawnManager();
    }

    /**
     * 提供懒加载的 SeasonRankingUpdateService Bean。
     * Provides a lazy SeasonRankingUpdateService bean.
     *
     * SeasonRankingUpdateService instance
     */
    @Bean
    @Lazy
    public SeasonRankingUpdateService seasonRankingUpdateService() {
        return new SeasonRankingUpdateService();
    }

    /**
     * 提供懒加载的 ProtectorConquerorService Bean。
     * Provides a lazy ProtectorConquerorService bean.
     *
     * ProtectorConquerorService instance
     */
    @Bean
    @Lazy
    public ProtectorConquerorService protectorConquerorService() {
        return new ProtectorConquerorService();
    }

    /**
     * 提供懒加载的 DropRegistrationService Bean。
     * Provides a lazy DropRegistrationService bean.
     *
     * DropRegistrationService instance
     */
    @Bean
    @Lazy
    public DropRegistrationService dropRegistrationService() {
        return new DropRegistrationService();
    }

    /**
     * 提供懒加载的 IDFactory Bean。
     * Provides a lazy IDFactory bean.
     *
     * IDFactory instance
     */
    @Bean
    @Lazy
    public IDFactory gameIdFactory() {
        return new IDFactory();
    }

    /**
     * 提供懒加载的 ZoneService Bean。
     * Provides a lazy ZoneService bean.
     *
     * ZoneService instance
     */
    @Bean
    @Lazy
    public ZoneService zoneService() {
        return new ZoneService();
    }

    /**
     * 提供懒加载的 HotspotTeleportService Bean。
     * Provides a lazy HotspotTeleportService bean.
     *
     * HotspotTeleportService instance
     */
    @Bean
    @Lazy
    public HotspotTeleportService hotspotTeleportService() {
        return new HotspotTeleportService();
    }

    /**
     * 提供懒加载的 RoadService Bean。
     * Provides a lazy RoadService bean.
     *
     * RoadService instance
     */
    @Bean
    @Lazy
    public RoadService roadService() {
        return new RoadService();
    }

    /**
     * 提供懒加载的 World Bean。
     * Provides a lazy World bean.
     *
     * World instance
     */
    @Bean
    @Lazy
    public World world() {
        return new World();
    }

    /**
     * 提供懒加载的 ShutdownHook Bean。
     * Provides a lazy ShutdownHook bean.
     *
     * ShutdownHook instance
     */
    @Bean
    @Lazy
    public ShutdownHook shutdownHook() {
        return new ShutdownHook();
    }

    /**
     * 提供懒加载的 BannedMacManager Bean。
     * Provides a lazy BannedMacManager bean.
     *
     * BannedMacManager instance
     */
    @Bean
    @Lazy
    public BannedMacManager bannedMacManager() {
        return new BannedMacManager();
    }

    /**
     * 提供懒加载的 PacketLoggerService Bean。
     * Provides a lazy PacketLoggerService bean.
     *
     * PacketLoggerService instance
     */
    @Bean
    @Lazy
    public PacketLoggerService packetLoggerService() {
        return new PacketLoggerService();
    }

    /**
     * 提供懒加载的 NetworkController Bean。
     * Provides a lazy NetworkController bean.
     *
     * NetworkController instance
     */
    @Bean
    @Lazy
    public NetworkController networkController() {
        return new NetworkController();
    }

    /**
     * 提供懒加载的 AionPacketHandlerFactory Bean。
     * Provides a lazy AionPacketHandlerFactory bean.
     *
     * AionPacketHandlerFactory instance
     */
    @Bean
    @Lazy
    public AionPacketHandlerFactory aionPacketHandlerFactory() {
        return new AionPacketHandlerFactory();
    }

    /**
     * 提供懒加载的 PacketFloodFilter Bean。
     * Provides a lazy PacketFloodFilter bean.
     *
     * PacketFloodFilter instance
     */
    @Bean
    @Lazy
    public PacketFloodFilter packetFloodFilter() {
        return new PacketFloodFilter();
    }

    /**
     * 提供懒加载的 LsPacketHandlerFactory Bean。
     * Provides a lazy LsPacketHandlerFactory bean.
     *
     * LsPacketHandlerFactory instance
     */
    @Bean
    @Lazy
    public LsPacketHandlerFactory lsPacketHandlerFactory() {
        return new LsPacketHandlerFactory();
    }

    /**
     * 提供懒加载的 LoginServer Bean。
     * Provides a lazy LoginServer bean.
     *
     * LoginServer instance
     */
    @Bean
    @Lazy
    public LoginServer loginServer() {
        return new LoginServer();
    }

    /**
     * 提供懒加载的 ChatServer Bean。
     * Provides a lazy ChatServer bean.
     *
     * ChatServer instance
     */
    @Bean
    @Lazy
    public ChatServer chatServer() {
        return new ChatServer();
    }

    /**
     * 提供懒加载的 SiegeService Bean。
     * Provides a lazy SiegeService bean.
     *
     * SiegeService instance
     */
    @Bean
    @Lazy
    public SiegeService siegeService() {
        return new SiegeService();
    }

    /**
     * 提供懒加载的 BaseService Bean。
     * Provides a lazy BaseService bean.
     *
     * BaseService instance
     */
    @Bean
    @Lazy
    public BaseService baseService() {
        return new BaseService();
    }

    /**
     * 提供懒加载的 VortexService Bean。
     * Provides a lazy VortexService bean.
     *
     * VortexService instance
     */
    @Bean
    @Lazy
    public VortexService vortexService() {
        return new VortexService();
    }

    /**
     * 提供懒加载的 BeritraService Bean。
     * Provides a lazy BeritraService bean.
     *
     * BeritraService instance
     */
    @Bean
    @Lazy
    public BeritraService beritraService() {
        return new BeritraService();
    }

    /**
     * 提供懒加载的 AgentService Bean。
     * Provides a lazy AgentService bean.
     *
     * AgentService instance
     */
    @Bean
    @Lazy
    public AgentService agentService() {
        return new AgentService();
    }

    /**
     * 提供懒加载的 AnohaService Bean。
     * Provides a lazy AnohaService bean.
     *
     * AnohaService instance
     */
    @Bean
    @Lazy
    public AnohaService anohaService() {
        return new AnohaService();
    }

    /**
     * 提供懒加载的 SvsService Bean。
     * Provides a lazy SvsService bean.
     *
     * SvsService instance
     */
    @Bean
    @Lazy
    public SvsService svsService() {
        return new SvsService();
    }

    /**
     * 提供懒加载的 RvrService Bean。
     * Provides a lazy RvrService bean.
     *
     * RvrService instance
     */
    @Bean
    @Lazy
    public RvrService rvrService() {
        return new RvrService();
    }

    /**
     * 提供懒加载的 IuService Bean。
     * Provides a lazy IuService bean.
     *
     * IuService instance
     */
    @Bean
    @Lazy
    public IuService iuService() {
        return new IuService();
    }

    /**
     * 提供懒加载的 NightmareCircusService Bean。
     * Provides a lazy NightmareCircusService bean.
     *
     * NightmareCircusService instance
     */
    @Bean
    @Lazy
    public NightmareCircusService nightmareCircusService() {
        return new NightmareCircusService();
    }

    /**
     * 提供懒加载的 DynamicRiftService Bean。
     * Provides a lazy DynamicRiftService bean.
     *
     * DynamicRiftService instance
     */
    @Bean
    @Lazy
    public DynamicRiftService dynamicRiftService() {
        return new DynamicRiftService();
    }

    /**
     * 提供懒加载的 InstanceRiftService Bean。
     * Provides a lazy InstanceRiftService bean.
     *
     * InstanceRiftService instance
     */
    @Bean
    @Lazy
    public InstanceRiftService instanceRiftService() {
        return new InstanceRiftService();
    }

    /**
     * 提供懒加载的 ZorshivDredgionService Bean。
     * Provides a lazy ZorshivDredgionService bean.
     *
     * ZorshivDredgionService instance
     */
    @Bean
    @Lazy
    public ZorshivDredgionService zorshivDredgionService() {
        return new ZorshivDredgionService();
    }

    /**
     * 提供懒加载的 MoltenusService Bean。
     * Provides a lazy MoltenusService bean.
     *
     * MoltenusService instance
     */
    @Bean
    @Lazy
    public MoltenusService moltenusService() {
        return new MoltenusService();
    }

    /**
     * 提供懒加载的 RiftService Bean。
     * Provides a lazy RiftService bean.
     *
     * RiftService instance
     */
    @Bean
    @Lazy
    public RiftService riftService() {
        return new RiftService();
    }

    /**
     * 提供懒加载的 ConquestService Bean。
     * Provides a lazy ConquestService bean.
     *
     * ConquestService instance
     */
    @Bean
    @Lazy
    public ConquestService conquestService() {
        return new ConquestService();
    }

    /**
     * 提供懒加载的 IdianDepthsService Bean。
     * Provides a lazy IdianDepthsService bean.
     *
     * IdianDepthsService instance
     */
    @Bean
    @Lazy
    public IdianDepthsService idianDepthsService() {
        return new IdianDepthsService();
    }

    /**
     * 提供懒加载的 TowerOfEternityService Bean。
     * Provides a lazy TowerOfEternityService bean.
     *
     * TowerOfEternityService instance
     */
    @Bean
    @Lazy
    public TowerOfEternityService towerOfEternityService() {
        return new TowerOfEternityService();
    }

    /**
     * 提供懒加载的 AbyssLandingService Bean。
     * Provides a lazy AbyssLandingService bean.
     *
     * AbyssLandingService instance
     */
    @Bean
    @Lazy
    public AbyssLandingService abyssLandingService() {
        return new AbyssLandingService();
    }

    /**
     * 提供懒加载的 LandingUpdateService Bean。
     * Provides a lazy LandingUpdateService bean.
     *
     * LandingUpdateService instance
     */
    @Bean
    @Lazy
    public LandingUpdateService landingUpdateService() {
        return new LandingUpdateService();
    }

    /**
     * 提供懒加载的 AbyssLandingSpecialService Bean。
     * Provides a lazy AbyssLandingSpecialService bean.
     *
     * AbyssLandingSpecialService instance
     */
    @Bean
    @Lazy
    public AbyssLandingSpecialService abyssLandingSpecialService() {
        return new AbyssLandingSpecialService();
    }

    /**
     * 提供懒加载的 AStationService Bean。
     * Provides a lazy AStationService bean.
     *
     * AStationService instance
     */
    @Bean
    @Lazy
    public AStationService aStationService() {
        return new AStationService();
    }

    /**
     * 提供懒加载的 F2pService Bean。
     * Provides a lazy F2pService bean.
     *
     * F2pService instance
     */
    @Bean
    @Lazy
    public F2pService f2pService() {
        return new F2pService();
    }

    /**
     * 提供懒加载的 WindyGorgeService Bean。
     * Provides a lazy WindyGorgeService bean.
     *
     * WindyGorgeService instance
     */
    @Bean
    @Lazy
    public WindyGorgeService windyGorgeService() {
        return new WindyGorgeService();
    }

    /**
     * 提供懒加载的 MotionLoggingService Bean。
     * Provides a lazy MotionLoggingService bean.
     *
     * MotionLoggingService instance
     */
    @Bean
    @Lazy
    public MotionLoggingService motionLoggingService() {
        return new MotionLoggingService();
    }

    /**
     * 提供懒加载的 StaticDoorService Bean。
     * Provides a lazy StaticDoorService bean.
     *
     * StaticDoorService instance
     */
    @Bean
    @Lazy
    public StaticDoorService staticDoorService() {
        return new StaticDoorService();
    }

    /**
     * 提供懒加载的 KiskService Bean。
     * Provides a lazy KiskService bean.
     *
     * KiskService instance
     */
    @Bean
    @Lazy
    public KiskService kiskService() {
        return new KiskService();
    }

    /**
     * 提供懒加载的 RepurchaseService Bean。
     * Provides a lazy RepurchaseService bean.
     *
     * RepurchaseService instance
     */
    @Bean
    @Lazy
    public RepurchaseService repurchaseService() {
        return new RepurchaseService();
    }

    /**
     * 提供懒加载的 DropDistributionService Bean。
     * Provides a lazy DropDistributionService bean.
     *
     * DropDistributionService instance
     */
    @Bean
    @Lazy
    public DropDistributionService dropDistributionService() {
        return new DropDistributionService();
    }

    /**
     * 提供懒加载的 SystemMailService Bean。
     * Provides a lazy SystemMailService bean.
     *
     * SystemMailService instance
     */
    @Bean
    @Lazy
    public SystemMailService systemMailService() {
        return new SystemMailService();
    }

    /**
     * 提供懒加载的 BonusService Bean。
     * Provides a lazy BonusService bean.
     *
     * BonusService instance
     */
    @Bean
    @Lazy
    public BonusService bonusService() {
        return new BonusService();
    }

    /**
     * 提供懒加载的 PetService Bean。
     * Provides a lazy PetService bean.
     *
     * PetService instance
     */
    @Bean
    @Lazy
    public PetService petService() {
        return new PetService();
    }

    /**
     * 提供懒加载的 ArcadeUpgradeService Bean。
     * Provides a lazy ArcadeUpgradeService bean.
     *
     * ArcadeUpgradeService instance
     */
    @Bean
    @Lazy
    public ArcadeUpgradeService arcadeUpgradeService() {
        return new ArcadeUpgradeService();
    }

    /**
     * 提供懒加载的 AtreianBestiaryService Bean。
     * Provides a lazy AtreianBestiaryService bean.
     *
     * AtreianBestiaryService instance
     */
    @Bean
    @Lazy
    public AtreianBestiaryService atreianBestiaryService() {
        return new AtreianBestiaryService();
    }

    /**
     * 提供懒加载的 CoalescenceService Bean。
     * Provides a lazy CoalescenceService bean.
     *
     * CoalescenceService instance
     */
    @Bean
    @Lazy
    public CoalescenceService coalescenceService() {
        return new CoalescenceService();
    }

    /**
     * 提供懒加载的 GrowthEnergy Bean。
     * Provides a lazy GrowthEnergy bean.
     *
     * GrowthEnergy instance
     */
    @Bean
    @Lazy
    public GrowthEnergy growthEnergy() {
        return new GrowthEnergy();
    }

    /**
     * 提供懒加载的 ExpireTimerTask Bean。
     * Provides a lazy ExpireTimerTask bean.
     *
     * ExpireTimerTask instance
     */
    @Bean
    @Lazy
    public ExpireTimerTask expireTimerTask() {
        return new ExpireTimerTask();
    }

    /**
     * 提供懒加载的 TeamEffectUpdater Bean。
     * Provides a lazy TeamEffectUpdater bean.
     *
     * TeamEffectUpdater instance
     */
    @Bean
    @Lazy
    public TeamEffectUpdater teamEffectUpdater() {
        return new TeamEffectUpdater();
    }

    /**
     * 提供懒加载的 TeamMoveUpdater Bean。
     * Provides a lazy TeamMoveUpdater bean.
     *
     * TeamMoveUpdater instance
     */
    @Bean
    @Lazy
    public TeamMoveUpdater teamMoveUpdater() {
        return new TeamMoveUpdater();
    }

    /**
     * 提供懒加载的 TemporaryTradeTimeTask Bean。
     * Provides a lazy TemporaryTradeTimeTask bean.
     *
     * TemporaryTradeTimeTask instance
     */
    @Bean
    @Lazy
    public TemporaryTradeTimeTask temporaryTradeTimeTask() {
        return new TemporaryTradeTimeTask();
    }

    /**
     * 提供懒加载的 CreativityEssenceService Bean。
     * Provides a lazy CreativityEssenceService bean.
     *
     * CreativityEssenceService instance
     */
    @Bean
    @Lazy
    public CreativityEssenceService creativityEssenceService() {
        return new CreativityEssenceService();
    }

    /**
     * 提供懒加载的 CreativitySkillService Bean。
     * Provides a lazy CreativitySkillService bean.
     *
     * CreativitySkillService instance
     */
    @Bean
    @Lazy
    public CreativitySkillService creativitySkillService() {
        return new CreativitySkillService();
    }

    /**
     * 提供懒加载的 CreativityStatsService Bean。
     * Provides a lazy CreativityStatsService bean.
     *
     * CreativityStatsService instance
     */
    @Bean
    @Lazy
    public CreativityStatsService creativityStatsService() {
        return new CreativityStatsService();
    }

    /**
     * 提供懒加载的 CreativityTransfoService Bean。
     * Provides a lazy CreativityTransfoService bean.
     *
     * CreativityTransfoService instance
     */
    @Bean
    @Lazy
    public CreativityTransfoService creativityTransfoService() {
        return new CreativityTransfoService();
    }

    /**
     * 提供懒加载的 Accuracy Bean。
     * Provides a lazy Accuracy bean.
     *
     * Accuracy instance
     */
    @Bean
    @Lazy
    public Accuracy accuracy() {
        return new Accuracy();
    }

    /**
     * 提供懒加载的 Agility Bean。
     * Provides a lazy Agility bean.
     *
     * Agility instance
     */
    @Bean
    @Lazy
    public Agility agility() {
        return new Agility();
    }

    /**
     * 提供懒加载的 Health Bean。
     * Provides a lazy Health bean.
     *
     * Health instance
     */
    @Bean
    @Lazy
    public Health health() {
        return new Health();
    }

    /**
     * 提供懒加载的 Knowledge Bean。
     * Provides a lazy Knowledge bean.
     *
     * Knowledge instance
     */
    @Bean
    @Lazy
    public Knowledge knowledge() {
        return new Knowledge();
    }

    /**
     * 提供懒加载的 Power Bean。
     * Provides a lazy Power bean.
     *
     * Power instance
     */
    @Bean
    @Lazy
    public Power power() {
        return new Power();
    }

    /**
     * 提供懒加载的 Precision Bean。
     * Provides a lazy Precision bean.
     *
     * Precision instance
     */
    @Bean
    @Lazy
    public Precision precision() {
        return new Precision();
    }

    /**
     * 提供懒加载的 Will Bean。
     * Provides a lazy Will bean.
     *
     * Will instance
     */
    @Bean
    @Lazy
    public Will will() {
        return new Will();
    }

    /**
     * 提供懒加载的 CraftSkillUpdateService Bean。
     * Provides a lazy CraftSkillUpdateService bean.
     *
     * CraftSkillUpdateService instance
     */
    @Bean
    @Lazy
    public CraftSkillUpdateService craftSkillUpdateService() {
        return new CraftSkillUpdateService();
    }

    /**
     * 提供懒加载的 RelinquishCraftStatus Bean。
     * Provides a lazy RelinquishCraftStatus bean.
     *
     * RelinquishCraftStatus instance
     */
    @Bean
    @Lazy
    public RelinquishCraftStatus relinquishCraftStatus() {
        return new RelinquishCraftStatus();
    }

    /**
     * 提供懒加载的 DuelService Bean。
     * Provides a lazy DuelService bean.
     *
     * DuelService instance
     */
    @Bean
    @Lazy
    public DuelService duelService() {
        return new DuelService();
    }

    /**
     * 提供懒加载的 LifeStatsRestoreService Bean。
     * Provides a lazy LifeStatsRestoreService bean.
     *
     * LifeStatsRestoreService instance
     */
    @Bean
    public LifeStatsRestoreService lifeStatsRestoreService() {
        return new LifeStatsRestoreService();
    }

    /**
     * 提供懒加载的 SeasonRankingService Bean。
     * Provides a lazy SeasonRankingService bean.
     *
     * SeasonRankingService instance
     */
    @Bean
    @Lazy
    public SeasonRankingService seasonRankingService() {
        return new SeasonRankingService();
    }

    /**
     * 提供懒加载的 RiftManager Bean。
     * Provides a lazy RiftManager bean.
     *
     * RiftManager instance
     */
    @Bean
    @Lazy
    public RiftManager riftManager() {
        return new RiftManager();
    }

    /**
     * 提供懒加载的 DropService Bean。
     * Provides a lazy DropService bean.
     *
     * DropService instance
     */
    @Bean
    public DropService dropService() {
        return new DropService();
    }

    /**
     * 提供懒加载的 MailService Bean。
     * Provides a lazy MailService bean.
     *
     * MailService instance
     */
    @Bean
    public MailService mailService() {
        return new MailService();
    }

    /**
     * 提供懒加载的 PvpService Bean。
     * Provides a lazy PvpService bean.
     *
     * PvpService instance
     */
    @Bean
    public PvpService pvpService() {
        return new PvpService();
    }

    /**
     * 提供懒加载的 AutoGroupService Bean。
     * Provides a lazy AutoGroupService bean.
     *
     * AutoGroupService instance
     */
    @Bean
    public AutoGroupService autoGroupService() {
        return new RetailMatchmakingService();
    }

    /**
     * 提供懒加载的 AbyssRankingCache Bean。
     * Provides a lazy AbyssRankingCache bean.
     *
     * AbyssRankingCache instance
     */
    @Bean
    public AbyssRankingCache abyssRankingCache() {
        return new AbyssRankingCache();
    }

    /**
     * 提供懒加载的 LegionService Bean。
     * Provides a lazy LegionService bean.
     *
     * LegionService instance
     */
    @Bean
    @Lazy
    public LegionService legionService() {
        return new LegionService();
    }

    /**
     * 提供懒加载的 ThievesGuildService Bean。
     * Provides a lazy ThievesGuildService bean.
     *
     * ThievesGuildService instance
     */
    @Bean
    public ThievesGuildService thievesGuildService() {
        return new ThievesGuildService();
    }

    /**
     * 提供懒加载的 BalaurAssaultService Bean。
     * Provides a lazy BalaurAssaultService bean.
     *
     * BalaurAssaultService instance
     */
    @Bean
    public BalaurAssaultService balaurAssaultService() {
        return new BalaurAssaultService();
    }

    /**
     * 提供懒加载的 BattlefieldUnionService Bean。
     * Provides a lazy BattlefieldUnionService bean.
     *
     * BattlefieldUnionService instance
     */
    @Bean
    public BattlefieldUnionService battlefieldUnionService() {
        return new BattlefieldUnionService();
    }

    /**
     * 提供懒加载的 WebshopService Bean。
     * Provides a lazy WebshopService bean.
     *
     * WebshopService instance
     */
    @Bean
    @Lazy
    public WebshopService webshopService() {
        return new WebshopService();
    }

    /**
     * 提供懒加载的 SurveyService Bean。
     * Provides a lazy SurveyService bean.
     *
     * SurveyService instance
     */
    @Bean
    @Lazy
    public SurveyService surveyService() {
        return new SurveyService();
    }

    /**
     * 提供懒加载的 FindGroupService Bean。
     * Provides a lazy FindGroupService bean.
     *
     * FindGroupService instance
     */
    @Bean
    @Lazy
    public FindGroupService findGroupService() {
        return new FindGroupService();
    }

    /**
     * 提供懒加载的 InGameShopEn Bean。
     * Provides a lazy InGameShopEn bean.
     *
     * InGameShopEn instance
     */
    @Bean
    @Lazy
    public InGameShopEn inGameShopEn() {
        return new InGameShopEn();
    }

    /**
     * 提供懒加载的 HousingService Bean。
     * Provides a lazy HousingService bean.
     *
     * HousingService instance
     */
    @Bean
    @Lazy
    public HousingService housingService() {
        return new HousingService();
    }

    /**
     * 提供懒加载的 MovementNotifyTask Bean。
     * Provides a lazy MovementNotifyTask bean.
     *
     * MovementNotifyTask instance
     */
    @Bean
    @Lazy
    public MovementNotifyTask movementNotifyTask() {
        return new MovementNotifyTask();
    }

    /**
     * 提供懒加载的 MoveTaskManager Bean。
     * Provides a lazy MoveTaskManager bean.
     *
     * MoveTaskManager instance
     */
    @Bean
    @Lazy
    public MoveTaskManager moveTaskManager() {
        return new MoveTaskManager();
    }

    /**
     * 提供懒加载的 PlayerMoveTaskManager Bean。
     * Provides a lazy PlayerMoveTaskManager bean.
     *
     * PlayerMoveTaskManager instance
     */
    @Bean
    @Lazy
    public PlayerMoveTaskManager playerMoveTaskManager() {
        return new PlayerMoveTaskManager();
    }

    /**
     * 提供懒加载的 ZoneUpdateService Bean。
     * Provides a lazy ZoneUpdateService bean.
     *
     * ZoneUpdateService instance
     */
    @Bean
    @Lazy
    public ZoneUpdateService zoneUpdateService() {
        return new ZoneUpdateService();
    }
}
