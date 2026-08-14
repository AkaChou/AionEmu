package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
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
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 运行时服务集合启动网关：按固定顺序解析并初始化各运行时服务，
 * 优先使用 Spring 注入的 {@link ObjectProvider}，否则回退 {@link GameRuntimeServiceBridge}。
 * Gateway that starts the runtime services set in a fixed order, preferring Spring
 * {@link ObjectProvider}s and falling back to {@link GameRuntimeServiceBridge}.
 */
@Component
public class GameRuntimeServicesGateway {

    /**
     * 管理服务提供者（可选）。
     * Optional admin service provider.
     */
    private ObjectProvider<AdminService> adminServiceProvider;

    /**
     * 角色转移服务提供者（可选）。
     * Optional player-transfer service provider.
     */
    private ObjectProvider<PlayerTransferService> playerTransferServiceProvider;

    /**
     * 周期存档服务提供者（可选）。
     * Optional periodic-save service provider.
     */
    private ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider;

    /**
     * 领地服务提供者（可选）。
     * Optional territory service provider.
     */
    private ObjectProvider<TerritoryService> territoryServiceProvider;

    /**
     * 游戏时间服务提供者（可选）。
     * Optional game-time service provider.
     */
    private ObjectProvider<GameTimeService> gameTimeServiceProvider;

    /**
     * 公告服务提供者（可选）。
     * Optional announcement service provider.
     */
    private ObjectProvider<AnnouncementService> announcementServiceProvider;

    /**
     * 调试服务提供者（可选）。
     * Optional debug service provider.
     */
    private ObjectProvider<DebugService> debugServiceProvider;

    /**
     * 天气服务提供者（可选）。
     * Optional weather service provider.
     */
    private ObjectProvider<WeatherService> weatherServiceProvider;

    /**
     * 寄售服务提供者（可选）。
     * Optional broker service provider.
     */
    private ObjectProvider<BrokerService> brokerServiceProvider;

    /**
     * 军团服务提供者（可选）。
     * Optional legion service provider.
     */
    private ObjectProvider<LegionService> legionServiceProvider;

    /**
     * 影响力提供者（可选）。
     * Optional influence provider.
     */
    private ObjectProvider<Influence> influenceProvider;

    /**
     * 交易服务提供者（可选）。
     * Optional exchange service provider.
     */
    private ObjectProvider<ExchangeService> exchangeServiceProvider;

    /**
     * 请愿服务提供者（可选）。
     * Optional petition service provider.
     */
    private ObjectProvider<PetitionService> petitionServiceProvider;

    /**
     * 飞行环服务提供者（可选）。
     * Optional fly-ring service provider.
     */
    private ObjectProvider<FlyRingService> flyRingServiceProvider;

    /**
     * 治愈区服务提供者（可选）。
     * Optional curing-zone service provider.
     */
    private ObjectProvider<CuringZoneService> curingZoneServiceProvider;

    /**
     * 泉水区服务提供者（可选）。
     * Optional spring-zone service provider.
     */
    private ObjectProvider<SpringZoneService> springZoneServiceProvider;

    /**
     * 增益活动服务提供者（可选）。
     * Optional boost-event service provider.
     */
    private ObjectProvider<BoostEventService> boostEventServiceProvider;

    /**
     * DB 任务管理器提供者（可选）。
     * Optional DB task-manager provider.
     */
    private ObjectProvider<TaskManagerFromDB> taskManagerFromDBProvider;

    /**
     * 限购交易服务提供者（可选）。
     * Optional limited-item trade service provider.
     */
    private ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider;

    /**
     * Web 商城服务提供者（可选）。
     * Optional webshop service provider.
     */
    private ObjectProvider<WebshopService> webshopServiceProvider;

    /**
     * 问卷服务提供者（可选）。
     * Optional survey service provider.
     */
    private ObjectProvider<SurveyService> surveyServiceProvider;

    /**
     * 寻找队伍服务提供者（可选）。
     * Optional find-group service provider.
     */
    private ObjectProvider<FindGroupService> findGroupServiceProvider;

    /**
     * 商城服务提供者（可选）。
     * Optional in-game shop provider.
     */
    private ObjectProvider<InGameShopEn> inGameShopEnProvider;

    /**
     * 运行时服务桥接提供者（可选）。
     * Optional runtime service bridge provider.
     */
    private ObjectProvider<GameRuntimeServiceBridge> runtimeServiceBridgeProvider;

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
     * Inject the player-transfer service provider.
     *
     * @param playerTransferServiceProvider 角色转移服务提供者 / Player-transfer service provider
     */
    @Autowired(required = false)
    void setPlayerTransferServiceProvider(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        this.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    /**
     * 注入周期存档服务提供者。
     * Inject the periodic-save service provider.
     *
     * @param periodicSaveServiceProvider 周期存档服务提供者 / Periodic-save service provider
     */
    @Autowired(required = false)
    void setPeriodicSaveServiceProvider(ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider) {
        this.periodicSaveServiceProvider = periodicSaveServiceProvider;
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
     * Inject the game-time service provider.
     *
     * @param gameTimeServiceProvider 游戏时间服务提供者 / Game-time service provider
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
     * 注入寄售服务提供者。
     * Inject the broker service provider.
     *
     * @param brokerServiceProvider 寄售服务提供者 / Broker service provider
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
     * 注入影响力提供者。
     * Inject the influence provider.
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
     * Inject the fly-ring service provider.
     *
     * @param flyRingServiceProvider 飞行环服务提供者 / Fly-ring service provider
     */
    @Autowired(required = false)
    void setFlyRingServiceProvider(ObjectProvider<FlyRingService> flyRingServiceProvider) {
        this.flyRingServiceProvider = flyRingServiceProvider;
    }

    /**
     * 注入治愈区服务提供者。
     * Inject the curing-zone service provider.
     *
     * @param curingZoneServiceProvider 治愈区服务提供者 / Curing-zone service provider
     */
    @Autowired(required = false)
    void setCuringZoneServiceProvider(ObjectProvider<CuringZoneService> curingZoneServiceProvider) {
        this.curingZoneServiceProvider = curingZoneServiceProvider;
    }

    /**
     * 注入泉水区服务提供者。
     * Inject the spring-zone service provider.
     *
     * @param springZoneServiceProvider 泉水区服务提供者 / Spring-zone service provider
     */
    @Autowired(required = false)
    void setSpringZoneServiceProvider(ObjectProvider<SpringZoneService> springZoneServiceProvider) {
        this.springZoneServiceProvider = springZoneServiceProvider;
    }

    /**
     * 注入增益活动服务提供者。
     * Inject the boost-event service provider.
     *
     * @param boostEventServiceProvider 增益活动服务提供者 / Boost-event service provider
     */
    @Autowired(required = false)
    void setBoostEventServiceProvider(ObjectProvider<BoostEventService> boostEventServiceProvider) {
        this.boostEventServiceProvider = boostEventServiceProvider;
    }

    /**
     * 注入 DB 任务管理器提供者。
     * Inject the DB task-manager provider.
     *
     * @param taskManagerFromDBProvider DB 任务管理器提供者 / DB task-manager provider
     */
    @Autowired(required = false)
    void setTaskManagerFromDBProvider(ObjectProvider<TaskManagerFromDB> taskManagerFromDBProvider) {
        this.taskManagerFromDBProvider = taskManagerFromDBProvider;
    }

    /**
     * 注入限购交易服务提供者。
     * Inject the limited-item trade service provider.
     *
     * @param limitedItemTradeServiceProvider 限购交易服务提供者 / Limited-item trade service provider
     */
    @Autowired(required = false)
    void setLimitedItemTradeServiceProvider(ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider) {
        this.limitedItemTradeServiceProvider = limitedItemTradeServiceProvider;
    }

    /**
     * 注入 Web 商城服务提供者。
     * Inject the webshop service provider.
     *
     * @param webshopServiceProvider Web 商城服务提供者 / Webshop service provider
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
     * 注入寻找队伍服务提供者。
     * Inject the find-group service provider.
     *
     * @param findGroupServiceProvider 寻找队伍服务提供者 / Find-group service provider
     */
    @Autowired(required = false)
    void setFindGroupServiceProvider(ObjectProvider<FindGroupService> findGroupServiceProvider) {
        this.findGroupServiceProvider = findGroupServiceProvider;
    }

    /**
     * 注入商城服务提供者。
     * Inject the in-game shop provider.
     *
     * @param inGameShopEnProvider 商城服务提供者 / In-game shop provider
     */
    @Autowired(required = false)
    void setInGameShopEnProvider(ObjectProvider<InGameShopEn> inGameShopEnProvider) {
        this.inGameShopEnProvider = inGameShopEnProvider;
    }

    /**
     * 注入运行时服务桥接提供者。
     * Inject the runtime service bridge provider.
     *
     * @param runtimeServiceBridgeProvider 运行时服务桥接提供者 / Runtime service bridge provider
     */
    @Autowired(required = false)
    void setRuntimeServiceBridgeProvider(ObjectProvider<GameRuntimeServiceBridge> runtimeServiceBridgeProvider) {
        this.runtimeServiceBridgeProvider = runtimeServiceBridgeProvider;
    }

    /**
     * 启动运行时服务集合：打印分区、解析并初始化各服务、加载副本并启动游戏时钟。
     * Start the runtime services set: print section, resolve and initialize services,
     * load instances, and start the game-time clock.
     */
    public void start() {
        runtimeServiceBridge().printServicesSection();
        periodicSaveService();
        adminService();
        playerTransferService();
        territoryService().initTerritory();
        gameTimeService();
        announcementService();
        debugService();
        weatherService();
        brokerService();
        legionService();
        influence();
        exchangeService();
        petitionService();
        runtimeServiceBridge().loadInstances();
        flyRingService();
        curingZoneService();
        springZoneService();
        boostEventService().onStart();
        taskManagerFromDB();
        limitedItemTradeService().start();
        webshopService();
        surveyService();
        findGroupService();
        inGameShopEn();
        runtimeServiceBridge().startGameTimeClock();
    }

    /**
     * 解析周期存档服务。
     * Resolve the periodic-save service.
     *
     * @return 周期存档服务 / Periodic-save service
     */
    private PeriodicSaveService periodicSaveService() {
        if (periodicSaveServiceProvider == null) {
            return runtimeServiceBridge().periodicSaveService();
        }
        return periodicSaveServiceProvider.getIfAvailable(() -> runtimeServiceBridge().periodicSaveService());
    }

    /**
     * 解析管理服务。
     * Resolve the admin service.
     *
     * @return 管理服务 / Admin service
     */
    private AdminService adminService() {
        if (adminServiceProvider == null) {
            return runtimeServiceBridge().adminService();
        }
        return adminServiceProvider.getIfAvailable(() -> runtimeServiceBridge().adminService());
    }

    /**
     * 解析角色转移服务。
     * Resolve the player-transfer service.
     *
     * @return 角色转移服务 / Player-transfer service
     */
    private PlayerTransferService playerTransferService() {
        if (playerTransferServiceProvider == null) {
            return runtimeServiceBridge().playerTransferService();
        }
        return playerTransferServiceProvider.getIfAvailable(() -> runtimeServiceBridge().playerTransferService());
    }

    /**
     * 解析领地服务。
     * Resolve the territory service.
     *
     * @return 领地服务 / Territory service
     */
    private TerritoryService territoryService() {
        if (territoryServiceProvider == null) {
            return runtimeServiceBridge().territoryService();
        }
        return territoryServiceProvider.getIfAvailable(() -> runtimeServiceBridge().territoryService());
    }

    /**
     * 解析游戏时间服务。
     * Resolve the game-time service.
     *
     * @return 游戏时间服务 / Game-time service
     */
    private GameTimeService gameTimeService() {
        if (gameTimeServiceProvider == null) {
            return runtimeServiceBridge().gameTimeService();
        }
        return gameTimeServiceProvider.getIfAvailable(() -> runtimeServiceBridge().gameTimeService());
    }

    /**
     * 解析公告服务。
     * Resolve the announcement service.
     *
     * @return 公告服务 / Announcement service
     */
    private AnnouncementService announcementService() {
        if (announcementServiceProvider == null) {
            return runtimeServiceBridge().announcementService();
        }
        return announcementServiceProvider.getIfAvailable(() -> runtimeServiceBridge().announcementService());
    }

    /**
     * 解析调试服务。
     * Resolve the debug service.
     *
     * @return 调试服务 / Debug service
     */
    private DebugService debugService() {
        if (debugServiceProvider == null) {
            return runtimeServiceBridge().debugService();
        }
        return debugServiceProvider.getIfAvailable(() -> runtimeServiceBridge().debugService());
    }

    /**
     * 解析天气服务。
     * Resolve the weather service.
     *
     * @return 天气服务 / Weather service
     */
    private WeatherService weatherService() {
        if (weatherServiceProvider == null) {
            return runtimeServiceBridge().weatherService();
        }
        return weatherServiceProvider.getIfAvailable(() -> runtimeServiceBridge().weatherService());
    }

    /**
     * 解析寄售服务。
     * Resolve the broker service.
     *
     * @return 寄售服务 / Broker service
     */
    private BrokerService brokerService() {
        if (brokerServiceProvider == null) {
            return runtimeServiceBridge().brokerService();
        }
        return brokerServiceProvider.getIfAvailable(() -> runtimeServiceBridge().brokerService());
    }

    /**
     * 解析军团服务。
     * Resolve the legion service.
     *
     * @return 军团服务 / Legion service
     */
    private LegionService legionService() {
        if (legionServiceProvider == null) {
            return runtimeServiceBridge().legionService();
        }
        return legionServiceProvider.getIfAvailable(() -> runtimeServiceBridge().legionService());
    }

    /**
     * 解析影响力。
     * Resolve influence.
     *
     * @return 影响力 / Influence
     */
    private Influence influence() {
        if (influenceProvider == null) {
            return runtimeServiceBridge().influence();
        }
        return influenceProvider.getIfAvailable(() -> runtimeServiceBridge().influence());
    }

    /**
     * 解析交易服务。
     * Resolve the exchange service.
     *
     * @return 交易服务 / Exchange service
     */
    private ExchangeService exchangeService() {
        if (exchangeServiceProvider == null) {
            return runtimeServiceBridge().exchangeService();
        }
        return exchangeServiceProvider.getIfAvailable(() -> runtimeServiceBridge().exchangeService());
    }

    /**
     * 解析请愿服务。
     * Resolve the petition service.
     *
     * @return 请愿服务 / Petition service
     */
    private PetitionService petitionService() {
        if (petitionServiceProvider == null) {
            return runtimeServiceBridge().petitionService();
        }
        return petitionServiceProvider.getIfAvailable(() -> runtimeServiceBridge().petitionService());
    }

    /**
     * 解析飞行环服务。
     * Resolve the fly-ring service.
     *
     * @return 飞行环服务 / Fly-ring service
     */
    private FlyRingService flyRingService() {
        if (flyRingServiceProvider == null) {
            return runtimeServiceBridge().flyRingService();
        }
        return flyRingServiceProvider.getIfAvailable(() -> runtimeServiceBridge().flyRingService());
    }

    /**
     * 解析治愈区服务。
     * Resolve the curing-zone service.
     *
     * @return 治愈区服务 / Curing-zone service
     */
    private CuringZoneService curingZoneService() {
        if (curingZoneServiceProvider == null) {
            return runtimeServiceBridge().curingZoneService();
        }
        return curingZoneServiceProvider.getIfAvailable(() -> runtimeServiceBridge().curingZoneService());
    }

    /**
     * 解析泉水区服务。
     * Resolve the spring-zone service.
     *
     * @return 泉水区服务 / Spring-zone service
     */
    private SpringZoneService springZoneService() {
        if (springZoneServiceProvider == null) {
            return runtimeServiceBridge().springZoneService();
        }
        return springZoneServiceProvider.getIfAvailable(() -> runtimeServiceBridge().springZoneService());
    }

    /**
     * 解析增益活动服务。
     * Resolve the boost-event service.
     *
     * @return 增益活动服务 / Boost-event service
     */
    private BoostEventService boostEventService() {
        if (boostEventServiceProvider == null) {
            return runtimeServiceBridge().boostEventService();
        }
        return boostEventServiceProvider.getIfAvailable(() -> runtimeServiceBridge().boostEventService());
    }

    /**
     * 解析 DB 任务管理器。
     * Resolve the DB task manager.
     *
     * @return DB 任务管理器 / DB task manager
     */
    private TaskManagerFromDB taskManagerFromDB() {
        if (taskManagerFromDBProvider == null) {
            return runtimeServiceBridge().taskManagerFromDB();
        }
        return taskManagerFromDBProvider.getIfAvailable(() -> runtimeServiceBridge().taskManagerFromDB());
    }

    /**
     * 解析限购交易服务。
     * Resolve the limited-item trade service.
     *
     * @return 限购交易服务 / Limited-item trade service
     */
    private LimitedItemTradeService limitedItemTradeService() {
        if (limitedItemTradeServiceProvider == null) {
            return runtimeServiceBridge().limitedItemTradeService();
        }
        return limitedItemTradeServiceProvider.getIfAvailable(() -> runtimeServiceBridge().limitedItemTradeService());
    }

    /**
     * 解析 Web 商城服务。
     * Resolve the webshop service.
     *
     * @return Web 商城服务 / Webshop service
     */
    private WebshopService webshopService() {
        if (webshopServiceProvider == null) {
            return runtimeServiceBridge().webshopService();
        }
        return webshopServiceProvider.getIfAvailable(() -> runtimeServiceBridge().webshopService());
    }

    /**
     * 解析问卷服务。
     * Resolve the survey service.
     *
     * @return 问卷服务 / Survey service
     */
    private SurveyService surveyService() {
        if (surveyServiceProvider == null) {
            return runtimeServiceBridge().surveyService();
        }
        return surveyServiceProvider.getIfAvailable(() -> runtimeServiceBridge().surveyService());
    }

    /**
     * 解析寻找队伍服务。
     * Resolve the find-group service.
     *
     * @return 寻找队伍服务 / Find-group service
     */
    private FindGroupService findGroupService() {
        if (findGroupServiceProvider == null) {
            return runtimeServiceBridge().findGroupService();
        }
        return findGroupServiceProvider.getIfAvailable(() -> runtimeServiceBridge().findGroupService());
    }

    /**
     * 解析商城服务。
     * Resolve the in-game shop.
     *
     * @return 商城服务 / In-game shop
     */
    private InGameShopEn inGameShopEn() {
        if (inGameShopEnProvider == null) {
            return runtimeServiceBridge().inGameShopEn();
        }
        return inGameShopEnProvider.getIfAvailable(() -> runtimeServiceBridge().inGameShopEn());
    }

    /**
     * 解析运行时服务桥接。
     * Resolve the runtime service bridge.
     *
     * @return 运行时服务桥接 / Runtime service bridge
     */
    private GameRuntimeServiceBridge runtimeServiceBridge() {
        if (runtimeServiceBridgeProvider == null) {
            return new GameRuntimeServiceBridge();
        }
        return runtimeServiceBridgeProvider.getIfAvailable(GameRuntimeServiceBridge::new);
    }
}
