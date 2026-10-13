package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
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
     * 可选注入事件服务提供者。
     * Optionally inject the event-service provider.
     * @param eventServiceProvider 事件服务提供者 / Event-service provider
     */
    @Autowired(required = false)
    void setEventServiceProvider(ObjectProvider<EventService> eventServiceProvider) {
        this.eventServiceProvider = eventServiceProvider;
    }

    /**
     * 可选注入玩家事件服务提供者。
     * Optionally inject the player-event service provider.
     * @param playerEventServiceProvider 玩家事件服务提供者 / Player-event service provider
     */
    @Autowired(required = false)
    void setPlayerEventServiceProvider(ObjectProvider<PlayerEventService> playerEventServiceProvider) {
        this.playerEventServiceProvider = playerEventServiceProvider;
    }

    /**
     * 可选注入疯狂守护者服务提供者。
     * Optionally inject the Crazy-Daeva service provider.
     * @param crazyDaevaServiceProvider 疯狂守护者服务提供者 / Crazy-Daeva service provider
     */
    @Autowired(required = false)
    void setCrazyDaevaServiceProvider(ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider) {
        this.crazyDaevaServiceProvider = crazyDaevaServiceProvider;
    }

    /**
     * 可选注入欧比斯排名更新服务提供者。
     * Optionally inject the abyss-rank update service provider.
     * @param abyssRankUpdateServiceProvider 欧比斯排名更新服务提供者 / Abyss-rank update service provider
     */
    @Autowired(required = false)
    void setAbyssRankUpdateServiceProvider(ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider) {
        this.abyssRankUpdateServiceProvider = abyssRankUpdateServiceProvider;
    }

    /**
     * 可选注入数据包广播器提供者。
     * Optionally inject the packet-broadcaster provider.
     * @param packetBroadcasterProvider 数据包广播器提供者 / Packet-broadcaster provider
     */
    @Autowired(required = false)
    void setPacketBroadcasterProvider(ObjectProvider<PacketBroadcaster> packetBroadcasterProvider) {
        this.packetBroadcasterProvider = packetBroadcasterProvider;
    }

    /**
     * 启动事件运行时：按开关启停各子系统并调度排名更新。
     * Start the event runtime: enable subsystems per flags and schedule ranking updates.
     */
    public void start() {
        printEventsSection();
        if (isEventServiceEnabled()) {
            eventService().start();
        }
        if (isPlayerEventEnabled()) {
            playerEventService();
        }
        if (isCrazyDaevaEnabled()) {
            crazyDaevaService().startTimer();
        }
        AbyssRankUpdateService abyssRankUpdateService = abyssRankUpdateService();
        if (isTopRankingUpdateEnabled()) {
            abyssRankUpdateService.scheduleUpdateHour();
        } else {
            abyssRankUpdateService.scheduleUpdateMinute();
        }
        abyssRankUpdateService.initRewardWeeklyManager();
        packetBroadcaster();
        spawnTemporarySpawns();
    }

    /**
     * 打印事件分区标题。
     * Print the events section header.
     */
    private void printEventsSection() {
        Util.printSection(I18n.get("console.section.events"));
    }

    /**
     * 事件服务是否启用。
     * Whether the event service is enabled.
     * @return 启用则 true / true when enabled
     */
    private boolean isEventServiceEnabled() {
        return EventsConfig.ENABLE_EVENT_SERVICE;
    }

    /**
     * 玩家事件是否启用。
     * Whether player events are enabled.
     * @return 启用则 true / true when enabled
     */
    private boolean isPlayerEventEnabled() {
        return EventsConfig.EVENT_ENABLED;
    }

    /**
     * 疯狂守护者是否启用。
     * Whether Crazy Daeva is enabled.
     * @return 启用则 true / true when enabled
     */
    private boolean isCrazyDaevaEnabled() {
        return EventsConfig.ENABLE_CRAZY;
    }

    /**
     * 排名更新开关是否为小时级。
     * Whether the ranking update switch is hourly.
     * @return 小时级则 true / true for hourly
     */
    private boolean isTopRankingUpdateEnabled() {
        return RankingConfig.TOP_RANKING_UPDATE_SETTING;
    }

    /**
     * 生成临时刷怪。
     * Spawn the temporary spawns.
     */
    private void spawnTemporarySpawns() {
        TemporarySpawnEngine.spawnAll();
    }

    /**
     * 解析事件服务。
     * Resolve the event service.
     * @return 事件服务 / Event service
     */
    private EventService eventService() {
        return eventServiceProvider.getObject();
    }

    /**
     * 解析玩家事件服务。
     * Resolve the player-event service.
     * @return 玩家事件服务 / Player-event service
     */
    private PlayerEventService playerEventService() {
        return playerEventServiceProvider.getObject();
    }

    /**
     * 解析疯狂守护者服务。
     * Resolve the Crazy-Daeva service.
     * @return 疯狂守护者服务 / Crazy-Daeva service
     */
    private CrazyDaevaService crazyDaevaService() {
        return crazyDaevaServiceProvider.getObject();
    }

    /**
     * 解析欧比斯排名更新服务。
     * Resolve the abyss-rank update service.
     * @return 欧比斯排名更新服务 / Abyss-rank update service
     */
    private AbyssRankUpdateService abyssRankUpdateService() {
        return abyssRankUpdateServiceProvider.getObject();
    }

    /**
     * 解析数据包广播器。
     * Resolve the packet broadcaster.
     * @return 数据包广播器 / Packet broadcaster
     */
    private PacketBroadcaster packetBroadcaster() {
        return packetBroadcasterProvider.getObject();
    }

}
