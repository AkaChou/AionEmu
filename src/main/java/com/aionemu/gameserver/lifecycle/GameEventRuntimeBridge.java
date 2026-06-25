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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameEventRuntimeBridge {

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
        return EventService.getInstance();
    }

    public PlayerEventService playerEventService() {
        return PlayerEventService.getInstance();
    }

    public CrazyDaevaService crazyDaevaService() {
        return CrazyDaevaService.getInstance();
    }

    public AbyssRankUpdateService abyssRankUpdateService() {
        return AbyssRankUpdateService.getInstance();
    }

    public PacketBroadcaster packetBroadcaster() {
        return PacketBroadcaster.getInstance();
    }

    public void spawnTemporarySpawns() {
        TemporarySpawnEngine.spawnAll();
    }
}
