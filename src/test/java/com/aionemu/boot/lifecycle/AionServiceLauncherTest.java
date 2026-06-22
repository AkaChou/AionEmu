package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.transport.AionTransportBoundary;
import com.aionemu.commons.utils.AionEmbeddedFailureHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

class AionServiceLauncherTest {

    @AfterEach
    void clearEmbeddedMode() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedFailureHandler.clear();
        AionEmbeddedShutdownHandler.clear();
    }

    @Test
    void startsEnabledServicesInPhaseOrderAndStopsStartedServicesInReverseOrder() throws Exception {
        assertFalse(AionRuntimeMode.isBootEmbedded());

        AionServicesProperties properties = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        RecordingTransportBoundary transportBoundary = new RecordingTransportBoundary(events);
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            transportBoundary,
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("chat", 200, false, events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments("--example=true"));
        launcher.destroy();

        assertEquals(List.of("prepare", "start:login", "start:game", "stop:game", "stop:login", "stop:transport"), events);
        assertTrue(AionRuntimeMode.isBootEmbedded());
    }

    @Test
    void startsChatBetweenLoginAndGameWhenChatIsEnabled() throws Exception {
        AionServicesProperties properties = new AionServicesProperties();
        properties.getChat().setEnabled(true);
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            new RecordingTransportBoundary(events),
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("chat", 200, true, events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments());

        assertEquals(List.of("prepare", "start:login", "start:chat", "start:game"), events);
    }

    @Test
    void stopsAServiceWhoseStartupFailed() throws Exception {
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            new AionServicesProperties(),
            new RecordingTransportBoundary(events),
            List.of(new FailingLifecycle(events))
        );

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> launcher.run(new DefaultApplicationArguments())
        );
        assertTrue(thrown.getMessage().contains("boom"));

        assertEquals(List.of("prepare", "start:failing", "stop:failing", "stop:transport"), events);
    }

    @Test
    void stopsTransportBoundaryWhenTransportPreparationFailed() {
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            new AionServicesProperties(),
            new FailingTransportBoundary(events),
            List.of(new RecordingLifecycle("login", 100, true, events))
        );

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> launcher.run(new DefaultApplicationArguments())
        );
        assertTrue(thrown.getMessage().contains("transport failed"));

        assertEquals(List.of("prepare", "stop:transport"), events);
    }

    @Test
    void embeddedFailureStopsStartedServicesInReverseOrder() throws Exception {
        AionServicesProperties properties = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            new RecordingTransportBoundary(events),
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("chat", 200, false, events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments());
        AionEmbeddedFailureHandler.fail(new IllegalStateException("auth failed"));

        assertEquals(List.of("prepare", "start:login", "start:game", "stop:game", "stop:login", "stop:transport"), events);
    }

    @Test
    void embeddedShutdownRequestStopsStartedServicesInReverseOrder() throws Exception {
        AionServicesProperties properties = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            new RecordingTransportBoundary(events),
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("chat", 200, false, events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments());
        assertTrue(AionEmbeddedShutdownHandler.requestShutdown());

        assertEquals(List.of("prepare", "start:login", "start:game", "stop:game", "stop:login", "stop:transport"), events);
    }

    @Test
    void destroyStopsServicesAndTransportOnlyOnceWhenCalledRepeatedly() throws Exception {
        AionServicesProperties properties = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            new RecordingTransportBoundary(events),
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments());
        launcher.destroy();
        launcher.destroy();

        assertEquals(List.of("prepare", "start:login", "start:game", "stop:game", "stop:login", "stop:transport"), events);
    }

    private static final class RecordingTransportBoundary extends AionTransportBoundary {
        private final List<String> events;

        private RecordingTransportBoundary(List<String> events) {
            super(new AionServicesProperties(), null, null);
            this.events = events;
        }

        @Override
        public void prepare() {
            events.add("prepare");
        }

        @Override
        public void destroy() {
            events.add("stop:transport");
        }
    }

    private static final class FailingTransportBoundary extends AionTransportBoundary {
        private final List<String> events;

        private FailingTransportBoundary(List<String> events) {
            super(new AionServicesProperties(), null, null);
            this.events = events;
        }

        @Override
        public void prepare() {
            events.add("prepare");
            throw new IllegalStateException("transport failed");
        }

        @Override
        public void destroy() {
            events.add("stop:transport");
        }
    }

    private static class RecordingLifecycle implements AionServiceLifecycle {
        private final String name;
        private final int phase;
        private final boolean enabled;
        private final List<String> events;

        private RecordingLifecycle(String name, int phase, boolean enabled, List<String> events) {
            this.name = name;
            this.phase = phase;
            this.enabled = enabled;
            this.events = events;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getPhase() {
            return phase;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void start(ApplicationArguments args) {
            events.add("start:" + name);
        }

        @Override
        public void stop() {
            events.add("stop:" + name);
        }
    }

    private static final class FailingLifecycle extends RecordingLifecycle {
        private FailingLifecycle(List<String> events) {
            super("failing", 100, true, events);
        }

        @Override
        public void start(ApplicationArguments args) {
            super.start(args);
            throw new IllegalStateException("boom");
        }
    }
}
