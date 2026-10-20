package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.aionemu.boot.i18n.I18n;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import static org.junit.jupiter.api.Assertions.*;

class GameSystemLifecycleTest {

    @Test
    void startRunsInitializersLogsSystemInfoAndRecordsStartupTime() {
        List<String> events = new ArrayList<>();
        GameSystemLifecycle lifecycle = new RecordingGameSystemLifecycle(events, null, legacyEventNames(), 2);

        long startupTime = lifecycle.start(8_000);
        long repeatedStartupTime = lifecycle.start(8_000);

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, startupTime);
        assertEquals(2, repeatedStartupTime);
        assertEquals(2, lifecycle.getStartupTimeSeconds());
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
        assertEquals(List.of(
            "section:system",
            "versions",
            "infos",
            "memory:512:128:384:1024",
            "startup:2"
        ), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("system failed");
        GameSystemLifecycle lifecycle = new RecordingGameSystemLifecycle(
            events,
            failure,
            List.of(
                "section:system",
                "versions",
                "memory:512:128:384:1024",
                "startup:2"
            ),
            2
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lifecycle.start(8_000));

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());
        assertEquals(-1, lifecycle.getStartupTimeSeconds());

        long startupTime = lifecycle.start(8_000);

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, startupTime);
        assertEquals(List.of(
            "section:system",
            "versions",
            "section:system",
            "versions",
            "memory:512:128:384:1024",
            "startup:2"
        ), events);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void startupBannerOmitsRepositoryUrl() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("console.startup.successful", Locale.ENGLISH, "Startup successful");
        source.addMessage("console.startup.open_source_notice", Locale.ENGLISH, "Welcome to AionEmu.");
        source.addMessage(
            "console.startup.open_source_license",
            Locale.ENGLISH,
            "License: GNU GPL-3.0 (see the LICENSE file bundled with this project)"
        );
        I18n.setMessageSource(source);
        I18n.applyCountryCode(1);
        try {
            List<String> lines = GameSystemLifecycle.bannerLines();

            assertTrue(lines.contains("  Startup successful"));
            assertTrue(lines.contains("  Welcome to AionEmu."));
            assertTrue(lines.contains(
                "  License: GNU GPL-3.0 (see the LICENSE file bundled with this project)"
            ));
            assertTrue(lines.stream().noneMatch(line -> line.contains("github.com/AionEmu/AionEmu")));
            assertTrue(lines.stream().noneMatch(line -> line.contains("Project:")));
        } finally {
            I18n.setMessageSource(null);
            I18n.applyCountryCode(1);
        }
    }

    private static List<String> legacyEventNames() {
        return List.of(
            "section:system",
            "versions",
            "infos",
            "memory:512:128:384:1024",
            "startup:2"
        );
    }

    private static final class RecordingGameSystemLifecycle extends GameSystemLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private final List<String> eventNames;
        private final long startupTimeSeconds;
        private boolean failed;

        private RecordingGameSystemLifecycle(
            List<String> events,
            RuntimeException firstFailure,
            List<String> eventNames,
            long startupTimeSeconds
        ) {
            this.events = events;
            this.firstFailure = firstFailure;
            this.eventNames = List.copyOf(eventNames);
            this.startupTimeSeconds = startupTimeSeconds;
        }

        @Override
        protected long printSystemFinalization(long serverStartTimeMillis) {
            for (String eventName : eventNames) {
                events.add(eventName);
                if (events.size() == 2 && firstFailure != null && !failed) {
                    failed = true;
                    throw firstFailure;
                }
            }
            return startupTimeSeconds;
        }
    }

}
