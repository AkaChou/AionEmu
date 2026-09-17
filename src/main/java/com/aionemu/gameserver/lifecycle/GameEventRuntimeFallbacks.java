package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.eventEngine.EventScheduler;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;

/**
 * 事件运行时服务的回退工厂：Spring 提供者不可用时返回各事件运行时单例；已退役双源兜底的组件直接 fail-fast。
 * Fallback factory for event-runtime services: returns each event-runtime singleton when Spring providers are
 * unavailable; components whose dual-source fallback is retired fail fast instead.
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
        return EventService.getInstance();
    }

    /**
     * 返回玩家事件服务回退实例。
     * Return the player-event service fallback instance.
     *
     * @return 玩家事件服务 / Player-event service
     */
    static PlayerEventService playerEventService() {
        return PlayerEventService.getInstance();
    }

    /**
     * 返回疯狂守护者服务回退实例。
     * Return the Crazy-Daeva service fallback instance.
     *
     * @return 疯狂守护者服务 / Crazy-Daeva service
     */
    static CrazyDaevaService crazyDaevaService() {
        return CrazyDaevaService.getInstance();
    }

    /**
     * 返回欧比斯排名更新服务回退实例。
     * Return the abyss-rank update service fallback instance.
     *
     * @return 欧比斯排名更新服务 / Abyss-rank update service
     */
    static AbyssRankUpdateService abyssRankUpdateService() {
        return AbyssRankUpdateService.getInstance();
    }

    /**
     * 返回数据包广播器回退实例。
     * Return the packet-broadcaster fallback instance.
     *
     * @return 数据包广播器 / Packet broadcaster
     */
    static PacketBroadcaster packetBroadcaster() {
        return PacketBroadcaster.getInstance();
    }

    /**
     * 返回 EventScheduler：双源兜底已退役，交由 {@link EventScheduler#getInstance()} fail-fast。
     * Returns EventScheduler: the dual-source fallback is retired;
     * delegates to EventScheduler.getInstance() and fails fast.
     *
     * @return EventScheduler 实例 / EventScheduler instance
     */
    static EventScheduler eventScheduler() {
        return EventScheduler.getInstance();
    }
}
