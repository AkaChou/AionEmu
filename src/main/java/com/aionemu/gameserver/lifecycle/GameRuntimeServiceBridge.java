package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.services.AdminService;
import com.aionemu.gameserver.services.AnnouncementService;
import com.aionemu.gameserver.services.BrokerService;
import com.aionemu.gameserver.services.CuringZoneService;
import com.aionemu.gameserver.services.DebugService;
import com.aionemu.gameserver.services.ExchangeService;
import com.aionemu.gameserver.services.FindGroupService;
import com.aionemu.gameserver.services.FlyRingService;
import com.aionemu.gameserver.services.GameTimeService;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.LimitedItemTradeService;
import com.aionemu.gameserver.services.PeriodicSaveService;
import com.aionemu.gameserver.services.PetitionService;
import com.aionemu.gameserver.services.SpringZoneService;
import com.aionemu.gameserver.services.SurveyService;
import com.aionemu.gameserver.services.WeatherService;
import com.aionemu.gameserver.services.WebshopService;
import com.aionemu.gameserver.services.events.BoostEventService;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 运行时服务桥接：通过 {@link ObjectProvider} 或单例 {@code getInstance} 解析游戏运行时服务，
 * 并提供控制台分区输出、副本加载与游戏时间时钟启动。
 * RuntimeServiceBridge that resolves game runtime services via {@link ObjectProvider}
 * or singleton {@code getInstance}, and provides console section printing, instance loading,
 * and game-time clock start.
 *
 * <p>覆盖的服务包括：周期存档、管理、角色转移、领地、游戏时间、公告、调试、天气、拍卖行、
 * 军团、影响力、交易、请愿、飞行环、治愈区、泉水区、增益活动、DB 任务管理、限时道具交易、
 * 网店、问卷、找队与游戏内商城等。
 * Covered services include: PeriodicSave, Admin, Transfer, Territory, GameTime, Announcement,
 * Debug, Weather, Broker, Legion, Influence, Exchange, Petition, FlyRing, CuringZone, SpringZone,
 * BoostEvent, TaskManagerFromDB, LimitedItemTrade, Webshop, Survey, FindGroup, and InGameShop.</p>
 */
public class GameRuntimeServiceBridge {

    /**
     * 周期存档服务提供者。
     * Periodic save service provider.
     */
    private ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider;

    /**
     * 管理服务提供者。
     * Admin service provider.
     */
    private ObjectProvider<AdminService> adminServiceProvider;

    /**
     * 角色转移服务提供者。
     * Player transfer service provider.
     */
    private ObjectProvider<PlayerTransferService> playerTransferServiceProvider;

    /**
     * 领地服务提供者。
     * Territory service provider.
     */
    private ObjectProvider<TerritoryService> territoryServiceProvider;

    /**
     * 游戏时间服务提供者。
     * Game time service provider.
     */
    private ObjectProvider<GameTimeService> gameTimeServiceProvider;

    /**
     * 公告服务提供者。
     * Announcement service provider.
     */
    private ObjectProvider<AnnouncementService> announcementServiceProvider;

    /**
     * 调试服务提供者。
     * Debug service provider.
     */
    private ObjectProvider<DebugService> debugServiceProvider;

    /**
     * 天气服务提供者。
     * Weather service provider.
     */
    private ObjectProvider<WeatherService> weatherServiceProvider;

    /**
     * 拍卖行服务提供者。
     * Broker service provider.
     */
    private ObjectProvider<BrokerService> brokerServiceProvider;

    /**
     * 军团服务提供者。
     * Legion service provider.
     */
    private ObjectProvider<LegionService> legionServiceProvider;

    /**
     * 影响力模型提供者。
     * Influence model provider.
     */
    private ObjectProvider<Influence> influenceProvider;

    /**
     * 交易服务提供者。
     * Exchange service provider.
     */
    private ObjectProvider<ExchangeService> exchangeServiceProvider;

    /**
     * 请愿服务提供者。
     * Petition service provider.
     */
    private ObjectProvider<PetitionService> petitionServiceProvider;

    /**
     * 飞行环服务提供者。
     * Fly ring service provider.
     */
    private ObjectProvider<FlyRingService> flyRingServiceProvider;

    /**
     * 治愈区服务提供者。
     * Curing zone service provider.
     */
    private ObjectProvider<CuringZoneService> curingZoneServiceProvider;

    /**
     * 泉水区服务提供者。
     * Spring zone service provider.
     */
    private ObjectProvider<SpringZoneService> springZoneServiceProvider;

    /**
     * 增益活动服务提供者。
     * Boost event service provider.
     */
    private ObjectProvider<BoostEventService> boostEventServiceProvider;

    /**
     * DB 任务管理器提供者。
     * Task manager from DB provider.
     */
    private ObjectProvider<TaskManagerFromDB> taskManagerFromDBProvider;

    /**
     * 限时道具交易服务提供者。
     * Limited item trade service provider.
     */
    private ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider;

    /**
     * 网店服务提供者。
     * Webshop service provider.
     */
    private ObjectProvider<WebshopService> webshopServiceProvider;

    /**
     * 问卷服务提供者。
     * Survey service provider.
     */
    private ObjectProvider<SurveyService> surveyServiceProvider;

    /**
     * 找队服务提供者。
     * Find-group service provider.
     */
    private ObjectProvider<FindGroupService> findGroupServiceProvider;

    /**
     * 游戏内商城引擎提供者。
     * In-game shop engine provider.
     */
    private ObjectProvider<InGameShopEn> inGameShopEnProvider;

    /**
     * 注入周期存档服务提供者。
     * Inject the periodic save service provider.
     *
     * @param periodicSaveServiceProvider 周期存档服务提供者 / Periodic save service provider
     */
    @Autowired(required = false)
    void setPeriodicSaveServiceProvider(ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider) {
        this.periodicSaveServiceProvider = periodicSaveServiceProvider;
    }

    /**
     * 注入管理服务提供者。
     * Inject the admin service provider.
     *
     * @param adminServiceProvider 管理服务提供者 / Admin service provider
     */
    @Autowired(required = false)
    void setAdminServiceProvider(ObjectProvider<AdminService> adminServiceProvider) {
        this.adminServiceProvider = adminServiceProvider;
    }

    /**
     * 注入角色转移服务提供者。
     * Inject the player transfer service provider.
     *
     * @param playerTransferServiceProvider 角色转移服务提供者 / Player transfer service provider
     */
    @Autowired(required = false)
    void setPlayerTransferServiceProvider(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        this.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    /**
     * 注入领地服务提供者。
     * Inject the territory service provider.
     *
     * @param territoryServiceProvider 领地服务提供者 / Territory service provider
     */
    @Autowired(required = false)
    void setTerritoryServiceProvider(ObjectProvider<TerritoryService> territoryServiceProvider) {
        this.territoryServiceProvider = territoryServiceProvider;
    }

    /**
     * 注入游戏时间服务提供者。
     * Inject the game time service provider.
     *
     * @param gameTimeServiceProvider 游戏时间服务提供者 / Game time service provider
     */
    @Autowired(required = false)
    void setGameTimeServiceProvider(ObjectProvider<GameTimeService> gameTimeServiceProvider) {
        this.gameTimeServiceProvider = gameTimeServiceProvider;
    }

    /**
     * 注入公告服务提供者。
     * Inject the announcement service provider.
     *
     * @param announcementServiceProvider 公告服务提供者 / Announcement service provider
     */
    @Autowired(required = false)
    void setAnnouncementServiceProvider(ObjectProvider<AnnouncementService> announcementServiceProvider) {
        this.announcementServiceProvider = announcementServiceProvider;
    }

    /**
     * 注入调试服务提供者。
     * Inject the debug service provider.
     *
     * @param debugServiceProvider 调试服务提供者 / Debug service provider
     */
    @Autowired(required = false)
    void setDebugServiceProvider(ObjectProvider<DebugService> debugServiceProvider) {
        this.debugServiceProvider = debugServiceProvider;
    }

    /**
     * 注入天气服务提供者。
     * Inject the weather service provider.
     *
     * @param weatherServiceProvider 天气服务提供者 / Weather service provider
     */
    @Autowired(required = false)
    void setWeatherServiceProvider(ObjectProvider<WeatherService> weatherServiceProvider) {
        this.weatherServiceProvider = weatherServiceProvider;
    }

    /**
     * 注入拍卖行服务提供者。
     * Inject the broker service provider.
     *
     * @param brokerServiceProvider 拍卖行服务提供者 / Broker service provider
     */
    @Autowired(required = false)
    void setBrokerServiceProvider(ObjectProvider<BrokerService> brokerServiceProvider) {
        this.brokerServiceProvider = brokerServiceProvider;
    }

    /**
     * 注入军团服务提供者。
     * Inject the legion service provider.
     *
     * @param legionServiceProvider 军团服务提供者 / Legion service provider
     */
    @Autowired(required = false)
    void setLegionServiceProvider(ObjectProvider<LegionService> legionServiceProvider) {
        this.legionServiceProvider = legionServiceProvider;
    }

    /**
     * 注入影响力模型提供者。
     * Inject the influence model provider.
     *
     * @param influenceProvider 影响力提供者 / Influence provider
     */
    @Autowired(required = false)
    void setInfluenceProvider(ObjectProvider<Influence> influenceProvider) {
        this.influenceProvider = influenceProvider;
    }

    /**
     * 注入交易服务提供者。
     * Inject the exchange service provider.
     *
     * @param exchangeServiceProvider 交易服务提供者 / Exchange service provider
     */
    @Autowired(required = false)
    void setExchangeServiceProvider(ObjectProvider<ExchangeService> exchangeServiceProvider) {
        this.exchangeServiceProvider = exchangeServiceProvider;
    }

    /**
     * 注入请愿服务提供者。
     * Inject the petition service provider.
     *
     * @param petitionServiceProvider 请愿服务提供者 / Petition service provider
     */
    @Autowired(required = false)
    void setPetitionServiceProvider(ObjectProvider<PetitionService> petitionServiceProvider) {
        this.petitionServiceProvider = petitionServiceProvider;
    }

    /**
     * 注入飞行环服务提供者。
     * Inject the fly ring service provider.
     *
     * @param flyRingServiceProvider 飞行环服务提供者 / Fly ring service provider
     */
    @Autowired(required = false)
    void setFlyRingServiceProvider(ObjectProvider<FlyRingService> flyRingServiceProvider) {
        this.flyRingServiceProvider = flyRingServiceProvider;
    }

    /**
     * 注入治愈区服务提供者。
     * Inject the curing zone service provider.
     *
     * @param curingZoneServiceProvider 治愈区服务提供者 / Curing zone service provider
     */
    @Autowired(required = false)
    void setCuringZoneServiceProvider(ObjectProvider<CuringZoneService> curingZoneServiceProvider) {
        this.curingZoneServiceProvider = curingZoneServiceProvider;
    }

    /**
     * 注入泉水区服务提供者。
     * Inject the spring zone service provider.
     *
     * @param springZoneServiceProvider 泉水区服务提供者 / Spring zone service provider
     */
    @Autowired(required = false)
    void setSpringZoneServiceProvider(ObjectProvider<SpringZoneService> springZoneServiceProvider) {
        this.springZoneServiceProvider = springZoneServiceProvider;
    }

    /**
     * 注入增益活动服务提供者。
     * Inject the boost event service provider.
     *
     * @param boostEventServiceProvider 增益活动服务提供者 / Boost event service provider
     */
    @Autowired(required = false)
    void setBoostEventServiceProvider(ObjectProvider<BoostEventService> boostEventServiceProvider) {
        this.boostEventServiceProvider = boostEventServiceProvider;
    }

    /**
     * 注入 DB 任务管理器提供者。
     * Inject the task manager from DB provider.
     *
     * @param taskManagerFromDBProvider DB 任务管理器提供者 / Task manager from DB provider
     */
    @Autowired(required = false)
    void setTaskManagerFromDBProvider(ObjectProvider<TaskManagerFromDB> taskManagerFromDBProvider) {
        this.taskManagerFromDBProvider = taskManagerFromDBProvider;
    }

    /**
     * 注入限时道具交易服务提供者。
     * Inject the limited item trade service provider.
     *
     * @param limitedItemTradeServiceProvider 限时道具交易服务提供者 / Limited item trade service provider
     */
    @Autowired(required = false)
    void setLimitedItemTradeServiceProvider(ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider) {
        this.limitedItemTradeServiceProvider = limitedItemTradeServiceProvider;
    }

    /**
     * 注入网店服务提供者。
     * Inject the webshop service provider.
     *
     * @param webshopServiceProvider 网店服务提供者 / Webshop service provider
     */
    @Autowired(required = false)
    void setWebshopServiceProvider(ObjectProvider<WebshopService> webshopServiceProvider) {
        this.webshopServiceProvider = webshopServiceProvider;
    }

    /**
     * 注入问卷服务提供者。
     * Inject the survey service provider.
     *
     * @param surveyServiceProvider 问卷服务提供者 / Survey service provider
     */
    @Autowired(required = false)
    void setSurveyServiceProvider(ObjectProvider<SurveyService> surveyServiceProvider) {
        this.surveyServiceProvider = surveyServiceProvider;
    }

    /**
     * 注入找队服务提供者。
     * Inject the find-group service provider.
     *
     * @param findGroupServiceProvider 找队服务提供者 / Find-group service provider
     */
    @Autowired(required = false)
    void setFindGroupServiceProvider(ObjectProvider<FindGroupService> findGroupServiceProvider) {
        this.findGroupServiceProvider = findGroupServiceProvider;
    }

    /**
     * 注入游戏内商城引擎提供者。
     * Inject the in-game shop engine provider.
     *
     * @param inGameShopEnProvider 游戏内商城引擎提供者 / In-game shop engine provider
     */
    @Autowired(required = false)
    void setInGameShopEnProvider(ObjectProvider<InGameShopEn> inGameShopEnProvider) {
        this.inGameShopEnProvider = inGameShopEnProvider;
    }

    /**
     * 输出运行时服务控制台分区标题。
     * Print the console section header for runtime services.
     */
    public void printServicesSection() {
        Util.printSection(I18n.get("console.section.services"));
    }

    /**
     * 解析周期存档服务。
     * Resolve the periodic save service.
     *
     * @return 周期存档服务 / Periodic save service
     */
    public PeriodicSaveService periodicSaveService() {
        return getIfAvailable(periodicSaveServiceProvider, PeriodicSaveService::getInstance);
    }

    /**
     * 解析管理服务。
     * Resolve the admin service.
     *
     * Admin service
     */
    public AdminService adminService() {
        return getIfAvailable(adminServiceProvider, AdminService::getInstance);
    }

    /**
     * 解析角色转移服务。
     * Resolve the player transfer service.
     *
     * @return 角色转移服务 / Player transfer service
     */
    public PlayerTransferService playerTransferService() {
        return getIfAvailable(playerTransferServiceProvider, PlayerTransferService::getInstance);
    }

    /**
     * 解析领地服务。
     * Resolve the territory service.
     *
     * Territory service
     */
    public TerritoryService territoryService() {
        return getIfAvailable(territoryServiceProvider, TerritoryService::getInstance);
    }

    /**
     * 解析游戏时间服务。
     * Resolve the game time service.
     *
     * @return 游戏时间服务 / Game time service
     */
    public GameTimeService gameTimeService() {
        return getIfAvailable(gameTimeServiceProvider, GameTimeService::getInstance);
    }

    /**
     * 解析公告服务。
     * Resolve the announcement service.
     *
     * Announcement service
     */
    public AnnouncementService announcementService() {
        return getIfAvailable(announcementServiceProvider, AnnouncementService::getInstance);
    }

    /**
     * 解析调试服务。
     * Resolve the debug service.
     *
     * Debug service
     */
    public DebugService debugService() {
        return getIfAvailable(debugServiceProvider, DebugService::getInstance);
    }

    /**
     * 解析天气服务。
     * Resolve the weather service.
     *
     * Weather service
     */
    public WeatherService weatherService() {
        return getIfAvailable(weatherServiceProvider, WeatherService::getInstance);
    }

    /**
     * 解析拍卖行服务。
     * Resolve the broker service.
     *
     * @return 拍卖行服务 / Broker service
     */
    public BrokerService brokerService() {
        return getIfAvailable(brokerServiceProvider, BrokerService::getInstance);
    }

    /**
     * 解析军团服务。
     * Resolve the legion service.
     *
     * Legion service
     */
    public LegionService legionService() {
        return getIfAvailable(legionServiceProvider, LegionService::getInstance);
    }

    /**
     * 解析影响力模型。
     * Resolve the influence model.
     *
     * @return 影响力模型 / Influence model
     */
    public Influence influence() {
        return getIfAvailable(influenceProvider, Influence::getInstance);
    }

    /**
     * 解析交易服务。
     * Resolve the exchange service.
     *
     * Exchange service
     */
    public ExchangeService exchangeService() {
        return getIfAvailable(exchangeServiceProvider, ExchangeService::getInstance);
    }

    /**
     * 解析请愿服务。
     * Resolve the petition service.
     *
     * Petition service
     */
    public PetitionService petitionService() {
        return getIfAvailable(petitionServiceProvider, PetitionService::getInstance);
    }

    /**
     * 解析飞行环服务。
     * Resolve the fly ring service.
     *
     * @return 飞行环服务 / Fly ring service
     */
    public FlyRingService flyRingService() {
        return getIfAvailable(flyRingServiceProvider, FlyRingService::getInstance);
    }

    /**
     * 解析治愈区服务。
     * Resolve the curing zone service.
     *
     * @return 治愈区服务 / Curing zone service
     */
    public CuringZoneService curingZoneService() {
        return getIfAvailable(curingZoneServiceProvider, CuringZoneService::getInstance);
    }

    /**
     * 解析泉水区服务。
     * Resolve the spring zone service.
     *
     * @return 泉水区服务 / Spring zone service
     */
    public SpringZoneService springZoneService() {
        return getIfAvailable(springZoneServiceProvider, SpringZoneService::getInstance);
    }

    /**
     * 解析增益活动服务。
     * Resolve the boost event service.
     *
     * @return 增益活动服务 / Boost event service
     */
    public BoostEventService boostEventService() {
        return getIfAvailable(boostEventServiceProvider, BoostEventService::getInstance);
    }

    /**
     * 解析 DB 任务管理器。
     * Resolve the task manager from DB.
     *
     * @return DB 任务管理器 / Task manager from DB
     */
    public TaskManagerFromDB taskManagerFromDB() {
        return getIfAvailable(taskManagerFromDBProvider, TaskManagerFromDB::getInstance);
    }

    /**
     * 解析限时道具交易服务。
     * Resolve the limited item trade service.
     *
     * @return 限时道具交易服务 / Limited item trade service
     */
    public LimitedItemTradeService limitedItemTradeService() {
        return getIfAvailable(limitedItemTradeServiceProvider, LimitedItemTradeService::getInstance);
    }

    /**
     * 解析网店服务。
     * Resolve the webshop service.
     *
     * Webshop service
     */
    public WebshopService webshopService() {
        return getIfAvailable(webshopServiceProvider, WebshopService::getInstance);
    }

    /**
     * 解析问卷服务。
     * Resolve the survey service.
     *
     * Survey service
     */
    public SurveyService surveyService() {
        return getIfAvailable(surveyServiceProvider, SurveyService::getInstance);
    }

    /**
     * 解析找队服务。
     * Resolve the find-group service.
     *
     * Find-group service
     */
    public FindGroupService findGroupService() {
        return getIfAvailable(findGroupServiceProvider, FindGroupService::getInstance);
    }

    /**
     * 解析游戏内商城引擎。
     * Resolve the in-game shop engine.
     *
     * @return 游戏内商城引擎 / In-game shop engine
     */
    public InGameShopEn inGameShopEn() {
        return getIfAvailable(inGameShopEnProvider, InGameShopEn::getInstance);
    }

    /**
     * 启动游戏时间时钟。
     * Start the game-time clock.
     */
    public void startGameTimeClock() {
        GameTimeManager.startClock();
    }

    /**
     * 在 {@link ObjectProvider} 可用时取 Bean，否则走单例回退。
     * Return the bean from {@link ObjectProvider} when available, otherwise use the singleton fallback.
     *
     * @param provider 可选提供者 / Optional provider
     * @param fallback 单例回退供应器 / Singleton fallback supplier
     * @param <T> 服务类型 / Service type
     * Service instance
     */
    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }
}
