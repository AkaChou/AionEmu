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
 * 事件服务静态绑定：把 Spring ObjectProvider 喂入事件域服务自持的 fail-fast 单例通道。
 * Event static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Event services.
 * <p>本组件取代已退役的事件静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Event static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class EventServiceBindings implements DisposableBean {

    /**
     * 构造并注册各事件服务的实例提供者。
     * Construct and register instance providers for Event services.
     * @param eventServiceProvider EventService 提供者 / EventService provider
     * @param playerEventServiceProvider PlayerEventService 提供者 / PlayerEventService provider
     * @param crazyDaevaServiceProvider CrazyDaevaService 提供者 / CrazyDaevaService provider
     * @param abyssRankUpdateServiceProvider AbyssRankUpdateService 提供者 / AbyssRankUpdateService provider
     * @param packetBroadcasterProvider PacketBroadcaster 提供者 / PacketBroadcaster provider
     * @param eventSchedulerProvider EventScheduler 提供者 / EventScheduler provider
     */
    public EventServiceBindings(ObjectProvider<EventService> eventServiceProvider,
            ObjectProvider<PlayerEventService> playerEventServiceProvider,
            ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider,
            ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider,
            ObjectProvider<PacketBroadcaster> packetBroadcasterProvider,
            ObjectProvider<EventScheduler> eventSchedulerProvider) {
        EventService.setInstanceProvider(eventServiceProvider);
        PlayerEventService.setInstanceProvider(playerEventServiceProvider);
        CrazyDaevaService.setInstanceProvider(crazyDaevaServiceProvider);
        AbyssRankUpdateService.setInstanceProvider(abyssRankUpdateServiceProvider);
        PacketBroadcaster.setInstanceProvider(packetBroadcasterProvider);
        EventScheduler.setInstanceProvider(eventSchedulerProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        EventService.setInstanceProvider(null);
        PlayerEventService.setInstanceProvider(null);
        CrazyDaevaService.setInstanceProvider(null);
        AbyssRankUpdateService.setInstanceProvider(null);
        PacketBroadcaster.setInstanceProvider(null);
        EventScheduler.setInstanceProvider(null);
    }
}
