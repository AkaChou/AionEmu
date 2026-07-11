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

/**
 * 运行时服务集合静态门面：将 Spring {@link ObjectProvider} 绑定到各服务的 instance provider，
 * 并在销毁时清空，供非 Spring 路径回退到传统单例。
 * Static facade for the runtime services set: binds Spring {@link ObjectProvider}s to each
 * service instance provider and clears them on destroy, with classic-singleton fallback.
 */
@Component
public final class GameRuntimeServices implements DisposableBean {

    /**
     * 周期存档服务提供者。
     * Periodic-save service provider.
     */
    private static volatile ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider;

    /**
     * 管理服务提供者。
     * Admin service provider.
     */
    private static volatile ObjectProvider<AdminService> adminServiceProvider;

    /**
     * 角色转移服务提供者。
     * Player-transfer service provider.
     */
    private static volatile ObjectProvider<PlayerTransferService> playerTransferServiceProvider;

    /**
     * 领地服务提供者。
     * Territory service provider.
     */
    private static volatile ObjectProvider<TerritoryService> territoryServiceProvider;

    /**
     * 公告服务提供者。
     * Announcement service provider.
     */
    private static volatile ObjectProvider<AnnouncementService> announcementServiceProvider;

    /**
     * 天气服务提供者。
     * Weather service provider.
     */
    private static volatile ObjectProvider<WeatherService> weatherServiceProvider;

    /**
     * 寄售服务提供者。
     * Broker service provider.
     */
    private static volatile ObjectProvider<BrokerService> brokerServiceProvider;

    /**
     * 影响力提供者。
     * Influence provider.
     */
    private static volatile ObjectProvider<Influence> influenceProvider;

    /**
     * 交易服务提供者。
     * Exchange service provider.
     */
    private static volatile ObjectProvider<ExchangeService> exchangeServiceProvider;

    /**
     * 请愿服务提供者。
     * Petition service provider.
     */
    private static volatile ObjectProvider<PetitionService> petitionServiceProvider;

    /**
     * 增益活动服务提供者。
     * Boost-event service provider.
     */
    private static volatile ObjectProvider<BoostEventService> boostEventServiceProvider;

    /**
     * 限购交易服务提供者。
     * Limited-item trade service provider.
     */
    private static volatile ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider;

    /**
     * 问卷服务提供者。
     * Survey service provider.
     */
    private static volatile ObjectProvider<SurveyService> surveyServiceProvider;

    /**
     * 寻找队伍服务提供者。
     * Find-group service provider.
     */
    private static volatile ObjectProvider<FindGroupService> findGroupServiceProvider;

    /**
     * 商城服务提供者。
     * In-game shop provider.
     */
    private static volatile ObjectProvider<InGameShopEn> inGameShopEnProvider;

    /**
     * GM 服务提供者。
     * GM service provider.
     */
    private static volatile ObjectProvider<GMService> gmServiceProvider;

    /**
     * 构造并绑定各运行时服务的 instance provider。
     * Construct and bind instance providers for all runtime services.
     *
     * Periodic save
     * Admin
     * Player transfer
     * Territory
     * Game time
     * Announcement
     * Debug
     * Weather
     * Broker
     * Influence
     * Exchange
     * Petition
     * Fly ring
     * Curing zone
     * Spring zone
     * Boost event
     * DB task manager
     * Limited-item trade
     * Webshop
     * Survey
     * Find group
     * In-game shop
     * GM
     */
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
        GameRuntimeServices.periodicSaveServiceProvider = periodicSaveServiceProvider;
        GameRuntimeServices.adminServiceProvider = adminServiceProvider;
        GameRuntimeServices.playerTransferServiceProvider = playerTransferServiceProvider;
        GameRuntimeServices.territoryServiceProvider = territoryServiceProvider;
        GameRuntimeServices.announcementServiceProvider = announcementServiceProvider;
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

    /**
     * 解析管理服务实例。
     * Resolve the admin service instance.
     *
     * Admin service
     */
    public static AdminService adminService() {
        return getIfAvailable(adminServiceProvider, AdminService::getInstance);
    }

    /**
     * 解析周期存档服务实例。
     * Resolve the periodic-save service instance.
     *
     * @return 周期存档服务 / Periodic-save service
     */
    public static PeriodicSaveService periodicSaveService() {
        return getIfAvailable(periodicSaveServiceProvider, PeriodicSaveService::getInstance);
    }

    /**
     * 解析公告服务实例。
     * Resolve the announcement service instance.
     *
     * Announcement service
     */
    public static AnnouncementService announcementService() {
        return getIfAvailable(announcementServiceProvider, AnnouncementService::getInstance);
    }

    /**
     * 解析角色转移服务实例。
     * Resolve the player-transfer service instance.
     *
     * @return 角色转移服务 / Player-transfer service
     */
    public static PlayerTransferService playerTransferService() {
        return getIfAvailable(playerTransferServiceProvider, PlayerTransferService::getInstance);
    }

    /**
     * 解析领地服务实例。
     * Resolve the territory service instance.
     *
     * Territory service
     */
    public static TerritoryService territoryService() {
        return getIfAvailable(territoryServiceProvider, TerritoryService::getInstance);
    }

    /**
     * 解析天气服务实例。
     * Resolve the weather service instance.
     *
     * Weather service
     */
    public static WeatherService weatherService() {
        return getIfAvailable(weatherServiceProvider, WeatherService::getInstance);
    }

    /**
     * 解析寄售服务实例。
     * Resolve the broker service instance.
     *
     * Broker service
     */
    public static BrokerService brokerService() {
        return getIfAvailable(brokerServiceProvider, BrokerService::getInstance);
    }

    /**
     * 解析影响力实例。
     * Resolve the influence instance.
     *
     * Influence
     */
    public static Influence influence() {
        return getIfAvailable(influenceProvider, Influence::getInstance);
    }

    /**
     * 解析交易服务实例。
     * Resolve the exchange service instance.
     *
     * Exchange service
     */
    public static ExchangeService exchangeService() {
        return getIfAvailable(exchangeServiceProvider, ExchangeService::getInstance);
    }

    /**
     * 解析请愿服务实例。
     * Resolve the petition service instance.
     *
     * Petition service
     */
    public static PetitionService petitionService() {
        return getIfAvailable(petitionServiceProvider, PetitionService::getInstance);
    }

    /**
     * 解析增益活动服务实例。
     * Resolve the boost-event service instance.
     *
     * @return 增益活动服务 / Boost-event service
     */
    public static BoostEventService boostEventService() {
        return getIfAvailable(boostEventServiceProvider, BoostEventService::getInstance);
    }

    /**
     * 解析限购交易服务实例。
     * Resolve the limited-item trade service instance.
     *
     * @return 限购交易服务 / Limited-item trade service
     */
    public static LimitedItemTradeService limitedItemTradeService() {
        return getIfAvailable(limitedItemTradeServiceProvider, LimitedItemTradeService::getInstance);
    }

    /**
     * 解析问卷服务实例。
     * Resolve the survey service instance.
     *
     * Survey service
     */
    public static SurveyService surveyService() {
        return getIfAvailable(surveyServiceProvider, SurveyService::getInstance);
    }

    /**
     * 解析寻找队伍服务实例。
     * Resolve the find-group service instance.
     *
     * @return 寻找队伍服务 / Find-group service
     */
    public static FindGroupService findGroupService() {
        return getIfAvailable(findGroupServiceProvider, FindGroupService::getInstance);
    }

    /**
     * 解析商城服务实例。
     * Resolve the in-game shop instance.
     *
     * In-game shop
     */
    public static InGameShopEn inGameShopEn() {
        return getIfAvailable(inGameShopEnProvider, InGameShopEn::getInstance);
    }

    /**
     * 解析 GM 服务实例。
     * Resolve the GM service instance.
     *
     * GM service
     */
    public static GMService gmService() {
        return getIfAvailable(gmServiceProvider, GMService::getInstance);
    }

    /**
     * 优先从提供者取 bean，否则执行回退供应器。
     * Prefer the provider bean, otherwise run the fallback supplier.
     *
     * Spring provider
     * @param fallback 回退供应器 / Fallback supplier
     * @param <T> 服务类型 / Service type
     * Service instance
     */
    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }

    /**
     * Spring 销毁时清空静态提供者与各服务 instance provider。
     * Clear static providers and each service instance provider on Spring destroy.
     */
    @Override
    public void destroy() {
        periodicSaveServiceProvider = null;
        adminServiceProvider = null;
        PeriodicSaveService.setInstanceProvider(null);
        AdminService.setInstanceProvider(null);
        playerTransferServiceProvider = null;
        PlayerTransferService.setInstanceProvider(null);
        territoryServiceProvider = null;
        TerritoryService.setInstanceProvider(null);
        GameTimeService.setInstanceProvider(null);
        announcementServiceProvider = null;
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
