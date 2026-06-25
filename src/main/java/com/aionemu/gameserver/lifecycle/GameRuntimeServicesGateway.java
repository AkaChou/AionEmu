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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameRuntimeServicesGateway {

    private ObjectProvider<AdminService> adminServiceProvider;
    private ObjectProvider<PlayerTransferService> playerTransferServiceProvider;

    @Autowired(required = false)
    void setAdminServiceProvider(ObjectProvider<AdminService> adminServiceProvider) {
        this.adminServiceProvider = adminServiceProvider;
    }

    @Autowired(required = false)
    void setPlayerTransferServiceProvider(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        this.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    public void start() {
        Util.printSection(" *** Services *** ");
        PeriodicSaveService.getInstance();
        adminService();
        playerTransferService();
        TerritoryService.getInstance().initTerritory();
        GameTimeService.getInstance();
        AnnouncementService.getInstance();
        DebugService.getInstance();
        WeatherService.getInstance();
        BrokerService.getInstance();
        Influence.getInstance();
        ExchangeService.getInstance();
        PetitionService.getInstance();
        InstanceService.load();
        FlyRingService.getInstance();
        CuringZoneService.getInstance();
        SpringZoneService.getInstance();
        BoostEventService.getInstance().onStart();
        TaskManagerFromDB.getInstance();
        LimitedItemTradeService.getInstance().start();
        GameTimeManager.startClock();
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
}
