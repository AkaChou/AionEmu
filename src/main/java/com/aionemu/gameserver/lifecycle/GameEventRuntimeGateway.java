package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 事件运行时网关：按配置启动事件、排名更新、广播与临时刷怪。
 * Event-runtime gateway: starts events, ranking updates, broadcasting, and temporary spawns per config.
 */
@Component
public class GameEventRuntimeGateway {

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
     * 运行时桥接提供者。
     * Runtime-bridge provider.
     */
    private ObjectProvider<GameEventRuntimeBridge> runtimeBridgeProvider;

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
     * 可选注入运行时桥接提供者。
     * Optionally inject the runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥接提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameEventRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 启动事件运行时：按开关启停各子系统并调度排名更新。
     * Start the event runtime: enable subsystems per flags and schedule ranking updates.
     */
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

    /**
     * 解析事件服务。
     * Resolve the event service.
     *
     * Event service
     */
    private EventService eventService() {
        if (eventServiceProvider == null) {
            return runtimeBridge().eventService();
        }
        return eventServiceProvider.getIfAvailable(() -> runtimeBridge().eventService());
    }

    /**
     * 解析玩家事件服务。
     * Resolve the player-event service.
     *
     * @return 玩家事件服务 / Player-event service
     */
    private PlayerEventService playerEventService() {
        if (playerEventServiceProvider == null) {
            return runtimeBridge().playerEventService();
        }
        return playerEventServiceProvider.getIfAvailable(() -> runtimeBridge().playerEventService());
    }

    /**
     * 解析疯狂守护者服务。
     * Resolve the Crazy-Daeva service.
     *
     * @return 疯狂守护者服务 / Crazy-Daeva service
     */
    private CrazyDaevaService crazyDaevaService() {
        if (crazyDaevaServiceProvider == null) {
            return runtimeBridge().crazyDaevaService();
        }
        return crazyDaevaServiceProvider.getIfAvailable(() -> runtimeBridge().crazyDaevaService());
    }

    /**
     * 解析欧比斯排名更新服务。
     * Resolve the abyss-rank update service.
     *
     * @return 欧比斯排名更新服务 / Abyss-rank update service
     */
    private AbyssRankUpdateService abyssRankUpdateService() {
        if (abyssRankUpdateServiceProvider == null) {
            return runtimeBridge().abyssRankUpdateService();
        }
        return abyssRankUpdateServiceProvider.getIfAvailable(() -> runtimeBridge().abyssRankUpdateService());
    }

    /**
     * 解析数据包广播器。
     * Resolve the packet broadcaster.
     *
     * @return 数据包广播器 / Packet broadcaster
     */
    private PacketBroadcaster packetBroadcaster() {
        if (packetBroadcasterProvider == null) {
            return runtimeBridge().packetBroadcaster();
        }
        return packetBroadcasterProvider.getIfAvailable(() -> runtimeBridge().packetBroadcaster());
    }

    /**
     * 解析运行时桥接。
     * Resolve the runtime bridge.
     *
     * @return 运行时桥接 / Runtime bridge
     */
    private GameEventRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameEventRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameEventRuntimeBridge::new);
    }
}
