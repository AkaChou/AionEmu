package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.GameServer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameWorldActivationLifecycleTest {

    @Test
    void usesWorldActivationGatewayCollaborator() {
        assertEquals(GameWorldActivationGateway.class, fieldType("worldActivationGateway"));
    }

    @Test
    void worldActivationGatewayBridgesDropRegistrationThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameWorldActivationGateway.class, "dropRegistrationServiceProvider"));
    }

    @Test
    void startRunsDropRegistrationServerActivationAndOfflineMarkerOnce() {
        List<String> events = new ArrayList<>();
        GameServer server = new GameServer();
        GameWorldActivationLifecycle lifecycle = new GameWorldActivationLifecycle(
            new RecordingGameWorldActivationGateway(events, server, null));

        GameServer firstServer = lifecycle.start();
        GameServer secondServer = lifecycle.start();

        assertSame(server, firstServer);
        assertSame(server, secondServer);
        assertTrue(lifecycle.isActivated());
        assertEquals(List.of("dropRegistration", "activeServer", "playersOffline"), events);
        assertTrue(lifecycle.getActivationTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("active server failed");
        GameServer server = new GameServer();
        GameWorldActivationLifecycle lifecycle = new GameWorldActivationLifecycle(
            new RecordingGameWorldActivationGateway(events, server, failure));

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            lifecycle::start
        );

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isActivated());

        GameServer activatedServer = lifecycle.start();

        assertSame(server, activatedServer);
        assertTrue(lifecycle.isActivated());
        assertEquals(
            List.of("dropRegistration", "activeServer", "dropRegistration", "activeServer", "playersOffline"),
            events
        );
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameWorldActivationLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameWorldActivationGateway extends GameWorldActivationGateway {

        private final List<String> events;
        private final GameServer server;
        private final RuntimeException firstFailure;

        private RecordingGameWorldActivationGateway(
            List<String> events,
            GameServer server,
            RuntimeException firstFailure
        ) {
            this.events = events;
            this.server = server;
            this.firstFailure = firstFailure;
        }

        @Override
        public GameServer activate() {
            events.add("dropRegistration");
            events.add("activeServer");
            if (events.size() == 2 && firstFailure != null) {
                throw firstFailure;
            }
            events.add("playersOffline");
            return server;
        }
    }
}
