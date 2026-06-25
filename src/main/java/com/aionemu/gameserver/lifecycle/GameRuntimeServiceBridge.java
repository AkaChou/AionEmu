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
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class GameRuntimeServiceBridge {

    private ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider;
    private ObjectProvider<AdminService> adminServiceProvider;
    private ObjectProvider<PlayerTransferService> playerTransferServiceProvider;
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

    @Autowired(required = false)
    void setPeriodicSaveServiceProvider(ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider) {
        this.periodicSaveServiceProvider = periodicSaveServiceProvider;
    }

    @Autowired(required = false)
    void setAdminServiceProvider(ObjectProvider<AdminService> adminServiceProvider) {
        this.adminServiceProvider = adminServiceProvider;
    }

    @Autowired(required = false)
    void setPlayerTransferServiceProvider(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        this.playerTransferServiceProvider = playerTransferServiceProvider;
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

    public void printServicesSection() {
        Util.printSection(" *** Services *** ");
    }

    public PeriodicSaveService periodicSaveService() {
        return getIfAvailable(periodicSaveServiceProvider, PeriodicSaveService::getInstance);
    }

    public AdminService adminService() {
        return getIfAvailable(adminServiceProvider, AdminService::getInstance);
    }

    public PlayerTransferService playerTransferService() {
        return getIfAvailable(playerTransferServiceProvider, PlayerTransferService::getInstance);
    }

    public TerritoryService territoryService() {
        return getIfAvailable(territoryServiceProvider, TerritoryService::getInstance);
    }

    public GameTimeService gameTimeService() {
        return getIfAvailable(gameTimeServiceProvider, GameTimeService::getInstance);
    }

    public AnnouncementService announcementService() {
        return getIfAvailable(announcementServiceProvider, AnnouncementService::getInstance);
    }

    public DebugService debugService() {
        return getIfAvailable(debugServiceProvider, DebugService::getInstance);
    }

    public WeatherService weatherService() {
        return getIfAvailable(weatherServiceProvider, WeatherService::getInstance);
    }

    public BrokerService brokerService() {
        return getIfAvailable(brokerServiceProvider, BrokerService::getInstance);
    }

    public Influence influence() {
        return getIfAvailable(influenceProvider, Influence::getInstance);
    }

    public ExchangeService exchangeService() {
        return getIfAvailable(exchangeServiceProvider, ExchangeService::getInstance);
    }

    public PetitionService petitionService() {
        return getIfAvailable(petitionServiceProvider, PetitionService::getInstance);
    }

    public void loadInstances() {
        InstanceService.load();
    }

    public FlyRingService flyRingService() {
        return getIfAvailable(flyRingServiceProvider, FlyRingService::getInstance);
    }

    public CuringZoneService curingZoneService() {
        return getIfAvailable(curingZoneServiceProvider, CuringZoneService::getInstance);
    }

    public SpringZoneService springZoneService() {
        return getIfAvailable(springZoneServiceProvider, SpringZoneService::getInstance);
    }

    public BoostEventService boostEventService() {
        return getIfAvailable(boostEventServiceProvider, BoostEventService::getInstance);
    }

    public TaskManagerFromDB taskManagerFromDB() {
        return getIfAvailable(taskManagerFromDBProvider, TaskManagerFromDB::getInstance);
    }

    public LimitedItemTradeService limitedItemTradeService() {
        return getIfAvailable(limitedItemTradeServiceProvider, LimitedItemTradeService::getInstance);
    }

    public void startGameTimeClock() {
        GameTimeManager.startClock();
    }

    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }
}
