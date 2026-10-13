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
import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.persistence.dao.ServerVariableDao;
import com.aionemu.gameserver.services.WebshopService;
import com.aionemu.gameserver.services.events.BoostEventService;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 运行时服务集合生命周期：按固定顺序解析并初始化各运行时服务，加载状态由基类承载，
 * 由 Spring 注入的 {@link ObjectProvider} 解析各运行时服务。
 * Lifecycle for the runtime services set, started in a fixed order, preferring Spring
 * via Spring-injected {@link ObjectProvider}s; load state is hosted by the base class.
 */
@Component
public class GameRuntimeServicesLifecycle extends GameStartupStage {

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
     * 服务器变量 DAO 提供者（可选）。
     * Optional server-variable DAO provider.
     */
    private ObjectProvider<ServerVariableDao> serverVariableDaoProvider;

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
     * 注入管理服务提供者。
     * Inject the admin service provider.
     * @param adminServiceProvider 管理服务提供者 / Admin service provider
     */
    @Autowired(required = false)
    void setAdminServiceProvider(ObjectProvider<AdminService> adminServiceProvider) {
        this.adminServiceProvider = adminServiceProvider;
    }

    /**
     * 注入角色转移服务提供者。
     * Inject the player-transfer service provider.
     * @param playerTransferServiceProvider 角色转移服务提供者 / Player-transfer service provider
     */
    @Autowired(required = false)
    void setPlayerTransferServiceProvider(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        this.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    /**
     * 注入周期存档服务提供者。
     * Inject the periodic-save service provider.
     * @param periodicSaveServiceProvider 周期存档服务提供者 / Periodic-save service provider
     */
    @Autowired(required = false)
    void setPeriodicSaveServiceProvider(ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider) {
        this.periodicSaveServiceProvider = periodicSaveServiceProvider;
    }

    /**
     * 注入领地服务提供者。
     * Inject the territory service provider.
     * @param territoryServiceProvider 领地服务提供者 / Territory service provider
     */
    @Autowired(required = false)
    void setTerritoryServiceProvider(ObjectProvider<TerritoryService> territoryServiceProvider) {
        this.territoryServiceProvider = territoryServiceProvider;
    }

    /**
     * 注入游戏时间服务提供者。
     * Inject the game-time service provider.
     * @param gameTimeServiceProvider 游戏时间服务提供者 / Game-time service provider
     */
    @Autowired(required = false)
    void setGameTimeServiceProvider(ObjectProvider<GameTimeService> gameTimeServiceProvider) {
        this.gameTimeServiceProvider = gameTimeServiceProvider;
    }

    /**
     * 可选注入服务器变量 DAO 提供者。
     * Optionally inject the server-variable DAO provider.
     * @param serverVariableDaoProvider 服务器变量 DAO 提供者 / Server-variable DAO provider
     */
    @Autowired(required = false)
    void setServerVariableDaoProvider(ObjectProvider<ServerVariableDao> serverVariableDaoProvider) {
        this.serverVariableDaoProvider = serverVariableDaoProvider;
    }

    /**
     * 注入公告服务提供者。
     * Inject the announcement service provider.
     * @param announcementServiceProvider 公告服务提供者 / Announcement service provider
     */
    @Autowired(required = false)
    void setAnnouncementServiceProvider(ObjectProvider<AnnouncementService> announcementServiceProvider) {
        this.announcementServiceProvider = announcementServiceProvider;
    }

    /**
     * 注入调试服务提供者。
     * Inject the debug service provider.
     * @param debugServiceProvider 调试服务提供者 / Debug service provider
     */
    @Autowired(required = false)
    void setDebugServiceProvider(ObjectProvider<DebugService> debugServiceProvider) {
        this.debugServiceProvider = debugServiceProvider;
    }

    /**
     * 注入天气服务提供者。
     * Inject the weather service provider.
     * @param weatherServiceProvider 天气服务提供者 / Weather service provider
     */
    @Autowired(required = false)
    void setWeatherServiceProvider(ObjectProvider<WeatherService> weatherServiceProvider) {
        this.weatherServiceProvider = weatherServiceProvider;
    }

    /**
     * 注入寄售服务提供者。
     * Inject the broker service provider.
     * @param brokerServiceProvider 寄售服务提供者 / Broker service provider
     */
    @Autowired(required = false)
    void setBrokerServiceProvider(ObjectProvider<BrokerService> brokerServiceProvider) {
        this.brokerServiceProvider = brokerServiceProvider;
    }

    /**
     * 注入军团服务提供者。
     * Inject the legion service provider.
     * @param legionServiceProvider 军团服务提供者 / Legion service provider
     */
    @Autowired(required = false)
    void setLegionServiceProvider(ObjectProvider<LegionService> legionServiceProvider) {
        this.legionServiceProvider = legionServiceProvider;
    }

    /**
     * 注入影响力提供者。
     * Inject the influence provider.
     * @param influenceProvider 影响力提供者 / Influence provider
     */
    @Autowired(required = false)
    void setInfluenceProvider(ObjectProvider<Influence> influenceProvider) {
        this.influenceProvider = influenceProvider;
    }

    /**
     * 注入交易服务提供者。
     * Inject the exchange service provider.
     * @param exchangeServiceProvider 交易服务提供者 / Exchange service provider
     */
    @Autowired(required = false)
    void setExchangeServiceProvider(ObjectProvider<ExchangeService> exchangeServiceProvider) {
        this.exchangeServiceProvider = exchangeServiceProvider;
    }

    /**
     * 注入请愿服务提供者。
     * Inject the petition service provider.
     * @param petitionServiceProvider 请愿服务提供者 / Petition service provider
     */
    @Autowired(required = false)
    void setPetitionServiceProvider(ObjectProvider<PetitionService> petitionServiceProvider) {
        this.petitionServiceProvider = petitionServiceProvider;
    }

    /**
     * 注入飞行环服务提供者。
     * Inject the fly-ring service provider.
     * @param flyRingServiceProvider 飞行环服务提供者 / Fly-ring service provider
     */
    @Autowired(required = false)
    void setFlyRingServiceProvider(ObjectProvider<FlyRingService> flyRingServiceProvider) {
        this.flyRingServiceProvider = flyRingServiceProvider;
    }

    /**
     * 注入治愈区服务提供者。
     * Inject the curing-zone service provider.
     * @param curingZoneServiceProvider 治愈区服务提供者 / Curing-zone service provider
     */
    @Autowired(required = false)
    void setCuringZoneServiceProvider(ObjectProvider<CuringZoneService> curingZoneServiceProvider) {
        this.curingZoneServiceProvider = curingZoneServiceProvider;
    }

    /**
     * 注入泉水区服务提供者。
     * Inject the spring-zone service provider.
     * @param springZoneServiceProvider 泉水区服务提供者 / Spring-zone service provider
     */
    @Autowired(required = false)
    void setSpringZoneServiceProvider(ObjectProvider<SpringZoneService> springZoneServiceProvider) {
        this.springZoneServiceProvider = springZoneServiceProvider;
    }

    /**
     * 注入增益活动服务提供者。
     * Inject the boost-event service provider.
     * @param boostEventServiceProvider 增益活动服务提供者 / Boost-event service provider
     */
    @Autowired(required = false)
    void setBoostEventServiceProvider(ObjectProvider<BoostEventService> boostEventServiceProvider) {
        this.boostEventServiceProvider = boostEventServiceProvider;
    }

    /**
     * 注入 DB 任务管理器提供者。
     * Inject the DB task-manager provider.
     * @param taskManagerFromDBProvider DB 任务管理器提供者 / DB task-manager provider
     */
    @Autowired(required = false)
    void setTaskManagerFromDBProvider(ObjectProvider<TaskManagerFromDB> taskManagerFromDBProvider) {
        this.taskManagerFromDBProvider = taskManagerFromDBProvider;
    }

    /**
     * 注入限购交易服务提供者。
     * Inject the limited-item trade service provider.
     * @param limitedItemTradeServiceProvider 限购交易服务提供者 / Limited-item trade service provider
     */
    @Autowired(required = false)
    void setLimitedItemTradeServiceProvider(ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider) {
        this.limitedItemTradeServiceProvider = limitedItemTradeServiceProvider;
    }

    /**
     * 注入 Web 商城服务提供者。
     * Inject the webshop service provider.
     * @param webshopServiceProvider Web 商城服务提供者 / Webshop service provider
     */
    @Autowired(required = false)
    void setWebshopServiceProvider(ObjectProvider<WebshopService> webshopServiceProvider) {
        this.webshopServiceProvider = webshopServiceProvider;
    }

    /**
     * 注入问卷服务提供者。
     * Inject the survey service provider.
     * @param surveyServiceProvider 问卷服务提供者 / Survey service provider
     */
    @Autowired(required = false)
    void setSurveyServiceProvider(ObjectProvider<SurveyService> surveyServiceProvider) {
        this.surveyServiceProvider = surveyServiceProvider;
    }

    /**
     * 注入寻找队伍服务提供者。
     * Inject the find-group service provider.
     * @param findGroupServiceProvider 寻找队伍服务提供者 / Find-group service provider
     */
    @Autowired(required = false)
    void setFindGroupServiceProvider(ObjectProvider<FindGroupService> findGroupServiceProvider) {
        this.findGroupServiceProvider = findGroupServiceProvider;
    }

    /**
     * 注入商城服务提供者。
     * Inject the in-game shop provider.
     * @param inGameShopEnProvider 商城服务提供者 / In-game shop provider
     */
    @Autowired(required = false)
    void setInGameShopEnProvider(ObjectProvider<InGameShopEn> inGameShopEnProvider) {
        this.inGameShopEnProvider = inGameShopEnProvider;
    }


    /**
     * 启动本阶段：打印分区、解析并初始化各服务、加载副本并启动游戏时钟。
     * Start this stage: print section, resolve and initialize services, load instances,
     * and start the game-time clock.
     */
    @Override
    protected void runStage() {
        printServicesSection();
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
        startGameTimeClock();
    }

    /**
     * 提前绑定游戏时间的持久化来源（幂等，须在任何读取游戏时间的阶段之前调用）。
     * Bind the game-time persistence source ahead of time (idempotent; must run before any phase that reads
     * game time).
     *
     * <p>与 {@link #start()} 分离：时钟启动仍留在完整阶段里，这里只保证游戏时间实例在任何读取它的阶段之前
     * 就绪（临时刷怪所在的 {@code eventRuntimeLifecycle} 早于本阶段）。
     * Separate from {@link #start()}: starting the clock stays in the full phase; this only guarantees the
     * game-time instance exists before any phase reads it (the temp-spawn {@code eventRuntimeLifecycle} runs
     * before this one).</p>
     */
    public synchronized void initializeGameTime() {
        bindGameTime();
    }

    /**
     * 打印运行时服务分区标题。
     * Print the runtime-services section header.
     */
    private void printServicesSection() {
        Util.printSection(I18n.get("console.section.services"));
    }

    /**
     * 初始化游戏时间：绑定服务器变量 DAO 并读取游戏时间。
     * Initialize game time: bind the server-variable DAO and read the game time.
     * <p>幂等：游戏时间已绑定时直接返回。</p>
     * <p>Idempotent: returns immediately once the game time is bound.</p>
     */
    private void bindGameTime() {
        if (GameTimeManager.getGameTime() != null) {
            return;
        }
        GameTimeManager.init(serverVariableDaoProvider.getObject());
    }

    /**
     * 启动游戏时间时钟。
     * Start the game-time clock.
     */
    private void startGameTimeClock() {
        bindGameTime();
        GameTimeManager.startClock();
    }

    /**
     * 解析周期存档服务。
     * Resolve the periodic-save service.
     */
    private void periodicSaveService() {
        periodicSaveServiceProvider.getObject();
    }

    /**
     * 解析管理服务。
     * Resolve the admin service.
     */
    private void adminService() {
        adminServiceProvider.getObject();
    }

    /**
     * 解析角色转移服务。
     * Resolve the player-transfer service.
     */
    private void playerTransferService() {
        playerTransferServiceProvider.getObject();
    }

    /**
     * 解析领地服务。
     * Resolve the territory service.
     * @return 领地服务 / Territory service
     */
    private TerritoryService territoryService() {
        return territoryServiceProvider.getObject();
    }

    /**
     * 解析游戏时间服务。
     * Resolve the game-time service.
     */
    private void gameTimeService() {
        gameTimeServiceProvider.getObject();
    }

    /**
     * 解析公告服务。
     * Resolve the announcement service.
     */
    private void announcementService() {
        announcementServiceProvider.getObject();
    }

    /**
     * 解析调试服务。
     * Resolve the debug service.
     */
    private void debugService() {
        debugServiceProvider.getObject();
    }

    /**
     * 解析天气服务。
     * Resolve the weather service.
     */
    private void weatherService() {
        weatherServiceProvider.getObject();
    }

    /**
     * 解析寄售服务。
     * Resolve the broker service.
     */
    private void brokerService() {
        brokerServiceProvider.getObject();
    }

    /**
     * 解析军团服务。
     * Resolve the legion service.
     */
    private void legionService() {
        legionServiceProvider.getObject();
    }

    /**
     * 解析影响力。
     * Resolve influence.
     */
    private void influence() {
        influenceProvider.getObject();
    }

    /**
     * 解析交易服务。
     * Resolve the exchange service.
     */
    private void exchangeService() {
        exchangeServiceProvider.getObject();
    }

    /**
     * 解析请愿服务。
     * Resolve the petition service.
     */
    private void petitionService() {
        petitionServiceProvider.getObject();
    }

    /**
     * 解析飞行环服务。
     * Resolve the fly-ring service.
     */
    private void flyRingService() {
        flyRingServiceProvider.getObject();
    }

    /**
     * 解析治愈区服务。
     * Resolve the curing-zone service.
     */
    private void curingZoneService() {
        curingZoneServiceProvider.getObject();
    }

    /**
     * 解析泉水区服务。
     * Resolve the spring-zone service.
     */
    private void springZoneService() {
        springZoneServiceProvider.getObject();
    }

    /**
     * 解析增益活动服务。
     * Resolve the boost-event service.
     * @return 增益活动服务 / Boost-event service
     */
    private BoostEventService boostEventService() {
        return boostEventServiceProvider.getObject();
    }

    /**
     * 解析 DB 任务管理器。
     * Resolve the DB task manager.
     */
    private void taskManagerFromDB() {
        taskManagerFromDBProvider.getObject();
    }

    /**
     * 解析限购交易服务。
     * Resolve the limited-item trade service.
     * @return 限购交易服务 / Limited-item trade service
     */
    private LimitedItemTradeService limitedItemTradeService() {
        return limitedItemTradeServiceProvider.getObject();
    }

    /**
     * 解析 Web 商城服务。
     * Resolve the webshop service.
     */
    private void webshopService() {
        webshopServiceProvider.getObject();
    }

    /**
     * 解析问卷服务。
     * Resolve the survey service.
     */
    private void surveyService() {
        surveyServiceProvider.getObject();
    }

    /**
     * 解析寻找队伍服务。
     * Resolve the find-group service.
     */
    private void findGroupService() {
        findGroupServiceProvider.getObject();
    }

    /**
     * 解析商城服务。
     * Resolve the in-game shop.
     */
    private void inGameShopEn() {
        inGameShopEnProvider.getObject();
    }
}
