package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void runtimeBridgeDoesNotCallLegacySingletonsDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameEventRuntimeBridge.java"));

        assertFalse(source.contains("EventService.getInstance()"));
        assertFalse(source.contains("PlayerEventService.getInstance()"));
        assertFalse(source.contains("CrazyDaevaService.getInstance()"));
        assertFalse(source.contains("AbyssRankUpdateService.getInstance()"));
        assertFalse(source.contains("PacketBroadcaster.getInstance()"));
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
