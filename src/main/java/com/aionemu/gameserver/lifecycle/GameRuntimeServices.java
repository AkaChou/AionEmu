package com.aionemu.gameserver.lifecycle;

import java.util.function.Supplier;

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
import com.aionemu.gameserver.utils.audit.GMService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameRuntimeServices implements DisposableBean {

    private static volatile ObjectProvider<AdminService> adminServiceProvider;
    private static volatile ObjectProvider<PlayerTransferService> playerTransferServiceProvider;
    private static volatile ObjectProvider<TerritoryService> territoryServiceProvider;
    private static volatile ObjectProvider<WeatherService> weatherServiceProvider;
    private static volatile ObjectProvider<BrokerService> brokerServiceProvider;
    private static volatile ObjectProvider<Influence> influenceProvider;
    private static volatile ObjectProvider<ExchangeService> exchangeServiceProvider;
    private static volatile ObjectProvider<PetitionService> petitionServiceProvider;
    private static volatile ObjectProvider<BoostEventService> boostEventServiceProvider;
    private static volatile ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider;
    private static volatile ObjectProvider<SurveyService> surveyServiceProvider;
    private static volatile ObjectProvider<FindGroupService> findGroupServiceProvider;
    private static volatile ObjectProvider<InGameShopEn> inGameShopEnProvider;
    private static volatile ObjectProvider<GMService> gmServiceProvider;

    public GameRuntimeServices(ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider,
            ObjectProvider<AdminService> adminServiceProvider,
            ObjectProvider<PlayerTransferService> playerTransferServiceProvider,
            ObjectProvider<TerritoryService> territoryServiceProvider,
            ObjectProvider<GameTimeService> gameTimeServiceProvider,
            ObjectProvider<AnnouncementService> announcementServiceProvider,
            ObjectProvider<DebugService> debugServiceProvider,
            ObjectProvider<WeatherService> weatherServiceProvider,
            ObjectProvider<BrokerService> brokerServiceProvider,
            ObjectProvider<Influence> influenceProvider,
            ObjectProvider<ExchangeService> exchangeServiceProvider,
            ObjectProvider<PetitionService> petitionServiceProvider,
            ObjectProvider<FlyRingService> flyRingServiceProvider,
            ObjectProvider<CuringZoneService> curingZoneServiceProvider,
            ObjectProvider<SpringZoneService> springZoneServiceProvider,
            ObjectProvider<BoostEventService> boostEventServiceProvider,
            ObjectProvider<TaskManagerFromDB> taskManagerFromDBProvider,
            ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider,
            ObjectProvider<WebshopService> webshopServiceProvider,
            ObjectProvider<SurveyService> surveyServiceProvider,
            ObjectProvider<FindGroupService> findGroupServiceProvider,
            ObjectProvider<InGameShopEn> inGameShopEnProvider,
            ObjectProvider<GMService> gmServiceProvider) {
        GameRuntimeServices.adminServiceProvider = adminServiceProvider;
        GameRuntimeServices.playerTransferServiceProvider = playerTransferServiceProvider;
        GameRuntimeServices.territoryServiceProvider = territoryServiceProvider;
        GameRuntimeServices.weatherServiceProvider = weatherServiceProvider;
        GameRuntimeServices.brokerServiceProvider = brokerServiceProvider;
        GameRuntimeServices.influenceProvider = influenceProvider;
        GameRuntimeServices.exchangeServiceProvider = exchangeServiceProvider;
        GameRuntimeServices.petitionServiceProvider = petitionServiceProvider;
        GameRuntimeServices.boostEventServiceProvider = boostEventServiceProvider;
        GameRuntimeServices.limitedItemTradeServiceProvider = limitedItemTradeServiceProvider;
        GameRuntimeServices.surveyServiceProvider = surveyServiceProvider;
        GameRuntimeServices.findGroupServiceProvider = findGroupServiceProvider;
        GameRuntimeServices.inGameShopEnProvider = inGameShopEnProvider;
        GameRuntimeServices.gmServiceProvider = gmServiceProvider;
        PeriodicSaveService.setInstanceProvider(periodicSaveServiceProvider);
        AdminService.setInstanceProvider(adminServiceProvider);
        PlayerTransferService.setInstanceProvider(playerTransferServiceProvider);
        TerritoryService.setInstanceProvider(territoryServiceProvider);
        GameTimeService.setInstanceProvider(gameTimeServiceProvider);
        AnnouncementService.setInstanceProvider(announcementServiceProvider);
        DebugService.setInstanceProvider(debugServiceProvider);
        WeatherService.setInstanceProvider(weatherServiceProvider);
        BrokerService.setInstanceProvider(brokerServiceProvider);
        Influence.setInstanceProvider(influenceProvider);
        ExchangeService.setInstanceProvider(exchangeServiceProvider);
        PetitionService.setInstanceProvider(petitionServiceProvider);
        FlyRingService.setInstanceProvider(flyRingServiceProvider);
        CuringZoneService.setInstanceProvider(curingZoneServiceProvider);
        SpringZoneService.setInstanceProvider(springZoneServiceProvider);
        BoostEventService.setInstanceProvider(boostEventServiceProvider);
        TaskManagerFromDB.setInstanceProvider(taskManagerFromDBProvider);
        LimitedItemTradeService.setInstanceProvider(limitedItemTradeServiceProvider);
        WebshopService.setInstanceProvider(webshopServiceProvider);
        SurveyService.setInstanceProvider(surveyServiceProvider);
        FindGroupService.setInstanceProvider(findGroupServiceProvider);
        InGameShopEn.setInstanceProvider(inGameShopEnProvider);
        GMService.setInstanceProvider(gmServiceProvider);
    }

    public static AdminService adminService() {
        return getIfAvailable(adminServiceProvider, AdminService::getInstance);
    }

    public static PlayerTransferService playerTransferService() {
        return getIfAvailable(playerTransferServiceProvider, PlayerTransferService::getInstance);
    }

    public static TerritoryService territoryService() {
        return getIfAvailable(territoryServiceProvider, TerritoryService::getInstance);
    }

    public static WeatherService weatherService() {
        return getIfAvailable(weatherServiceProvider, WeatherService::getInstance);
    }

    public static BrokerService brokerService() {
        return getIfAvailable(brokerServiceProvider, BrokerService::getInstance);
    }

    public static Influence influence() {
        return getIfAvailable(influenceProvider, Influence::getInstance);
    }

    public static ExchangeService exchangeService() {
        return getIfAvailable(exchangeServiceProvider, ExchangeService::getInstance);
    }

    public static PetitionService petitionService() {
        return getIfAvailable(petitionServiceProvider, PetitionService::getInstance);
    }

    public static BoostEventService boostEventService() {
        return getIfAvailable(boostEventServiceProvider, BoostEventService::getInstance);
    }

    public static LimitedItemTradeService limitedItemTradeService() {
        return getIfAvailable(limitedItemTradeServiceProvider, LimitedItemTradeService::getInstance);
    }

    public static SurveyService surveyService() {
        return getIfAvailable(surveyServiceProvider, SurveyService::getInstance);
    }

    public static FindGroupService findGroupService() {
        return getIfAvailable(findGroupServiceProvider, FindGroupService::getInstance);
    }

    public static InGameShopEn inGameShopEn() {
        return getIfAvailable(inGameShopEnProvider, InGameShopEn::getInstance);
    }

    public static GMService gmService() {
        return getIfAvailable(gmServiceProvider, GMService::getInstance);
    }

    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }

    @Override
    public void destroy() {
        adminServiceProvider = null;
        PeriodicSaveService.setInstanceProvider(null);
        AdminService.setInstanceProvider(null);
        playerTransferServiceProvider = null;
        PlayerTransferService.setInstanceProvider(null);
        territoryServiceProvider = null;
        TerritoryService.setInstanceProvider(null);
        GameTimeService.setInstanceProvider(null);
        AnnouncementService.setInstanceProvider(null);
        DebugService.setInstanceProvider(null);
        weatherServiceProvider = null;
        WeatherService.setInstanceProvider(null);
        brokerServiceProvider = null;
        BrokerService.setInstanceProvider(null);
        influenceProvider = null;
        Influence.setInstanceProvider(null);
        exchangeServiceProvider = null;
        ExchangeService.setInstanceProvider(null);
        petitionServiceProvider = null;
        PetitionService.setInstanceProvider(null);
        FlyRingService.setInstanceProvider(null);
        CuringZoneService.setInstanceProvider(null);
        SpringZoneService.setInstanceProvider(null);
        boostEventServiceProvider = null;
        BoostEventService.setInstanceProvider(null);
        TaskManagerFromDB.setInstanceProvider(null);
        limitedItemTradeServiceProvider = null;
        LimitedItemTradeService.setInstanceProvider(null);
        WebshopService.setInstanceProvider(null);
        surveyServiceProvider = null;
        SurveyService.setInstanceProvider(null);
        findGroupServiceProvider = null;
        inGameShopEnProvider = null;
        FindGroupService.setInstanceProvider(null);
        InGameShopEn.setInstanceProvider(null);
        gmServiceProvider = null;
        GMService.setInstanceProvider(null);
    }
}
