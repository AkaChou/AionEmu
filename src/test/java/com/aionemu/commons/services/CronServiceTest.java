package com.aionemu.commons.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.services.cron.RunnableRunner;
import org.junit.jupiter.api.Test;

class CronServiceTest {

    @Test
    void isInitializedIsFalseBeforeContextCronServiceStarts() {
        try (ServiceContext.Scope ignored = ServiceContext.use("cron-test")) {
            assertFalse(CronService.isInitialized());
        }
    }

    @Test
    void canReinitializeSameServiceContextAfterShutdown() {
        String context = "cron-restart-test-" + System.nanoTime();
        CronService first;
        CronService second;

        try (ServiceContext.Scope ignored = ServiceContext.use(context)) {
            CronService.initSingleton(TestRunnableRunner.class);
            assertTrue(CronService.isInitialized());
            first = CronService.getInstance();

            first.shutdown();

            assertFalse(CronService.isInitialized());

            CronService.initSingleton(TestRunnableRunner.class);
            second = CronService.getInstance();
            second.shutdown();
        }

        assertNotSame(first, second);
    }

    public static final class TestRunnableRunner extends RunnableRunner {
        @Override
        public void executeRunnable(Runnable r) {
            r.run();
        }

        @Override
        public void executeLongRunningRunnable(Runnable r) {
            r.run();
        }
    }
}
