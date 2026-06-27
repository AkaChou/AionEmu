package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.eventEngine.EventScheduler;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;

final class GameEventRuntimeFallbacks {

    private GameEventRuntimeFallbacks() {
    }

    static EventService eventService() {
        return EventServiceFallback.INSTANCE;
    }

    static PlayerEventService playerEventService() {
        return PlayerEventServiceFallback.INSTANCE;
    }

    static CrazyDaevaService crazyDaevaService() {
        return CrazyDaevaServiceFallback.INSTANCE;
    }

    static AbyssRankUpdateService abyssRankUpdateService() {
        return AbyssRankUpdateServiceFallback.INSTANCE;
    }

    static PacketBroadcaster packetBroadcaster() {
        return PacketBroadcasterFallback.INSTANCE;
    }

    static EventScheduler eventScheduler() {
        return EventSchedulerFallback.INSTANCE;
    }

    private static final class EventServiceFallback {
        private static final EventService INSTANCE = EventService.getInstance();
    }

    private static final class PlayerEventServiceFallback {
        private static final PlayerEventService INSTANCE = PlayerEventService.getInstance();
    }

    private static final class CrazyDaevaServiceFallback {
        private static final CrazyDaevaService INSTANCE = CrazyDaevaService.getInstance();
    }

    private static final class AbyssRankUpdateServiceFallback {
        private static final AbyssRankUpdateService INSTANCE = AbyssRankUpdateService.getInstance();
    }

    private static final class PacketBroadcasterFallback {
        private static final PacketBroadcaster INSTANCE = PacketBroadcaster.getInstance();
    }

    private static final class EventSchedulerFallback {
        private static final EventScheduler INSTANCE = EventScheduler.getInstance();
    }
}
