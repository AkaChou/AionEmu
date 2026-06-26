package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameShutdownRequestTest {

    @AfterEach
    void resetProvider() {
        GameShutdownRequest.setShutdownHookProvider(null);
    }

    @Test
    void doShutdownUsesShutdownHookProviderBeforeLegacySingletonFallback() {
        List<String> events = new ArrayList<>();
        GameShutdownRequest.setShutdownHookProvider(provider(new RecordingShutdownHook(events)));

        GameShutdownRequest.doShutdown(10, 5, ShutdownMode.RESTART);

        assertEquals(List.of("doShutdown:10:5:RESTART"), events);
    }

    @Test
    void completeShutdownUsesShutdownHookProviderBeforeLegacySingletonFallback() {
        List<String> events = new ArrayList<>();
        GameShutdownRequest.setShutdownHookProvider(provider(new RecordingShutdownHook(events)));

        GameShutdownRequest.completeShutdown(ShutdownMode.SHUTDOWN, false);

        assertEquals(List.of("completeShutdown:SHUTDOWN:false"), events);
    }

    @Test
    void shutdownRequestDoesNotCallLegacyShutdownHookDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameShutdownRequest.java"));

        assertFalse(source.contains("ShutdownHook.getInstance()"));
    }

    private static ObjectProvider<ShutdownHook> provider(ShutdownHook shutdownHook) {
        return ObjectProvider.class.cast(Proxy.newProxyInstance(
            ObjectProvider.class.getClassLoader(),
            new Class<?>[] { ObjectProvider.class },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "shutdownHookProvider";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                if ("getIfAvailable".equals(method.getName()) || "getObject".equals(method.getName())) {
                    return shutdownHook;
                }
                throw new UnsupportedOperationException(method.toString());
            }
        ));
    }

    private static final class RecordingShutdownHook extends ShutdownHook {

        private final List<String> events;

        private RecordingShutdownHook(List<String> events) {
            this.events = events;
        }

        @Override
        public void doShutdown(int delay, int announceInterval, ShutdownMode mode) {
            events.add("doShutdown:" + delay + ":" + announceInterval + ":" + mode);
        }

        @Override
        public void completeShutdown(ShutdownMode mode, boolean haltRuntime) {
            events.add("completeShutdown:" + mode + ":" + haltRuntime);
        }
    }
}
