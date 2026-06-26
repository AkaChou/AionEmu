package com.aionemu.gameserver.services;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.ShutdownHook;
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
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.events.AtreianPassportService;
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
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import com.aionemu.gameserver.world.zone.ZoneService;
import java.text.ParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public class GameLegacyServiceBridgeConfiguration {

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
    public EventService eventService() {
        return new EventService();
    }

    @Bean
    @Lazy
    public PlayerEventService playerEventService() {
        return new PlayerEventService();
    }

    @Bean
    @Lazy
    public CrazyDaevaService crazyDaevaService() {
        return new CrazyDaevaService();
    }

    @Bean
    @Lazy
    public AbyssRankUpdateService abyssRankUpdateService() {
        return new AbyssRankUpdateService();
    }

    @Bean
    @Lazy
    public PacketBroadcaster packetBroadcaster() {
        return new PacketBroadcaster();
    }

    @Bean
    @Lazy
    public FFAService ffaService() {
        return new FFAService();
    }

    @Bean
    @Lazy
    public LadderService ladderService() {
        return new LadderService();
    }

    @Bean
    @Lazy
    public BGService bgService() {
        return new BGService();
    }

    @Bean
    @Lazy
    public BanditService banditService() {
        return new BanditService();
    }

    @Bean
    @Lazy
    public PlayerLimitService playerLimitService() {
        return new PlayerLimitService();
    }

    @Bean
    @Lazy
    public NpcShoutsService npcShoutsService() {
        return new NpcShoutsService();
    }

    @Bean
    @Lazy
    public ShieldService shieldService() {
        return new ShieldService();
    }

    @Bean
    @Lazy
    public HousingBidService housingBidService() {
        return HousingBidService.getInstance();
    }

    @Bean
    @Lazy
    public MaintenanceTask maintenanceTask() throws ParseException {
        return new MaintenanceTask(HousingConfig.HOUSE_MAINTENANCE_TIME);
    }

    @Bean
    @Lazy
    public TownService townService() {
        return TownService.getInstance();
    }

    @Bean
    @Lazy
    public ChallengeTaskService challengeTaskService() {
        return new ChallengeTaskService();
    }

    @Bean
    @Lazy
    public KamarBattlefieldService kamarBattlefieldService() {
        return new KamarBattlefieldService();
    }

    @Bean
    @Lazy
    public EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        return new EngulfedOphidanBridgeService();
    }

    @Bean
    @Lazy
    public SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        return new SuspiciousOphidanBridgeService();
    }

    @Bean
    @Lazy
    public IronWallWarfrontService ironWallWarfrontService() {
        return new IronWallWarfrontService();
    }

    @Bean
    @Lazy
    public IdgelDomeService idgelDomeService() {
        return new IdgelDomeService();
    }

    @Bean
    @Lazy
    public IdgelDomeLandmarkService idgelDomeLandmarkService() {
        return new IdgelDomeLandmarkService();
    }

    @Bean
    @Lazy
    public HallOfTenacityService hallOfTenacityService() {
        return new HallOfTenacityService();
    }

    @Bean
    @Lazy
    public GrandArenaTrainingCampService grandArenaTrainingCampService() {
        return new GrandArenaTrainingCampService();
    }

    @Bean
    @Lazy
    public IDRunService idRunService() {
        return new IDRunService();
    }

    @Bean
    @Lazy
    public ThreadPoolManager threadPoolManager() {
        return ThreadPoolManager.getInstance();
    }

    @Bean
    @Lazy
    public QuestEngine questEngine() {
        return new QuestEngine();
    }

    @Bean
    @Lazy
    public InstanceEngine instanceEngine() {
        return new InstanceEngine();
    }

    @Bean
    @Lazy
    public AI2Engine ai2Engine() {
        return new AI2Engine();
    }

    @Bean
    @Lazy
    public ChatProcessor chatProcessor() {
        return new ChatProcessor();
    }

    @Bean
    @Lazy
    public LunaShopService lunaShopService() {
        return new LunaShopService();
    }

    @Bean
    @Lazy
    public MinionService minionService() {
        return new MinionService();
    }

    @Bean
    @Lazy
    public ShugoSweepService shugoSweepService() {
        return new ShugoSweepService();
    }

    @Bean
    @Lazy
    public AtreianPassportService atreianPassportService() {
        return new AtreianPassportService();
    }

    @Bean
    @Lazy
    public EventWindowService eventWindowService() {
        return new EventWindowService();
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
        return TaskManagerFromDB.getInstance();
    }

    @Bean
    @Lazy
    public LimitedItemTradeService limitedItemTradeService() {
        return new LimitedItemTradeService();
    }

    @Bean
    @Lazy
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
    public WeddingService weddingService() {
        return new WeddingService();
    }

    @Bean
    @Lazy
    public VeteranRewardsService veteranRewardsService() {
        return new VeteranRewardsService();
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
        return new GeoService();
    }

    @Bean
    @Lazy
    public NavService navService() {
        return new NavService();
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
        return new DisputeLandService();
    }

    @Bean
    @Lazy
    public OutpostService outpostService() {
        return new OutpostService();
    }

    @Bean
    @Lazy
    public DredgionService2 dredgionService() {
        return new DredgionService2();
    }

    @Bean
    @Lazy
    public AsyunatarService asyunatarService() {
        return new AsyunatarService();
    }

    @Bean
    @Lazy
    public ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        return new ShugoImperialTombSpawnManager();
    }

    @Bean
    @Lazy
    public SeasonRankingUpdateService seasonRankingUpdateService() {
        return new SeasonRankingUpdateService();
    }

    @Bean
    @Lazy
    public ProtectorConquerorService protectorConquerorService() {
        return new ProtectorConquerorService();
    }

    @Bean
    @Lazy
    public DropRegistrationService dropRegistrationService() {
        return new DropRegistrationService();
    }

    @Bean
    @Lazy
    public IDFactory gameIdFactory() {
        return IDFactory.getInstance();
    }

    @Bean
    @Lazy
    public ZoneService zoneService() {
        return ZoneService.getInstance();
    }

    @Bean
    @Lazy
    public HotspotTeleportService hotspotTeleportService() {
        return HotspotTeleportService.getInstance();
    }

    @Bean
    @Lazy
    public RoadService roadService() {
        return RoadService.getInstance();
    }

    @Bean
    @Lazy
    public World world() {
        return World.getInstance();
    }

    @Bean
    @Lazy
    public ShutdownHook shutdownHook() {
        return new ShutdownHook();
    }

    @Bean
    @Lazy
    public BannedMacManager bannedMacManager() {
        return new BannedMacManager();
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
        return new SiegeService();
    }

    @Bean
    @Lazy
    public BaseService baseService() {
        return new BaseService();
    }

    @Bean
    @Lazy
    public VortexService vortexService() {
        return new VortexService();
    }

    @Bean
    @Lazy
    public BeritraService beritraService() {
        return new BeritraService();
    }

    @Bean
    @Lazy
    public AgentService agentService() {
        return new AgentService();
    }

    @Bean
    @Lazy
    public AnohaService anohaService() {
        return new AnohaService();
    }

    @Bean
    @Lazy
    public SvsService svsService() {
        return new SvsService();
    }

    @Bean
    @Lazy
    public RvrService rvrService() {
        return new RvrService();
    }

    @Bean
    @Lazy
    public IuService iuService() {
        return new IuService();
    }

    @Bean
    @Lazy
    public NightmareCircusService nightmareCircusService() {
        return new NightmareCircusService();
    }

    @Bean
    @Lazy
    public DynamicRiftService dynamicRiftService() {
        return new DynamicRiftService();
    }

    @Bean
    @Lazy
    public InstanceRiftService instanceRiftService() {
        return new InstanceRiftService();
    }

    @Bean
    @Lazy
    public ZorshivDredgionService zorshivDredgionService() {
        return new ZorshivDredgionService();
    }

    @Bean
    @Lazy
    public MoltenusService moltenusService() {
        return new MoltenusService();
    }

    @Bean
    @Lazy
    public RiftService riftService() {
        return new RiftService();
    }

    @Bean
    @Lazy
    public ConquestService conquestService() {
        return new ConquestService();
    }

    @Bean
    @Lazy
    public IdianDepthsService idianDepthsService() {
        return new IdianDepthsService();
    }

    @Bean
    @Lazy
    public TowerOfEternityService towerOfEternityService() {
        return new TowerOfEternityService();
    }

    @Bean
    @Lazy
    public AbyssLandingService abyssLandingService() {
        return new AbyssLandingService();
    }

    @Bean
    @Lazy
    public LandingUpdateService landingUpdateService() {
        return LandingUpdateService.getInstance();
    }

    @Bean
    @Lazy
    public AbyssLandingSpecialService abyssLandingSpecialService() {
        return new AbyssLandingSpecialService();
    }
}
