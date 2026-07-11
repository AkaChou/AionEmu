package com.aionemu.commons.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.services.cron.CronServiceException;
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
            first = CronService.initSingleton(TestRunnableRunner.class);
            assertTrue(CronService.isInitialized());
            assertSame(first, CronService.getInstance());

            first.shutdown();

            assertFalse(CronService.isInitialized());

            second = CronService.initSingleton(TestRunnableRunner.class);
            assertSame(second, CronService.getInstance());
            second.shutdown();
        }

        assertNotSame(first, second);
    }

    @Test
    void canShutdownCurrentServiceContextWithoutFetchingSingleton() {
        String context = "cron-shutdown-test-" + System.nanoTime();

        try (ServiceContext.Scope ignored = ServiceContext.use(context)) {
            assertFalse(CronService.shutdownCurrentIfInitialized());

            CronService.initSingleton(TestRunnableRunner.class);
            assertTrue(CronService.isInitialized());

            assertTrue(CronService.shutdownCurrentIfInitialized());
            assertFalse(CronService.isInitialized());
            assertFalse(CronService.shutdownCurrentIfInitialized());
        }
    }

    @Test
    void requireCurrentReturnsInitializedServiceOrThrows() {
        String context = "cron-current-test-" + System.nanoTime();

        try (ServiceContext.Scope ignored = ServiceContext.use(context)) {
            assertThrows(CronServiceException.class, CronService::requireCurrent);

            CronService cronService = CronService.initSingleton(TestRunnableRunner.class);

            assertSame(cronService, CronService.requireCurrent());
            cronService.shutdown();
        }
    }

    @Test
    void reloadsSupplierBackedTasksAndCancelsByOriginalRunnable() {
        String context = "cron-reload-test-" + System.nanoTime();

        try (ServiceContext.Scope ignored = ServiceContext.use(context)) {
            CronService cronService = CronService.initSingleton(TestRunnableRunner.class);
            Runnable task = () -> { };
            String[] expression = { "0 0 0 1 1 ? 2099" };
            cronService.schedule(task, () -> expression[0]);

            assertTrue(cronService.getRunnables().containsKey(task));
            expression[0] = "0 0 0 2 1 ? 2099";
            cronService.reload();
            assertTrue(cronService.getRunnables().containsKey(task));

            cronService.cancel(task);
            assertFalse(cronService.getRunnables().containsKey(task));
            cronService.shutdown();
        }
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
