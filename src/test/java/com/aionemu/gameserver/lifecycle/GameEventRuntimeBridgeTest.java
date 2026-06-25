package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class GameEventRuntimeBridgeTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        EventService eventService = instance(EventService.class);
        PlayerEventService playerEventService = instance(PlayerEventService.class);
        CrazyDaevaService crazyDaevaService = instance(CrazyDaevaService.class);
        AbyssRankUpdateService abyssRankUpdateService = instance(AbyssRankUpdateService.class);
        PacketBroadcaster packetBroadcaster = instance(PacketBroadcaster.class);
        GameEventRuntimeBridge runtimeBridge = new GameEventRuntimeBridge();

        runtimeBridge.setEventServiceProvider(provider(EventService.class, eventService));
        runtimeBridge.setPlayerEventServiceProvider(provider(PlayerEventService.class, playerEventService));
        runtimeBridge.setCrazyDaevaServiceProvider(provider(CrazyDaevaService.class, crazyDaevaService));
        runtimeBridge.setAbyssRankUpdateServiceProvider(provider(AbyssRankUpdateService.class, abyssRankUpdateService));
        runtimeBridge.setPacketBroadcasterProvider(provider(PacketBroadcaster.class, packetBroadcaster));

        assertSame(eventService, runtimeBridge.eventService());
        assertSame(playerEventService, runtimeBridge.playerEventService());
        assertSame(crazyDaevaService, runtimeBridge.crazyDaevaService());
        assertSame(abyssRankUpdateService, runtimeBridge.abyssRankUpdateService());
        assertSame(packetBroadcaster, runtimeBridge.packetBroadcaster());
    }

    private <T> T instance(Class<T> type) {
        return objenesis.newInstance(type);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
