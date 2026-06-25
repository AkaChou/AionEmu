package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.boot.lifecycle.AionProcessRuntimeBridge;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameAdminPanelShutdownRequestTest {

    @AfterEach
    void resetProvider() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedShutdownHandler.clear();
        GameAdminPanelShutdownRequest.setProcessRuntimeBridgeProvider(null);
    }

    @Test
    void embeddedShutdownRequestsBootShutdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        GameAdminPanelShutdownRequest.shutdown();

        assertEquals(AionEmbeddedShutdownMode.SHUTDOWN, requestedMode.get());
    }

    @Test
    void shutdownUsesProcessRuntimeBridgeProviderOutsideBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        GameAdminPanelShutdownRequest.setProcessRuntimeBridgeProvider(provider(new RecordingProcessRuntimeBridge(events)));

        GameAdminPanelShutdownRequest.shutdown();

        assertEquals(List.of("exit:0"), events);
    }

    private static ObjectProvider<AionProcessRuntimeBridge> provider(AionProcessRuntimeBridge runtimeBridge) {
        return ObjectProvider.class.cast(Proxy.newProxyInstance(
            ObjectProvider.class.getClassLoader(),
            new Class<?>[] { ObjectProvider.class },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "processRuntimeBridgeProvider";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                if ("getIfAvailable".equals(method.getName()) || "getObject".equals(method.getName())) {
                    return runtimeBridge;
                }
                throw new UnsupportedOperationException(method.toString());
            }
        ));
    }

    private static final class RecordingProcessRuntimeBridge extends AionProcessRuntimeBridge {

        private final List<String> events;

        private RecordingProcessRuntimeBridge(List<String> events) {
            this.events = events;
        }

        @Override
        public void exit(int status) {
            events.add("exit:" + status);
        }
    }
}
