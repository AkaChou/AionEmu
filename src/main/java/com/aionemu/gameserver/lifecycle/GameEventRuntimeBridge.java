package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.configs.main.RankingConfig;
import com.aionemu.gameserver.eventEngine.EventScheduler;
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

/**
 * 事件运行时桥接：解析事件服务并暴露配置开关与临时刷怪。
 * Event-runtime bridge: resolves event services and exposes config flags and temporary spawns.
 */
@Component
public class GameEventRuntimeBridge {

    /**
     * 事件服务提供者。
     * Event-service provider.
     */
    private ObjectProvider<EventService> eventServiceProvider;
    /**
     * 玩家事件服务提供者。
     * Player-event service provider.
     */
    private ObjectProvider<PlayerEventService> playerEventServiceProvider;
    /**
     * 疯狂守护者服务提供者。
     * Crazy-Daeva service provider.
     */
    private ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider;
    /**
     * 欧比斯排名更新服务提供者。
     * Abyss-rank update service provider.
     */
    private ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider;
    /**
     * 数据包广播器提供者。
     * Packet-broadcaster provider.
     */
    private ObjectProvider<PacketBroadcaster> packetBroadcasterProvider;
    /**
     * 事件调度器提供者。
     * Event-scheduler provider.
     */
    private ObjectProvider<EventScheduler> eventSchedulerProvider;

    /**
     * 可选注入事件服务提供者。
     * Optionally inject the event-service provider.
     *
     * @param eventServiceProvider 事件服务提供者 / Event-service provider
     */
    @Autowired(required = false)
    void setEventServiceProvider(ObjectProvider<EventService> eventServiceProvider) {
        this.eventServiceProvider = eventServiceProvider;
    }

    /**
     * 可选注入玩家事件服务提供者。
     * Optionally inject the player-event service provider.
     *
     * @param playerEventServiceProvider 玩家事件服务提供者 / Player-event service provider
     */
    @Autowired(required = false)
    void setPlayerEventServiceProvider(ObjectProvider<PlayerEventService> playerEventServiceProvider) {
        this.playerEventServiceProvider = playerEventServiceProvider;
    }

    /**
     * 可选注入疯狂守护者服务提供者。
     * Optionally inject the Crazy-Daeva service provider.
     *
     * @param crazyDaevaServiceProvider 疯狂守护者服务提供者 / Crazy-Daeva service provider
     */
    @Autowired(required = false)
    void setCrazyDaevaServiceProvider(ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider) {
        this.crazyDaevaServiceProvider = crazyDaevaServiceProvider;
    }

    /**
     * 可选注入欧比斯排名更新服务提供者。
     * Optionally inject the abyss-rank update service provider.
     *
     * @param abyssRankUpdateServiceProvider 欧比斯排名更新服务提供者 / Abyss-rank update service provider
     */
    @Autowired(required = false)
    void setAbyssRankUpdateServiceProvider(ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider) {
        this.abyssRankUpdateServiceProvider = abyssRankUpdateServiceProvider;
    }

    /**
     * 可选注入数据包广播器提供者。
     * Optionally inject the packet-broadcaster provider.
     *
     * @param packetBroadcasterProvider 数据包广播器提供者 / Packet-broadcaster provider
     */
    @Autowired(required = false)
    void setPacketBroadcasterProvider(ObjectProvider<PacketBroadcaster> packetBroadcasterProvider) {
        this.packetBroadcasterProvider = packetBroadcasterProvider;
    }

    /**
     * 可选注入事件调度器提供者。
     * Optionally inject the event-scheduler provider.
     *
     * @param eventSchedulerProvider 事件调度器提供者 / Event-scheduler provider
     */
    @Autowired(required = false)
    void setEventSchedulerProvider(ObjectProvider<EventScheduler> eventSchedulerProvider) {
        this.eventSchedulerProvider = eventSchedulerProvider;
    }

    /**
     * 打印事件分区标题。
     * Print the events section header.
     */
    public void printEventsSection() {
        Util.printSection(I18n.get("console.section.events"));
    }

    /**
     * 事件服务是否启用。
     * Whether the event service is enabled.
     *
     * @return {@code true} if enabled。
     */
    public boolean isEventServiceEnabled() {
        return EventsConfig.ENABLE_EVENT_SERVICE;
    }

    /**
     * 玩家事件是否启用。
     * Whether player events are enabled.
     *
     * @return {@code true} if enabled。
     */
    public boolean isPlayerEventEnabled() {
        return EventsConfig.EVENT_ENABLED;
    }

    /**
     * 疯狂守护者是否启用。
     * Whether Crazy Daeva is enabled.
     *
     * @return {@code true} if enabled。
     */
    public boolean isCrazyDaevaEnabled() {
        return EventsConfig.ENABLE_CRAZY;
    }

    /**
     * 顶级排名更新是否启用（按小时调度）。
     * Whether top-ranking update is enabled (hourly schedule).
     *
     * @return {@code true} if enabled。
     */
    public boolean isTopRankingUpdateEnabled() {
        return RankingConfig.TOP_RANKING_UPDATE_SETTING;
    }

    /**
     * 解析事件服务。
     * Resolve the event service.
     *
     * Event service
     */
    public EventService eventService() {
        if (eventServiceProvider == null) {
            return GameEventRuntimeFallbacks.eventService();
        }
        return eventServiceProvider.getIfAvailable(GameEventRuntimeFallbacks::eventService);
    }

    /**
     * 解析玩家事件服务。
     * Resolve the player-event service.
     *
     * @return 玩家事件服务 / Player-event service
     */
    public PlayerEventService playerEventService() {
        if (playerEventServiceProvider == null) {
            return GameEventRuntimeFallbacks.playerEventService();
        }
        return playerEventServiceProvider.getIfAvailable(GameEventRuntimeFallbacks::playerEventService);
    }

    /**
     * 解析疯狂守护者服务。
     * Resolve the Crazy-Daeva service.
     *
     * @return 疯狂守护者服务 / Crazy-Daeva service
     */
    public CrazyDaevaService crazyDaevaService() {
        if (crazyDaevaServiceProvider == null) {
            return GameEventRuntimeFallbacks.crazyDaevaService();
        }
        return crazyDaevaServiceProvider.getIfAvailable(GameEventRuntimeFallbacks::crazyDaevaService);
    }

    /**
     * 解析欧比斯排名更新服务。
     * Resolve the abyss-rank update service.
     *
     * @return 欧比斯排名更新服务 / Abyss-rank update service
     */
    public AbyssRankUpdateService abyssRankUpdateService() {
        if (abyssRankUpdateServiceProvider == null) {
            return GameEventRuntimeFallbacks.abyssRankUpdateService();
        }
        return abyssRankUpdateServiceProvider.getIfAvailable(GameEventRuntimeFallbacks::abyssRankUpdateService);
    }

    /**
     * 解析数据包广播器。
     * Resolve the packet broadcaster.
     *
     * @return 数据包广播器 / Packet broadcaster
     */
    public PacketBroadcaster packetBroadcaster() {
        if (packetBroadcasterProvider == null) {
            return GameEventRuntimeFallbacks.packetBroadcaster();
        }
        return packetBroadcasterProvider.getIfAvailable(GameEventRuntimeFallbacks::packetBroadcaster);
    }

    /**
     * 解析事件调度器。
     * Resolve the event scheduler.
     *
     * @return 事件调度器 / Event scheduler
     */
    public EventScheduler eventScheduler() {
        if (eventSchedulerProvider == null) {
            return GameEventRuntimeFallbacks.eventScheduler();
        }
        return eventSchedulerProvider.getIfAvailable(GameEventRuntimeFallbacks::eventScheduler);
    }

    /**
     * 刷出全部临时刷怪。
     * Spawn all temporary spawns.
     */
    public void spawnTemporarySpawns() {
        TemporarySpawnEngine.spawnAll();
    }
}
