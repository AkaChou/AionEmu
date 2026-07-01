package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.eventEngine.EventScheduler;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameEventServices implements DisposableBean {

    private static volatile ObjectProvider<EventService> eventServiceProvider;
    private static volatile ObjectProvider<PlayerEventService> playerEventServiceProvider;
    private static volatile ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider;
    private static volatile ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider;
    private static volatile ObjectProvider<PacketBroadcaster> packetBroadcasterProvider;
    private static volatile ObjectProvider<EventScheduler> eventSchedulerProvider;

    public GameEventServices(ObjectProvider<EventService> eventServiceProvider,
            ObjectProvider<PlayerEventService> playerEventServiceProvider,
            ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider,
            ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider,
            ObjectProvider<PacketBroadcaster> packetBroadcasterProvider,
            ObjectProvider<EventScheduler> eventSchedulerProvider) {
        GameEventServices.eventServiceProvider = eventServiceProvider;
        GameEventServices.playerEventServiceProvider = playerEventServiceProvider;
        GameEventServices.crazyDaevaServiceProvider = crazyDaevaServiceProvider;
        GameEventServices.abyssRankUpdateServiceProvider = abyssRankUpdateServiceProvider;
        GameEventServices.packetBroadcasterProvider = packetBroadcasterProvider;
        GameEventServices.eventSchedulerProvider = eventSchedulerProvider;
        EventService.setInstanceProvider(eventServiceProvider);
        PlayerEventService.setInstanceProvider(playerEventServiceProvider);
        CrazyDaevaService.setInstanceProvider(crazyDaevaServiceProvider);
        AbyssRankUpdateService.setInstanceProvider(abyssRankUpdateServiceProvider);
        PacketBroadcaster.setInstanceProvider(packetBroadcasterProvider);
        EventScheduler.setInstanceProvider(eventSchedulerProvider);
    }

    public static EventService eventService() {
        ObjectProvider<EventService> provider = eventServiceProvider;
        if (provider == null) {
            return EventService.getInstance();
        }
        return provider.getIfAvailable(EventService::getInstance);
    }

    public static PlayerEventService playerEventService() {
        ObjectProvider<PlayerEventService> provider = playerEventServiceProvider;
        if (provider == null) {
            return GameEventRuntimeFallbacks.playerEventService();
        }
        return provider.getIfAvailable(GameEventRuntimeFallbacks::playerEventService);
    }

    public static CrazyDaevaService crazyDaevaService() {
        ObjectProvider<CrazyDaevaService> provider = crazyDaevaServiceProvider;
        if (provider == null) {
            return GameEventRuntimeFallbacks.crazyDaevaService();
        }
        return provider.getIfAvailable(GameEventRuntimeFallbacks::crazyDaevaService);
    }

    public static AbyssRankUpdateService abyssRankUpdateService() {
        ObjectProvider<AbyssRankUpdateService> provider = abyssRankUpdateServiceProvider;
        if (provider == null) {
            return GameEventRuntimeFallbacks.abyssRankUpdateService();
        }
        return provider.getIfAvailable(GameEventRuntimeFallbacks::abyssRankUpdateService);
    }

    public static PacketBroadcaster packetBroadcaster() {
        ObjectProvider<PacketBroadcaster> provider = packetBroadcasterProvider;
        if (provider == null) {
            return GameEventRuntimeFallbacks.packetBroadcaster();
        }
        return provider.getIfAvailable(GameEventRuntimeFallbacks::packetBroadcaster);
    }

    public static EventScheduler eventScheduler() {
        ObjectProvider<EventScheduler> provider = eventSchedulerProvider;
        if (provider == null) {
            return GameEventRuntimeFallbacks.eventScheduler();
        }
        return provider.getIfAvailable(GameEventRuntimeFallbacks::eventScheduler);
    }

    @Override
    public void destroy() {
        eventServiceProvider = null;
        playerEventServiceProvider = null;
        crazyDaevaServiceProvider = null;
        abyssRankUpdateServiceProvider = null;
        packetBroadcasterProvider = null;
        eventSchedulerProvider = null;
        EventService.setInstanceProvider(null);
        PlayerEventService.setInstanceProvider(null);
        CrazyDaevaService.setInstanceProvider(null);
        AbyssRankUpdateService.setInstanceProvider(null);
        PacketBroadcaster.setInstanceProvider(null);
        EventScheduler.setInstanceProvider(null);
    }
}
