package com.aionemu.loginserver.taskmanager.handler.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.taskmanager.TaskRuntime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LoginTaskShutdownHandlerTest {

    @AfterEach
    void resetEmbeddedMode() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedShutdownHandler.clear();
    }

    @Test
    void embeddedShutdownTaskRequestsBootShutdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        var requestedMode = new java.util.concurrent.atomic.AtomicReference<AionEmbeddedShutdownMode>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        ShutdownHandler handler = new ShutdownHandler();
        handler.setTaskId(1);
        handler.trigger();

        assertEquals(AionEmbeddedShutdownMode.SHUTDOWN, requestedMode.get());
    }

    @Test
    void embeddedRestartTaskRequestsBootShutdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        var requestedMode = new java.util.concurrent.atomic.AtomicReference<AionEmbeddedShutdownMode>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        RestartHandler handler = new RestartHandler();
        handler.setTaskId(2);
        handler.trigger();

        assertEquals(AionEmbeddedShutdownMode.RESTART, requestedMode.get());
    }

    @Test
    void shutdownTaskUsesRuntimeShutdownOutsideBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        ShutdownHandler handler = new ShutdownHandler();
        handler.setTaskId(3);
        handler.setRuntime(new TaskRuntime(null, null, new RecordingShutdown(events)));
        handler.trigger();

        assertEquals(List.of("restartOnly:false", "start"), events);
    }

    @Test
    void restartTaskUsesRuntimeShutdownOutsideBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        RestartHandler handler = new RestartHandler();
        handler.setTaskId(4);
        handler.setRuntime(new TaskRuntime(null, null, new RecordingShutdown(events)));
        handler.trigger();

        assertEquals(List.of("restartOnly:true", "start"), events);
    }

    private static final class RecordingShutdown extends Shutdown {

        private final List<String> events;

        private RecordingShutdown(List<String> events) {
            super(null, null, null, null);
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
