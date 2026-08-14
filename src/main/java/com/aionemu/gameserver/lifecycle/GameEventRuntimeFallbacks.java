package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.eventEngine.EventScheduler;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;

/**
 * 事件运行时服务的回退工厂：在 Spring 提供者不可用时返回各事件运行时单例。
 * Fallback factory for event-runtime services: returns each event-runtime singleton when Spring providers are unavailable.
 */
final class GameEventRuntimeFallbacks {

    /**
     * 禁止实例化。
     * Prevent instantiation.
     */
    private GameEventRuntimeFallbacks() {
    }

    /**
     * 返回事件服务回退实例。
     * Return the event-service fallback instance.
     *
     * @return 事件服务 / Event service
     */
    static EventService eventService() {
        return EventServiceFallback.INSTANCE;
    }

    /**
     * 返回玩家事件服务回退实例。
     * Return the player-event service fallback instance.
     *
     * @return 玩家事件服务 / Player-event service
     */
    static PlayerEventService playerEventService() {
        return PlayerEventServiceFallback.INSTANCE;
    }

    /**
     * 返回疯狂守护者服务回退实例。
     * Return the Crazy-Daeva service fallback instance.
     *
     * @return 疯狂守护者服务 / Crazy-Daeva service
     */
    static CrazyDaevaService crazyDaevaService() {
        return CrazyDaevaServiceFallback.INSTANCE;
    }

    /**
     * 返回欧比斯排名更新服务回退实例。
     * Return the abyss-rank update service fallback instance.
     *
     * @return 欧比斯排名更新服务 / Abyss-rank update service
     */
    static AbyssRankUpdateService abyssRankUpdateService() {
        return AbyssRankUpdateServiceFallback.INSTANCE;
    }

    /**
     * 返回数据包广播器回退实例。
     * Return the packet-broadcaster fallback instance.
     *
     * @return 数据包广播器 / Packet broadcaster
     */
    static PacketBroadcaster packetBroadcaster() {
        return PacketBroadcasterFallback.INSTANCE;
    }

    /**
     * 返回事件调度器回退实例。
     * Return the event-scheduler fallback instance.
     *
     * @return 事件调度器 / Event scheduler
     */
    static EventScheduler eventScheduler() {
        return EventSchedulerFallback.INSTANCE;
    }

    /**
     * 事件服务懒加载回退持有者。
     * Lazy fallback holder for the event service.
     */
    private static final class EventServiceFallback {
        private static final EventService INSTANCE = EventService.getInstance();
    }

    /**
     * 玩家事件服务懒加载回退持有者。
     * Lazy fallback holder for the player-event service.
     */
    private static final class PlayerEventServiceFallback {
        private static final PlayerEventService INSTANCE = PlayerEventService.getInstance();
    }

    /**
     * 疯狂守护者服务懒加载回退持有者。
     * Lazy fallback holder for the Crazy-Daeva service.
     */
    private static final class CrazyDaevaServiceFallback {
        private static final CrazyDaevaService INSTANCE = CrazyDaevaService.getInstance();
    }

    /**
     * 欧比斯排名更新服务懒加载回退持有者。
     * Lazy fallback holder for the abyss-rank update service.
     */
    private static final class AbyssRankUpdateServiceFallback {
        private static final AbyssRankUpdateService INSTANCE = AbyssRankUpdateService.getInstance();
    }

    /**
     * 数据包广播器懒加载回退持有者。
     * Lazy fallback holder for the packet broadcaster.
     */
    private static final class PacketBroadcasterFallback {
        private static final PacketBroadcaster INSTANCE = PacketBroadcaster.getInstance();
    }

    /**
     * 事件调度器懒加载回退持有者。
     * Lazy fallback holder for the event scheduler.
     */
    private static final class EventSchedulerFallback {
        private static final EventScheduler INSTANCE = EventScheduler.getInstance();
    }
}
