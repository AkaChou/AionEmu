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

@Component
public class GameRuntimeServicesGateway {

    private ObjectProvider<AdminService> adminServiceProvider;
    private ObjectProvider<PlayerTransferService> playerTransferServiceProvider;
    private ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider;
    private ObjectProvider<TerritoryService> territoryServiceProvider;
    private ObjectProvider<GameTimeService> gameTimeServiceProvider;
    private ObjectProvider<AnnouncementService> announcementServiceProvider;
    private ObjectProvider<DebugService> debugServiceProvider;
    private ObjectProvider<WeatherService> weatherServiceProvider;
    private ObjectProvider<BrokerService> brokerServiceProvider;
    private ObjectProvider<LegionService> legionServiceProvider;
    private ObjectProvider<Influence> influenceProvider;
    private ObjectProvider<ExchangeService> exchangeServiceProvider;
    private ObjectProvider<PetitionService> petitionServiceProvider;
    private ObjectProvider<FlyRingService> flyRingServiceProvider;
    private ObjectProvider<CuringZoneService> curingZoneServiceProvider;
    private ObjectProvider<SpringZoneService> springZoneServiceProvider;
    private ObjectProvider<BoostEventService> boostEventServiceProvider;
    private ObjectProvider<TaskManagerFromDB> taskManagerFromDBProvider;
    private ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider;
    private ObjectProvider<WebshopService> webshopServiceProvider;
    private ObjectProvider<SurveyService> surveyServiceProvider;
    private ObjectProvider<FindGroupService> findGroupServiceProvider;
    private ObjectProvider<InGameShopEn> inGameShopEnProvider;
    private ObjectProvider<GameRuntimeServiceBridge> runtimeServiceBridgeProvider;

    @Autowired(required = false)
    void setAdminServiceProvider(ObjectProvider<AdminService> adminServiceProvider) {
        this.adminServiceProvider = adminServiceProvider;
    }

    @Autowired(required = false)
    void setPlayerTransferServiceProvider(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        this.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    @Autowired(required = false)
    void setPeriodicSaveServiceProvider(ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider) {
        this.periodicSaveServiceProvider = periodicSaveServiceProvider;
    }

    @Autowired(required = false)
    void setTerritoryServiceProvider(ObjectProvider<TerritoryService> territoryServiceProvider) {
        this.territoryServiceProvider = territoryServiceProvider;
    }

    @Autowired(required = false)
    void setGameTimeServiceProvider(ObjectProvider<GameTimeService> gameTimeServiceProvider) {
        this.gameTimeServiceProvider = gameTimeServiceProvider;
    }

    @Autowired(required = false)
    void setAnnouncementServiceProvider(ObjectProvider<AnnouncementService> announcementServiceProvider) {
        this.announcementServiceProvider = announcementServiceProvider;
    }

    @Autowired(required = false)
    void setDebugServiceProvider(ObjectProvider<DebugService> debugServiceProvider) {
        this.debugServiceProvider = debugServiceProvider;
    }

    @Autowired(required = false)
    void setWeatherServiceProvider(ObjectProvider<WeatherService> weatherServiceProvider) {
        this.weatherServiceProvider = weatherServiceProvider;
    }

    @Autowired(required = false)
    void setBrokerServiceProvider(ObjectProvider<BrokerService> brokerServiceProvider) {
        this.brokerServiceProvider = brokerServiceProvider;
    }

    @Autowired(required = false)
    void setLegionServiceProvider(ObjectProvider<LegionService> legionServiceProvider) {
        this.legionServiceProvider = legionServiceProvider;
    }

    @Autowired(required = false)
    void setInfluenceProvider(ObjectProvider<Influence> influenceProvider) {
        this.influenceProvider = influenceProvider;
    }

    @Autowired(required = false)
    void setExchangeServiceProvider(ObjectProvider<ExchangeService> exchangeServiceProvider) {
        this.exchangeServiceProvider = exchangeServiceProvider;
    }

    @Autowired(required = false)
    void setPetitionServiceProvider(ObjectProvider<PetitionService> petitionServiceProvider) {
        this.petitionServiceProvider = petitionServiceProvider;
    }

    @Autowired(required = false)
    void setFlyRingServiceProvider(ObjectProvider<FlyRingService> flyRingServiceProvider) {
        this.flyRingServiceProvider = flyRingServiceProvider;
    }

    @Autowired(required = false)
    void setCuringZoneServiceProvider(ObjectProvider<CuringZoneService> curingZoneServiceProvider) {
        this.curingZoneServiceProvider = curingZoneServiceProvider;
    }

    @Autowired(required = false)
    void setSpringZoneServiceProvider(ObjectProvider<SpringZoneService> springZoneServiceProvider) {
        this.springZoneServiceProvider = springZoneServiceProvider;
    }

    @Autowired(required = false)
    void setBoostEventServiceProvider(ObjectProvider<BoostEventService> boostEventServiceProvider) {
        this.boostEventServiceProvider = boostEventServiceProvider;
    }

    @Autowired(required = false)
    void setTaskManagerFromDBProvider(ObjectProvider<TaskManagerFromDB> taskManagerFromDBProvider) {
        this.taskManagerFromDBProvider = taskManagerFromDBProvider;
    }

    @Autowired(required = false)
    void setLimitedItemTradeServiceProvider(ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider) {
        this.limitedItemTradeServiceProvider = limitedItemTradeServiceProvider;
    }

    @Autowired(required = false)
    void setWebshopServiceProvider(ObjectProvider<WebshopService> webshopServiceProvider) {
        this.webshopServiceProvider = webshopServiceProvider;
    }

    @Autowired(required = false)
    void setSurveyServiceProvider(ObjectProvider<SurveyService> surveyServiceProvider) {
        this.surveyServiceProvider = surveyServiceProvider;
    }

    @Autowired(required = false)
    void setFindGroupServiceProvider(ObjectProvider<FindGroupService> findGroupServiceProvider) {
        this.findGroupServiceProvider = findGroupServiceProvider;
    }

    @Autowired(required = false)
    void setInGameShopEnProvider(ObjectProvider<InGameShopEn> inGameShopEnProvider) {
        this.inGameShopEnProvider = inGameShopEnProvider;
    }

    @Autowired(required = false)
    void setRuntimeServiceBridgeProvider(ObjectProvider<GameRuntimeServiceBridge> runtimeServiceBridgeProvider) {
        this.runtimeServiceBridgeProvider = runtimeServiceBridgeProvider;
    }

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

    private PeriodicSaveService periodicSaveService() {
        if (periodicSaveServiceProvider == null) {
            return runtimeServiceBridge().periodicSaveService();
        }
        return periodicSaveServiceProvider.getIfAvailable(() -> runtimeServiceBridge().periodicSaveService());
    }

    private AdminService adminService() {
        if (adminServiceProvider == null) {
            return runtimeServiceBridge().adminService();
        }
        return adminServiceProvider.getIfAvailable(() -> runtimeServiceBridge().adminService());
    }

    private PlayerTransferService playerTransferService() {
        if (playerTransferServiceProvider == null) {
            return runtimeServiceBridge().playerTransferService();
        }
        return playerTransferServiceProvider.getIfAvailable(() -> runtimeServiceBridge().playerTransferService());
    }

    private TerritoryService territoryService() {
        if (territoryServiceProvider == null) {
            return runtimeServiceBridge().territoryService();
        }
        return territoryServiceProvider.getIfAvailable(() -> runtimeServiceBridge().territoryService());
    }

    private GameTimeService gameTimeService() {
        if (gameTimeServiceProvider == null) {
            return runtimeServiceBridge().gameTimeService();
        }
        return gameTimeServiceProvider.getIfAvailable(() -> runtimeServiceBridge().gameTimeService());
    }

    private AnnouncementService announcementService() {
        if (announcementServiceProvider == null) {
            return runtimeServiceBridge().announcementService();
        }
        return announcementServiceProvider.getIfAvailable(() -> runtimeServiceBridge().announcementService());
    }

    private DebugService debugService() {
        if (debugServiceProvider == null) {
            return runtimeServiceBridge().debugService();
        }
        return debugServiceProvider.getIfAvailable(() -> runtimeServiceBridge().debugService());
    }

    private WeatherService weatherService() {
        if (weatherServiceProvider == null) {
            return runtimeServiceBridge().weatherService();
        }
        return weatherServiceProvider.getIfAvailable(() -> runtimeServiceBridge().weatherService());
    }

    private BrokerService brokerService() {
        if (brokerServiceProvider == null) {
            return runtimeServiceBridge().brokerService();
        }
        return brokerServiceProvider.getIfAvailable(() -> runtimeServiceBridge().brokerService());
    }

    private LegionService legionService() {
        if (legionServiceProvider == null) {
            return runtimeServiceBridge().legionService();
        }
        return legionServiceProvider.getIfAvailable(() -> runtimeServiceBridge().legionService());
    }

    private Influence influence() {
        if (influenceProvider == null) {
            return runtimeServiceBridge().influence();
        }
        return influenceProvider.getIfAvailable(() -> runtimeServiceBridge().influence());
    }

    private ExchangeService exchangeService() {
        if (exchangeServiceProvider == null) {
            return runtimeServiceBridge().exchangeService();
        }
        return exchangeServiceProvider.getIfAvailable(() -> runtimeServiceBridge().exchangeService());
    }

    private PetitionService petitionService() {
        if (petitionServiceProvider == null) {
            return runtimeServiceBridge().petitionService();
        }
        return petitionServiceProvider.getIfAvailable(() -> runtimeServiceBridge().petitionService());
    }

    private FlyRingService flyRingService() {
        if (flyRingServiceProvider == null) {
            return runtimeServiceBridge().flyRingService();
        }
        return flyRingServiceProvider.getIfAvailable(() -> runtimeServiceBridge().flyRingService());
    }

    private CuringZoneService curingZoneService() {
        if (curingZoneServiceProvider == null) {
            return runtimeServiceBridge().curingZoneService();
        }
        return curingZoneServiceProvider.getIfAvailable(() -> runtimeServiceBridge().curingZoneService());
    }

    private SpringZoneService springZoneService() {
        if (springZoneServiceProvider == null) {
            return runtimeServiceBridge().springZoneService();
        }
        return springZoneServiceProvider.getIfAvailable(() -> runtimeServiceBridge().springZoneService());
    }

    private BoostEventService boostEventService() {
        if (boostEventServiceProvider == null) {
            return runtimeServiceBridge().boostEventService();
        }
        return boostEventServiceProvider.getIfAvailable(() -> runtimeServiceBridge().boostEventService());
    }

    private TaskManagerFromDB taskManagerFromDB() {
        if (taskManagerFromDBProvider == null) {
            return runtimeServiceBridge().taskManagerFromDB();
        }
        return taskManagerFromDBProvider.getIfAvailable(() -> runtimeServiceBridge().taskManagerFromDB());
    }

    private LimitedItemTradeService limitedItemTradeService() {
        if (limitedItemTradeServiceProvider == null) {
            return runtimeServiceBridge().limitedItemTradeService();
        }
        return limitedItemTradeServiceProvider.getIfAvailable(() -> runtimeServiceBridge().limitedItemTradeService());
    }

    private WebshopService webshopService() {
        if (webshopServiceProvider == null) {
            return runtimeServiceBridge().webshopService();
        }
        return webshopServiceProvider.getIfAvailable(() -> runtimeServiceBridge().webshopService());
    }

    private SurveyService surveyService() {
        if (surveyServiceProvider == null) {
            return runtimeServiceBridge().surveyService();
        }
        return surveyServiceProvider.getIfAvailable(() -> runtimeServiceBridge().surveyService());
    }

    private FindGroupService findGroupService() {
        if (findGroupServiceProvider == null) {
            return runtimeServiceBridge().findGroupService();
        }
        return findGroupServiceProvider.getIfAvailable(() -> runtimeServiceBridge().findGroupService());
    }

    private InGameShopEn inGameShopEn() {
        if (inGameShopEnProvider == null) {
            return runtimeServiceBridge().inGameShopEn();
        }
        return inGameShopEnProvider.getIfAvailable(() -> runtimeServiceBridge().inGameShopEn());
    }

    private GameRuntimeServiceBridge runtimeServiceBridge() {
        if (runtimeServiceBridgeProvider == null) {
            return new GameRuntimeServiceBridge();
        }
        return runtimeServiceBridgeProvider.getIfAvailable(GameRuntimeServiceBridge::new);
    }
}
