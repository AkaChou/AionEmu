package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameRuntimeServicesLifecycleTest {

    @Test
    void startRunsInitializersOnceInLegacyOrderAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameRuntimeServicesLifecycle lifecycle = new GameRuntimeServicesLifecycle(
            () -> events.add("section"),
            initializers(events)
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "section",
            "periodicSave",
            "admin",
            "playerTransfer",
            "territory",
            "gameTime",
            "announcement",
            "debug",
            "weather",
            "broker",
            "influence",
            "exchange",
            "petition",
            "instance",
            "flyRing",
            "curingZone",
            "springZone",
            "boostEvent",
            "taskManager",
            "limitedItemTrade",
            "gameTimeClock"
        ), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("runtime services failed");
        GameRuntimeServicesLifecycle lifecycle = new GameRuntimeServicesLifecycle(() -> events.add("section"), List.<Runnable>of(
            () -> events.add("periodicSave"),
            () -> {
                events.add("admin");
                if (events.size() == 3) {
                    throw failure;
                }
            }
        ));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "periodicSave", "admin", "section", "periodicSave", "admin"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static List<Runnable> initializers(List<String> events) {
        return List.of(
            () -> events.add("periodicSave"),
            () -> events.add("admin"),
            () -> events.add("playerTransfer"),
            () -> events.add("territory"),
            () -> events.add("gameTime"),
            () -> events.add("announcement"),
            () -> events.add("debug"),
            () -> events.add("weather"),
            () -> events.add("broker"),
            () -> events.add("influence"),
            () -> events.add("exchange"),
            () -> events.add("petition"),
            () -> events.add("instance"),
            () -> events.add("flyRing"),
            () -> events.add("curingZone"),
            () -> events.add("springZone"),
            () -> events.add("boostEvent"),
            () -> events.add("taskManager"),
            () -> events.add("limitedItemTrade"),
            () -> events.add("gameTimeClock")
        );
    }
}
