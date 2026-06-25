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
import org.springframework.stereotype.Component;

@Component
public class GameEventRuntimeGateway {

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

    public void start() {
        Util.printSection(" *** Events *** ");
        if (EventsConfig.ENABLE_EVENT_SERVICE) {
            eventService().start();
        }
        if (EventsConfig.EVENT_ENABLED) {
            playerEventService();
        }
        if (EventsConfig.ENABLE_CRAZY) {
            crazyDaevaService().startTimer();
        }
        AbyssRankUpdateService abyssRankUpdateService = abyssRankUpdateService();
        if (RankingConfig.TOP_RANKING_UPDATE_SETTING) {
            abyssRankUpdateService.scheduleUpdateHour();
        } else {
            abyssRankUpdateService.scheduleUpdateMinute();
        }
        abyssRankUpdateService.initRewardWeeklyManager();
        packetBroadcaster();
        TemporarySpawnEngine.spawnAll();
    }

    private EventService eventService() {
        if (eventServiceProvider == null) {
            return EventService.getInstance();
        }
        return eventServiceProvider.getIfAvailable(EventService::getInstance);
    }

    private PlayerEventService playerEventService() {
        if (playerEventServiceProvider == null) {
            return PlayerEventService.getInstance();
        }
        return playerEventServiceProvider.getIfAvailable(PlayerEventService::getInstance);
    }

    private CrazyDaevaService crazyDaevaService() {
        if (crazyDaevaServiceProvider == null) {
            return CrazyDaevaService.getInstance();
        }
        return crazyDaevaServiceProvider.getIfAvailable(CrazyDaevaService::getInstance);
    }

    private AbyssRankUpdateService abyssRankUpdateService() {
        if (abyssRankUpdateServiceProvider == null) {
            return AbyssRankUpdateService.getInstance();
        }
        return abyssRankUpdateServiceProvider.getIfAvailable(AbyssRankUpdateService::getInstance);
    }

    private PacketBroadcaster packetBroadcaster() {
        if (packetBroadcasterProvider == null) {
            return PacketBroadcaster.getInstance();
        }
        return packetBroadcasterProvider.getIfAvailable(PacketBroadcaster::getInstance);
    }
}
