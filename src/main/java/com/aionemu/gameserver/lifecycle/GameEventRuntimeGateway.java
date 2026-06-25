package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameEventRuntimeGateway {

    private ObjectProvider<EventService> eventServiceProvider;
    private ObjectProvider<PlayerEventService> playerEventServiceProvider;
    private ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider;
    private ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider;
    private ObjectProvider<PacketBroadcaster> packetBroadcasterProvider;
    private ObjectProvider<GameEventRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setEventServiceProvider(ObjectProvider<EventService> eventServiceProvider) {
        this.eventServiceProvider = eventServiceProvider;
    }

    @Autowired(required = false)
    void setPlayerEventServiceProvider(ObjectProvider<PlayerEventService> playerEventServiceProvider) {
        this.playerEventServiceProvider = playerEventServiceProvider;
    }

    @Autowired(required = false)
    void setCrazyDaevaServiceProvider(ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider) {
        this.crazyDaevaServiceProvider = crazyDaevaServiceProvider;
    }

    @Autowired(required = false)
    void setAbyssRankUpdateServiceProvider(ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider) {
        this.abyssRankUpdateServiceProvider = abyssRankUpdateServiceProvider;
    }

    @Autowired(required = false)
    void setPacketBroadcasterProvider(ObjectProvider<PacketBroadcaster> packetBroadcasterProvider) {
        this.packetBroadcasterProvider = packetBroadcasterProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameEventRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void start() {
        GameEventRuntimeBridge runtimeBridge = runtimeBridge();
        runtimeBridge.printEventsSection();
        if (runtimeBridge.isEventServiceEnabled()) {
            eventService().start();
        }
        if (runtimeBridge.isPlayerEventEnabled()) {
            playerEventService();
        }
        if (runtimeBridge.isCrazyDaevaEnabled()) {
            crazyDaevaService().startTimer();
        }
        AbyssRankUpdateService abyssRankUpdateService = abyssRankUpdateService();
        if (runtimeBridge.isTopRankingUpdateEnabled()) {
            abyssRankUpdateService.scheduleUpdateHour();
        } else {
            abyssRankUpdateService.scheduleUpdateMinute();
        }
        abyssRankUpdateService.initRewardWeeklyManager();
        packetBroadcaster();
        runtimeBridge.spawnTemporarySpawns();
    }

    private EventService eventService() {
        if (eventServiceProvider == null) {
            return runtimeBridge().eventService();
        }
        return eventServiceProvider.getIfAvailable(() -> runtimeBridge().eventService());
    }

    private PlayerEventService playerEventService() {
        if (playerEventServiceProvider == null) {
            return runtimeBridge().playerEventService();
        }
        return playerEventServiceProvider.getIfAvailable(() -> runtimeBridge().playerEventService());
    }

    private CrazyDaevaService crazyDaevaService() {
        if (crazyDaevaServiceProvider == null) {
            return runtimeBridge().crazyDaevaService();
        }
        return crazyDaevaServiceProvider.getIfAvailable(() -> runtimeBridge().crazyDaevaService());
    }

    private AbyssRankUpdateService abyssRankUpdateService() {
        if (abyssRankUpdateServiceProvider == null) {
            return runtimeBridge().abyssRankUpdateService();
        }
        return abyssRankUpdateServiceProvider.getIfAvailable(() -> runtimeBridge().abyssRankUpdateService());
    }

    private PacketBroadcaster packetBroadcaster() {
        if (packetBroadcasterProvider == null) {
            return runtimeBridge().packetBroadcaster();
        }
        return packetBroadcasterProvider.getIfAvailable(() -> runtimeBridge().packetBroadcaster());
    }

    private GameEventRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameEventRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameEventRuntimeBridge::new);
    }
}
