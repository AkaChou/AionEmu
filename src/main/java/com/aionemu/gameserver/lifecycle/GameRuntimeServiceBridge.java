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

public class GameRuntimeServiceBridge {

    public void printServicesSection() {
        Util.printSection(" *** Services *** ");
    }

    public PeriodicSaveService periodicSaveService() {
        return PeriodicSaveService.getInstance();
    }

    public AdminService adminService() {
        return AdminService.getInstance();
    }

    public PlayerTransferService playerTransferService() {
        return PlayerTransferService.getInstance();
    }

    public TerritoryService territoryService() {
        return TerritoryService.getInstance();
    }

    public GameTimeService gameTimeService() {
        return GameTimeService.getInstance();
    }

    public AnnouncementService announcementService() {
        return AnnouncementService.getInstance();
    }

    public DebugService debugService() {
        return DebugService.getInstance();
    }

    public WeatherService weatherService() {
        return WeatherService.getInstance();
    }

    public BrokerService brokerService() {
        return BrokerService.getInstance();
    }

    public Influence influence() {
        return Influence.getInstance();
    }

    public ExchangeService exchangeService() {
        return ExchangeService.getInstance();
    }

    public PetitionService petitionService() {
        return PetitionService.getInstance();
    }

    public void loadInstances() {
        InstanceService.load();
    }

    public FlyRingService flyRingService() {
        return FlyRingService.getInstance();
    }

    public CuringZoneService curingZoneService() {
        return CuringZoneService.getInstance();
    }

    public SpringZoneService springZoneService() {
        return SpringZoneService.getInstance();
    }

    public BoostEventService boostEventService() {
        return BoostEventService.getInstance();
    }

    public TaskManagerFromDB taskManagerFromDB() {
        return TaskManagerFromDB.getInstance();
    }

    public LimitedItemTradeService limitedItemTradeService() {
        return LimitedItemTradeService.getInstance();
    }

    public void startGameTimeClock() {
        GameTimeManager.startClock();
    }
}
