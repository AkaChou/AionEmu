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

/**
 * 事件运行时 Spring 服务门面 / 静态访问桥：注册事件运行时实例提供者。
 * static access bridge for event runtime: registers event-runtime instance providers.
 */
@Component
public final class GameEventServices implements DisposableBean {

    /**
     * 事件服务的 Spring 提供者。
     * Spring provider for the event service.
     */
    private static volatile ObjectProvider<EventService> eventServiceProvider;
    /**
     * 已解析的事件服务单例；仅在真正解析到 Spring bean 后缓存，避免把回退实例钉住。
     * Resolved event-service singleton; cached only after a real Spring bean is resolved, so a fallback
     * instance is never pinned.
     */
    private static volatile EventService resolvedEventService;
    /**
     * 玩家事件服务的 Spring 提供者。
     * Spring provider for the player-event service.
     */
    private static volatile ObjectProvider<PlayerEventService> playerEventServiceProvider;
    /**
     * 已解析的玩家事件服务单例；语义同 {@link #resolvedEventService}。
     * Resolved player-event service singleton; same contract as {@link #resolvedEventService}.
     */
    private static volatile PlayerEventService resolvedPlayerEventService;
    /**
     * 疯狂守护者服务的 Spring 提供者。
     * Spring provider for the Crazy-Daeva service.
     */
    private static volatile ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider;
    /**
     * 已解析的疯狂守护者服务单例；语义同 {@link #resolvedEventService}。
     * Resolved Crazy-Daeva service singleton; same contract as {@link #resolvedEventService}.
     */
    private static volatile CrazyDaevaService resolvedCrazyDaevaService;
    /**
     * 欧比斯排名更新服务的 Spring 提供者。
     * Spring provider for the abyss-rank update service.
     */
    private static volatile ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider;
    /**
     * 已解析的欧比斯排名更新服务单例；语义同 {@link #resolvedEventService}。
     * Resolved abyss-rank update service singleton; same contract as {@link #resolvedEventService}.
     */
    private static volatile AbyssRankUpdateService resolvedAbyssRankUpdateService;
    /**
     * 数据包广播器的 Spring 提供者。
     * Spring provider for the packet broadcaster.
     */
    private static volatile ObjectProvider<PacketBroadcaster> packetBroadcasterProvider;
    /**
     * 已解析的数据包广播器单例；语义同 {@link #resolvedEventService}。
     * Resolved packet-broadcaster singleton; same contract as {@link #resolvedEventService}.
     */
    private static volatile PacketBroadcaster resolvedPacketBroadcaster;
    /**
     * 事件调度器的 Spring 提供者。
     * Spring provider for the event scheduler.
     */
    private static volatile ObjectProvider<EventScheduler> eventSchedulerProvider;
    /**
     * 已解析的事件调度器单例；语义同 {@link #resolvedEventService}。
     * Resolved event-scheduler singleton; same contract as {@link #resolvedEventService}.
     */
    private static volatile EventScheduler resolvedEventScheduler;

    /**
     * 构造并注册各事件运行时实例提供者。
     * Construct and register instance providers for each event-runtime service.
     *
     * @param eventServiceProvider 事件服务提供者 / Event-service provider
     * @param playerEventServiceProvider 玩家事件服务提供者 / Player-event service provider
     * @param crazyDaevaServiceProvider 疯狂守护者服务提供者 / Crazy-Daeva service provider
     * @param abyssRankUpdateServiceProvider 欧比斯排名更新服务提供者 / Abyss-rank update service provider
     * @param packetBroadcasterProvider 数据包广播器提供者 / Packet-broadcaster provider
     * @param eventSchedulerProvider 事件调度器提供者 / Event-scheduler provider
     */
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

    /**
     * 解析事件服务。
     * Resolve the event service.
     *
     * @return 事件服务 / Event service
     */
    public static EventService eventService() {
        EventService resolved = resolvedEventService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<EventService> provider = eventServiceProvider;
        if (provider == null) {
            return EventService.getInstance();
        }
        resolved = provider.getIfAvailable();
        if (resolved == null) {
            return EventService.getInstance();
        }
        resolvedEventService = resolved;
        return resolved;
    }

    /**
     * 解析玩家事件服务。
     * Resolve the player-event service.
     *
     * @return 玩家事件服务 / Player-event service
     */
    public static PlayerEventService playerEventService() {
        PlayerEventService resolved = resolvedPlayerEventService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<PlayerEventService> provider = playerEventServiceProvider;
        if (provider == null) {
            return GameEventRuntimeFallbacks.playerEventService();
        }
        resolved = provider.getIfAvailable();
        if (resolved == null) {
            return GameEventRuntimeFallbacks.playerEventService();
        }
        resolvedPlayerEventService = resolved;
        return resolved;
    }

    /**
     * 解析疯狂守护者服务。
     * Resolve the Crazy-Daeva service.
     *
     * @return 疯狂守护者服务 / Crazy-Daeva service
     */
    public static CrazyDaevaService crazyDaevaService() {
        CrazyDaevaService resolved = resolvedCrazyDaevaService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<CrazyDaevaService> provider = crazyDaevaServiceProvider;
        if (provider == null) {
            return GameEventRuntimeFallbacks.crazyDaevaService();
        }
        resolved = provider.getIfAvailable();
        if (resolved == null) {
            return GameEventRuntimeFallbacks.crazyDaevaService();
        }
        resolvedCrazyDaevaService = resolved;
        return resolved;
    }

    /**
     * 解析欧比斯排名更新服务。
     * Resolve the abyss-rank update service.
     *
     * @return 欧比斯排名更新服务 / Abyss-rank update service
     */
    public static AbyssRankUpdateService abyssRankUpdateService() {
        AbyssRankUpdateService resolved = resolvedAbyssRankUpdateService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<AbyssRankUpdateService> provider = abyssRankUpdateServiceProvider;
        if (provider == null) {
            return GameEventRuntimeFallbacks.abyssRankUpdateService();
        }
        resolved = provider.getIfAvailable();
        if (resolved == null) {
            return GameEventRuntimeFallbacks.abyssRankUpdateService();
        }
        resolvedAbyssRankUpdateService = resolved;
        return resolved;
    }

    /**
     * 解析数据包广播器。
     * Resolve the packet broadcaster.
     *
     * @return 数据包广播器 / Packet broadcaster
     */
    public static PacketBroadcaster packetBroadcaster() {
        PacketBroadcaster resolved = resolvedPacketBroadcaster;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<PacketBroadcaster> provider = packetBroadcasterProvider;
        if (provider == null) {
            return GameEventRuntimeFallbacks.packetBroadcaster();
        }
        resolved = provider.getIfAvailable();
        if (resolved == null) {
            return GameEventRuntimeFallbacks.packetBroadcaster();
        }
        resolvedPacketBroadcaster = resolved;
        return resolved;
    }

    /**
     * 解析事件调度器。
     * Resolve the event scheduler.
     *
     * @return 事件调度器 / Event scheduler
     */
    public static EventScheduler eventScheduler() {
        EventScheduler resolved = resolvedEventScheduler;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<EventScheduler> provider = eventSchedulerProvider;
        if (provider == null) {
            return GameEventRuntimeFallbacks.eventScheduler();
        }
        resolved = provider.getIfAvailable();
        if (resolved == null) {
            return GameEventRuntimeFallbacks.eventScheduler();
        }
        resolvedEventScheduler = resolved;
        return resolved;
    }

    /**
     * 销毁时清理静态提供者与服务实例桥。
     * Clear static providers and service instance bridges on destroy.
     */
    @Override
    public void destroy() {
        eventServiceProvider = null;
        playerEventServiceProvider = null;
        crazyDaevaServiceProvider = null;
        abyssRankUpdateServiceProvider = null;
        packetBroadcasterProvider = null;
        eventSchedulerProvider = null;
        resolvedEventService = null;
        resolvedPlayerEventService = null;
        resolvedCrazyDaevaService = null;
        resolvedAbyssRankUpdateService = null;
        resolvedPacketBroadcaster = null;
        resolvedEventScheduler = null;
        EventService.setInstanceProvider(null);
        PlayerEventService.setInstanceProvider(null);
        CrazyDaevaService.setInstanceProvider(null);
        AbyssRankUpdateService.setInstanceProvider(null);
        PacketBroadcaster.setInstanceProvider(null);
        EventScheduler.setInstanceProvider(null);
    }
}
