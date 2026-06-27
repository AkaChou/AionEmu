package com.aionemu.loginserver.taskmanager.handler.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.service.LoginShutdownServices;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class LoginTaskShutdownHandlerTest {

    @AfterEach
    void resetEmbeddedMode() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedShutdownHandler.clear();
        LoginShutdownServices.setShutdownProvider(null);
    }

    @Test
    void embeddedShutdownTaskRequestsBootShutdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        ShutdownHandler handler = new ShutdownHandler();
        handler.setTaskId(1);
        handler.trigger();

        assertEquals(AionEmbeddedShutdownMode.SHUTDOWN, requestedMode.get());
    }

    @Test
    void embeddedRestartTaskRequestsBootShutdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        RestartHandler handler = new RestartHandler();
        handler.setTaskId(2);
        handler.trigger();

        assertEquals(AionEmbeddedShutdownMode.RESTART, requestedMode.get());
    }

    @Test
    void shutdownTaskUsesShutdownProviderOutsideBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        LoginShutdownServices.setShutdownProvider(provider(new RecordingShutdown(events)));

        ShutdownHandler handler = new ShutdownHandler();
        handler.setTaskId(3);
        handler.trigger();

        assertEquals(List.of("restartOnly:false", "start"), events);
    }

    @Test
    void restartTaskUsesShutdownProviderOutsideBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        LoginShutdownServices.setShutdownProvider(provider(new RecordingShutdown(events)));

        RestartHandler handler = new RestartHandler();
        handler.setTaskId(4);
        handler.trigger();

        assertEquals(List.of("restartOnly:true", "start"), events);
    }

    private static ObjectProvider<Shutdown> provider(Shutdown shutdown) {
        return new ObjectProvider<>() {
            @Override
            public Shutdown getObject(Object... args) {
                return shutdown;
            }

            @Override
            public Shutdown getIfAvailable() {
                return shutdown;
            }

            @Override
            public Shutdown getObject() {
                return shutdown;
            }
        };
    }

    private static final class RecordingShutdown extends Shutdown {

        private final List<String> events;

        private RecordingShutdown(List<String> events) {
            this.events = events;
        }

        @Override
        public void setRestartOnly(boolean restartOnly) {
            events.add("restartOnly:" + restartOnly);
        }

        @Override
        public synchronized void start() {
            events.add("start");
        }
    }
}
