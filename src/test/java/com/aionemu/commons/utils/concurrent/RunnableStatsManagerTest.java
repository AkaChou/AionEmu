package com.aionemu.commons.utils.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.commons.configs.CommonsConfig;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RunnableStatsManagerTest {

    @Test
    void disabledStatsAreNeitherCollectedNorDumped() throws ReflectiveOperationException, IOException {
        boolean oldValue = CommonsConfig.RUNNABLESTATS_ENABLE;
        long logsBefore = countMethodStatsLogs();
        try {
            CommonsConfig.RUNNABLESTATS_ENABLE = false;
            RunnableStatsManager.handleStats(TestRunnable.class, 1);
            RunnableStatsManager.handleStats(MethodOwner.class, "test()", 1);
            RunnableStatsManager.dumpClassStats();

            assertFalse(classStats().containsKey(TestRunnable.class));
            assertFalse(classStats().containsKey(MethodOwner.class));
            assertEquals(logsBefore, countMethodStatsLogs());
        } finally {
            CommonsConfig.RUNNABLESTATS_ENABLE = oldValue;
        }
    }

    private long countMethodStatsLogs() throws IOException {
        try (Stream<Path> paths = Files.list(Path.of("."))) {
            return paths.filter(path -> path.getFileName().toString().matches("MethodStats-\\d+\\.log")).count();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Class<?>, ?> classStats() throws ReflectiveOperationException {
        Field field = RunnableStatsManager.class.getDeclaredField("classStats");
        field.setAccessible(true);
        return (Map<Class<?>, ?>) field.get(null);
    }

    private static final class TestRunnable implements Runnable {
        @Override
        public void run() {
        }
    }

    private static final class MethodOwner {
    }
}
