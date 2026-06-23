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
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameRuntimeServicesLifecycle {

    private final List<Runnable> initializers;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameRuntimeServicesLifecycle() {
        this(List.of(
            PeriodicSaveService::getInstance,
            AdminService::getInstance,
            PlayerTransferService::getInstance,
            () -> TerritoryService.getInstance().initTerritory(),
            GameTimeService::getInstance,
            AnnouncementService::getInstance,
            DebugService::getInstance,
            WeatherService::getInstance,
            BrokerService::getInstance,
            Influence::getInstance,
            ExchangeService::getInstance,
            PetitionService::getInstance,
            InstanceService::load,
            FlyRingService::getInstance,
            CuringZoneService::getInstance,
            SpringZoneService::getInstance,
            () -> BoostEventService.getInstance().onStart(),
            TaskManagerFromDB::getInstance,
            () -> LimitedItemTradeService.getInstance().start(),
            GameTimeManager::startClock
        ));
    }

    GameRuntimeServicesLifecycle(List<Runnable> initializers) {
        this.initializers = List.copyOf(initializers);
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            initializers.forEach(Runnable::run);
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
        }
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
