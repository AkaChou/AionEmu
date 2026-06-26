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
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.services.drop.DropDistributionService;
import com.aionemu.gameserver.services.mail.MailService;
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
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import com.aionemu.gameserver.world.zone.ZoneService;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
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
    public HousingBidService housingBidService() throws ParseException {
        return new HousingBidService();
    }

    @Bean
    @Lazy
    public MaintenanceTask maintenanceTask() throws ParseException {
        return new MaintenanceTask(HousingConfig.HOUSE_MAINTENANCE_TIME);
    }

    @Bean
    @Lazy
    public TownService townService() {
        return new TownService();
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
        return new ThreadPoolManager();
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
        return new TaskManagerFromDB();
    }

    @Bean
    @Lazy
    public LimitedItemTradeService limitedItemTradeService() {
        return new LimitedItemTradeService();
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
        return new DatabaseCleaningService();
    }

    @Bean
    @Lazy
    public AbyssRankCleaningService abyssRankCleaningService() {
        return new AbyssRankCleaningService();
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
        return new DataManager();
    }

    @Bean
    @Lazy
    public HTMLCache htmlCache() {
        return new HTMLCache();
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
        return new IDFactory();
    }

    @Bean
    @Lazy
    public ZoneService zoneService() {
        return new ZoneService();
    }

    @Bean
    @Lazy
    public HotspotTeleportService hotspotTeleportService() {
        return new HotspotTeleportService();
    }

    @Bean
    @Lazy
    public RoadService roadService() {
        return new RoadService();
    }

    @Bean
    @Lazy
    public World world() {
        return new World();
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
        return new LoginServer();
    }

    @Bean
    @Lazy
    public ChatServer chatServer() {
        return new ChatServer();
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
        return new LandingUpdateService();
    }

    @Bean
    @Lazy
    public AbyssLandingSpecialService abyssLandingSpecialService() {
        return new AbyssLandingSpecialService();
    }

    @Bean
    @Lazy
    public AStationService aStationService() {
        return new AStationService();
    }

    @Bean
    @Lazy
    public F2pService f2pService() {
        return new F2pService();
    }

    @Bean
    @Lazy
    public WindyGorgeService windyGorgeService() {
        return new WindyGorgeService();
    }

    @Bean
    @Lazy
    public MotionLoggingService motionLoggingService() {
        return new MotionLoggingService();
    }

    @Bean
    @Lazy
    public StaticDoorService staticDoorService() {
        return new StaticDoorService();
    }

    @Bean
    @Lazy
    public KiskService kiskService() {
        return new KiskService();
    }

    @Bean
    @Lazy
    public RepurchaseService repurchaseService() {
        return new RepurchaseService();
    }

    @Bean
    @Lazy
    public DropDistributionService dropDistributionService() {
        return new DropDistributionService();
    }

    @Bean
    @Lazy
    public SystemMailService systemMailService() {
        return new SystemMailService();
    }

    @Bean
    @Lazy
    public BonusService bonusService() {
        return new BonusService();
    }

    @Bean
    @Lazy
    public PetService petService() {
        return new PetService();
    }

    @Bean
    @Lazy
    public ArcadeUpgradeService arcadeUpgradeService() {
        return new ArcadeUpgradeService();
    }

    @Bean
    @Lazy
    public AtreianBestiaryService atreianBestiaryService() {
        return new AtreianBestiaryService();
    }

    @Bean
    @Lazy
    public CoalescenceService coalescenceService() {
        return new CoalescenceService();
    }

    @Bean
    @Lazy
    public GrowthEnergy growthEnergy() {
        return new GrowthEnergy();
    }

    @Bean
    @Lazy
    public ExpireTimerTask expireTimerTask() {
        return new ExpireTimerTask();
    }

    @Bean
    @Lazy
    public TeamEffectUpdater teamEffectUpdater() {
        return new TeamEffectUpdater();
    }

    @Bean
    @Lazy
    public TeamMoveUpdater teamMoveUpdater() {
        return new TeamMoveUpdater();
    }

    @Bean
    @Lazy
    public TemporaryTradeTimeTask temporaryTradeTimeTask() {
        return new TemporaryTradeTimeTask();
    }

    @Bean
    @Lazy
    public CreativityEssenceService creativityEssenceService() {
        return new CreativityEssenceService();
    }

    @Bean
    @Lazy
    public CreativitySkillService creativitySkillService() {
        return new CreativitySkillService();
    }

    @Bean
    @Lazy
    public CreativityStatsService creativityStatsService() {
        return new CreativityStatsService();
    }

    @Bean
    @Lazy
    public CreativityTransfoService creativityTransfoService() {
        return new CreativityTransfoService();
    }

    @Bean
    @Lazy
    public Accuracy accuracy() {
        return new Accuracy();
    }

    @Bean
    @Lazy
    public Agility agility() {
        return new Agility();
    }

    @Bean
    @Lazy
    public Health health() {
        return new Health();
    }

    @Bean
    @Lazy
    public Knowledge knowledge() {
        return new Knowledge();
    }

    @Bean
    @Lazy
    public Power power() {
        return new Power();
    }

    @Bean
    @Lazy
    public Precision precision() {
        return new Precision();
    }

    @Bean
    @Lazy
    public Will will() {
        return new Will();
    }

    @Bean
    @Lazy
    public CraftSkillUpdateService craftSkillUpdateService() {
        return new CraftSkillUpdateService();
    }

    @Bean
    @Lazy
    public RelinquishCraftStatus relinquishCraftStatus() {
        return new RelinquishCraftStatus();
    }

    @Bean
    @Lazy
    public DuelService duelService() {
        return new DuelService();
    }

    @Bean
    public LifeStatsRestoreService lifeStatsRestoreService() {
        return new LifeStatsRestoreService();
    }

    @Bean
    @Lazy
    public SeasonRankingService seasonRankingService() {
        return new SeasonRankingService();
    }

    @Bean
    @Lazy
    public RiftManager riftManager() {
        return new RiftManager();
    }

    @Bean
    public DropService dropService() {
        return new DropService();
    }

    @Bean
    public MailService mailService() {
        return new MailService();
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
    @Lazy
    public MovementNotifyTask movementNotifyTask() {
        return new MovementNotifyTask();
    }

    @Bean
    @Lazy
    public MoveTaskManager moveTaskManager() {
        return new MoveTaskManager();
    }

    @Bean
    @Lazy
    public PlayerMoveTaskManager playerMoveTaskManager() {
        return new PlayerMoveTaskManager();
    }

    @Bean
    @Lazy
    public ZoneUpdateService zoneUpdateService() {
        return new ZoneUpdateService();
    }
}
