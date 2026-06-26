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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameRuntimeServices implements DisposableBean {

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
            ObjectProvider<LimitedItemTradeService> limitedItemTradeServiceProvider) {
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
    }

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
    }
}
