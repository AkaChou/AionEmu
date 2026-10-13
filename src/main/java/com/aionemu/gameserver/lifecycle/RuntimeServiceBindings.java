package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
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
 * 运行时服务静态绑定：把 Spring ObjectProvider 喂入运行时域服务自持的 fail-fast 单例通道。
 * Runtime static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Runtime services.
 * <p>本组件取代已退役的运行时静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Runtime static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class RuntimeServiceBindings implements DisposableBean {

    /**
     * 构造并注册各运行时服务的实例提供者。
     * Construct and register instance providers for Runtime services.
     * @param periodicSaveServiceProvider PeriodicSaveService 提供者 / PeriodicSaveService provider
     * @param adminServiceProvider AdminService 提供者 / AdminService provider
     * @param playerTransferServiceProvider PlayerTransferService 提供者 / PlayerTransferService provider
     * @param territoryServiceProvider TerritoryService 提供者 / TerritoryService provider
     * @param gameTimeServiceProvider GameTimeService 提供者 / GameTimeService provider
     * @param announcementServiceProvider AnnouncementService 提供者 / AnnouncementService provider
     * @param debugServiceProvider DebugService 提供者 / DebugService provider
     * @param weatherServiceProvider WeatherService 提供者 / WeatherService provider
     * @param brokerServiceProvider BrokerService 提供者 / BrokerService provider
     * @param influenceProvider Influence 提供者 / Influence provider
     * @param exchangeServiceProvider ExchangeService 提供者 / ExchangeService provider
     * @param petitionServiceProvider PetitionService 提供者 / PetitionService provider
     * @param flyRingServiceProvider FlyRingService 提供者 / FlyRingService provider
     * @param curingZoneServiceProvider CuringZoneService 提供者 / CuringZoneService provider
     * @param springZoneServiceProvider SpringZoneService 提供者 / SpringZoneService provider
     * @param boostEventServiceProvider BoostEventService 提供者 / BoostEventService provider
     * @param taskManagerFromDBProvider TaskManagerFromDB 提供者 / TaskManagerFromDB provider
     * @param limitedItemTradeServiceProvider LimitedItemTradeService 提供者 / LimitedItemTradeService provider
     * @param webshopServiceProvider WebshopService 提供者 / WebshopService provider
     * @param surveyServiceProvider SurveyService 提供者 / SurveyService provider
     * @param findGroupServiceProvider FindGroupService 提供者 / FindGroupService provider
     * @param inGameShopEnProvider InGameShopEn 提供者 / InGameShopEn provider
     * @param gmServiceProvider GMService 提供者 / GMService provider
     */
    public RuntimeServiceBindings(ObjectProvider<PeriodicSaveService> periodicSaveServiceProvider,
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
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        PeriodicSaveService.setInstanceProvider(null);
        AdminService.setInstanceProvider(null);
        PlayerTransferService.setInstanceProvider(null);
        TerritoryService.setInstanceProvider(null);
        GameTimeService.setInstanceProvider(null);
        AnnouncementService.setInstanceProvider(null);
        DebugService.setInstanceProvider(null);
        WeatherService.setInstanceProvider(null);
        BrokerService.setInstanceProvider(null);
        Influence.setInstanceProvider(null);
        ExchangeService.setInstanceProvider(null);
        PetitionService.setInstanceProvider(null);
        FlyRingService.setInstanceProvider(null);
        CuringZoneService.setInstanceProvider(null);
        SpringZoneService.setInstanceProvider(null);
        BoostEventService.setInstanceProvider(null);
        TaskManagerFromDB.setInstanceProvider(null);
        LimitedItemTradeService.setInstanceProvider(null);
        WebshopService.setInstanceProvider(null);
        SurveyService.setInstanceProvider(null);
        FindGroupService.setInstanceProvider(null);
        InGameShopEn.setInstanceProvider(null);
        GMService.setInstanceProvider(null);
    }
}
