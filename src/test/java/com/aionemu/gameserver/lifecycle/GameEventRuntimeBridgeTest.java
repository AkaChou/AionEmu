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
import java.util.List;
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

    @Test
    void gameServerCodeUsesEventServiceBridgeInsteadOfDirectSingleton() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            List<Path> sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/EventService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameEventServices.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameEventRuntimeFallbacks.java")))
                .toList();

            for (Path sourcePath : sources) {
                String source = Files.readString(sourcePath);

                assertFalse(source.matches("(?s).*\\bEventService\\.getInstance\\(\\).*"), sourcePath.toString());
            }
        }
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
