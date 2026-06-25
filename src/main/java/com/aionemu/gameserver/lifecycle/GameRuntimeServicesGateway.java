package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.services.AdminService;
import com.aionemu.gameserver.services.AnnouncementService;
import com.aionemu.gameserver.services.BrokerService;
import com.aionemu.gameserver.services.CuringZoneService;
import com.aionemu.gameserver.services.DebugService;
import com.aionemu.gameserver.services.ExchangeService;
import com.aionemu.gameserver.services.FlyRingService;
import com.aionemu.gameserver.services.GameTimeService;
import com.aionemu.gameserver.services.LimitedItemTradeService;
import com.aionemu.gameserver.services.PeriodicSaveService;
import com.aionemu.gameserver.services.PetitionService;
import com.aionemu.gameserver.services.SpringZoneService;
import com.aionemu.gameserver.services.WeatherService;
import com.aionemu.gameserver.services.events.BoostEventService;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.utils.Util;
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
    private ObjectProvider<Influence> influenceProvider;
    private ObjectProvider<ExchangeService> exchangeServiceProvider;
    private ObjectProvider<PetitionService> petitionServiceProvider;
    private ObjectProvider<FlyRingService> flyRingServiceProvider;
    private ObjectProvider<CuringZoneService> curingZoneServiceProvider;
    private ObjectProvider<SpringZoneService> springZoneServiceProvider;
    private ObjectProvider<BoostEventService> boostEventServiceProvider;
    private ObjectProvider<TaskManagerFromDB> taskManagerFromDBProvider;
    private ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider;
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
    void setRuntimeServiceBridgeProvider(ObjectProvider<GameRuntimeServiceBridge> runtimeServiceBridgeProvider) {
        this.runtimeServiceBridgeProvider = runtimeServiceBridgeProvider;
    }

    public void start() {
        Util.printSection(" *** Services *** ");
        periodicSaveService();
        adminService();
        playerTransferService();
        territoryService().initTerritory();
        gameTimeService();
        announcementService();
        debugService();
        weatherService();
        brokerService();
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
        runtimeServiceBridge().startGameTimeClock();
    }

    private PeriodicSaveService periodicSaveService() {
        if (periodicSaveServiceProvider == null) {
            return PeriodicSaveService.getInstance();
        }
        return periodicSaveServiceProvider.getIfAvailable(PeriodicSaveService::getInstance);
    }

    private AdminService adminService() {
        if (adminServiceProvider == null) {
            return AdminService.getInstance();
        }
        return adminServiceProvider.getIfAvailable(AdminService::getInstance);
    }

    private PlayerTransferService playerTransferService() {
        if (playerTransferServiceProvider == null) {
            return PlayerTransferService.getInstance();
        }
        return playerTransferServiceProvider.getIfAvailable(PlayerTransferService::getInstance);
    }

    private TerritoryService territoryService() {
        if (territoryServiceProvider == null) {
            return TerritoryService.getInstance();
        }
        return territoryServiceProvider.getIfAvailable(TerritoryService::getInstance);
    }

    private GameTimeService gameTimeService() {
        if (gameTimeServiceProvider == null) {
            return GameTimeService.getInstance();
        }
        return gameTimeServiceProvider.getIfAvailable(GameTimeService::getInstance);
    }

    private AnnouncementService announcementService() {
        if (announcementServiceProvider == null) {
            return AnnouncementService.getInstance();
        }
        return announcementServiceProvider.getIfAvailable(AnnouncementService::getInstance);
    }

    private DebugService debugService() {
        if (debugServiceProvider == null) {
            return DebugService.getInstance();
        }
        return debugServiceProvider.getIfAvailable(DebugService::getInstance);
    }

    private WeatherService weatherService() {
        if (weatherServiceProvider == null) {
            return WeatherService.getInstance();
        }
        return weatherServiceProvider.getIfAvailable(WeatherService::getInstance);
    }

    private BrokerService brokerService() {
        if (brokerServiceProvider == null) {
            return BrokerService.getInstance();
        }
        return brokerServiceProvider.getIfAvailable(BrokerService::getInstance);
    }

    private Influence influence() {
        if (influenceProvider == null) {
            return Influence.getInstance();
        }
        return influenceProvider.getIfAvailable(Influence::getInstance);
    }

    private ExchangeService exchangeService() {
        if (exchangeServiceProvider == null) {
            return ExchangeService.getInstance();
        }
        return exchangeServiceProvider.getIfAvailable(ExchangeService::getInstance);
    }

    private PetitionService petitionService() {
        if (petitionServiceProvider == null) {
            return PetitionService.getInstance();
        }
        return petitionServiceProvider.getIfAvailable(PetitionService::getInstance);
    }

    private FlyRingService flyRingService() {
        if (flyRingServiceProvider == null) {
            return FlyRingService.getInstance();
        }
        return flyRingServiceProvider.getIfAvailable(FlyRingService::getInstance);
    }

    private CuringZoneService curingZoneService() {
        if (curingZoneServiceProvider == null) {
            return CuringZoneService.getInstance();
        }
        return curingZoneServiceProvider.getIfAvailable(CuringZoneService::getInstance);
    }

    private SpringZoneService springZoneService() {
        if (springZoneServiceProvider == null) {
            return SpringZoneService.getInstance();
        }
        return springZoneServiceProvider.getIfAvailable(SpringZoneService::getInstance);
    }

    private BoostEventService boostEventService() {
        if (boostEventServiceProvider == null) {
            return BoostEventService.getInstance();
        }
        return boostEventServiceProvider.getIfAvailable(BoostEventService::getInstance);
    }

    private TaskManagerFromDB taskManagerFromDB() {
        if (taskManagerFromDBProvider == null) {
            return TaskManagerFromDB.getInstance();
        }
        return taskManagerFromDBProvider.getIfAvailable(TaskManagerFromDB::getInstance);
    }

    private LimitedItemTradeService limitedItemTradeService() {
        if (limitedItemTradeServiceProvider == null) {
            return LimitedItemTradeService.getInstance();
        }
        return limitedItemTradeServiceProvider.getIfAvailable(LimitedItemTradeService::getInstance);
    }

    private GameRuntimeServiceBridge runtimeServiceBridge() {
        if (runtimeServiceBridgeProvider == null) {
            return new GameRuntimeServiceBridge();
        }
        return runtimeServiceBridgeProvider.getIfAvailable(GameRuntimeServiceBridge::new);
    }
}
