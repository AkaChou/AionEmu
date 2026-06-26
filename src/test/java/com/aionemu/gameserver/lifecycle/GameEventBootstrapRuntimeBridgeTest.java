package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class GameEventBootstrapRuntimeBridgeTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        LunaShopService lunaShopService = instance(LunaShopService.class);
        MinionService minionService = instance(MinionService.class);
        ShugoSweepService shugoSweepService = instance(ShugoSweepService.class);
        AtreianPassportService atreianPassportService = instance(AtreianPassportService.class);
        EventWindowService eventWindowService = instance(EventWindowService.class);
        GameEventBootstrapRuntimeBridge runtimeBridge = new GameEventBootstrapRuntimeBridge();

        runtimeBridge.setLunaShopServiceProvider(provider(LunaShopService.class, lunaShopService));
        runtimeBridge.setMinionServiceProvider(provider(MinionService.class, minionService));
        runtimeBridge.setShugoSweepServiceProvider(provider(ShugoSweepService.class, shugoSweepService));
        runtimeBridge.setAtreianPassportServiceProvider(provider(AtreianPassportService.class, atreianPassportService));
        runtimeBridge.setEventWindowServiceProvider(provider(EventWindowService.class, eventWindowService));

        assertSame(lunaShopService, runtimeBridge.lunaShopService());
        assertSame(minionService, runtimeBridge.minionService());
        assertSame(shugoSweepService, runtimeBridge.shugoSweepService());
        assertSame(atreianPassportService, runtimeBridge.atreianPassportService());
        assertSame(eventWindowService, runtimeBridge.eventWindowService());
    }

    @Test
    void runtimeBridgeDoesNotCallLegacySingletonsDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameEventBootstrapRuntimeBridge.java"));

        assertFalse(source.contains("LunaShopService.getInstance()"));
        assertFalse(source.contains("MinionService.getInstance()"));
        assertFalse(source.contains("ShugoSweepService.getInstance()"));
        assertFalse(source.contains("AtreianPassportService.getInstance()"));
        assertFalse(source.contains("EventWindowService.getInstance()"));
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
