package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.configs.main.RankingConfig;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.spawnengine.TemporarySpawnEngine;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameEventRuntimeBridge {

    private ObjectProvider<EventService> eventServiceProvider;
    private ObjectProvider<PlayerEventService> playerEventServiceProvider;
    private ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider;
    private ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider;
    private ObjectProvider<PacketBroadcaster> packetBroadcasterProvider;

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

    public void printEventsSection() {
        Util.printSection(" *** Events *** ");
    }

    public boolean isEventServiceEnabled() {
        return EventsConfig.ENABLE_EVENT_SERVICE;
    }

    public boolean isPlayerEventEnabled() {
        return EventsConfig.EVENT_ENABLED;
    }

    public boolean isCrazyDaevaEnabled() {
        return EventsConfig.ENABLE_CRAZY;
    }

    public boolean isTopRankingUpdateEnabled() {
        return RankingConfig.TOP_RANKING_UPDATE_SETTING;
    }

    public EventService eventService() {
        if (eventServiceProvider == null) {
            return GameEventRuntimeFallbacks.eventService();
        }
        return eventServiceProvider.getIfAvailable(GameEventRuntimeFallbacks::eventService);
    }

    public PlayerEventService playerEventService() {
        if (playerEventServiceProvider == null) {
            return GameEventRuntimeFallbacks.playerEventService();
        }
        return playerEventServiceProvider.getIfAvailable(GameEventRuntimeFallbacks::playerEventService);
    }

    public CrazyDaevaService crazyDaevaService() {
        if (crazyDaevaServiceProvider == null) {
            return GameEventRuntimeFallbacks.crazyDaevaService();
        }
        return crazyDaevaServiceProvider.getIfAvailable(GameEventRuntimeFallbacks::crazyDaevaService);
    }

    public AbyssRankUpdateService abyssRankUpdateService() {
        if (abyssRankUpdateServiceProvider == null) {
            return GameEventRuntimeFallbacks.abyssRankUpdateService();
        }
        return abyssRankUpdateServiceProvider.getIfAvailable(GameEventRuntimeFallbacks::abyssRankUpdateService);
    }

    public PacketBroadcaster packetBroadcaster() {
        if (packetBroadcasterProvider == null) {
            return GameEventRuntimeFallbacks.packetBroadcaster();
        }
        return packetBroadcasterProvider.getIfAvailable(GameEventRuntimeFallbacks::packetBroadcaster);
    }

    public void spawnTemporarySpawns() {
        TemporarySpawnEngine.spawnAll();
    }
}
