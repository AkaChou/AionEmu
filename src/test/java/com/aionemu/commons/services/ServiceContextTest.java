package com.aionemu.commons.services;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.commons.services.cron.RunnableRunner;
import com.aionemu.commons.utils.concurrent.RunnableWrapper;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ServiceContextTest {

    @Test
    void runnableWrapperRestoresCapturedServiceContext() {
        AtomicReference<String> observedContext = new AtomicReference<>();
        RunnableWrapper wrapper;

        try (ServiceContext.Scope ignored = ServiceContext.use("login")) {
            wrapper = new RunnableWrapper(new Runnable() {
                @Override
                public void run() {
                    observedContext.set(ServiceContext.current());
                }
            });
        }

        try (ServiceContext.Scope ignored = ServiceContext.use("game")) {
            wrapper.run();
        }

        assertEquals("login", observedContext.get());
    }

    @Test
    void cronServiceIsIsolatedByServiceContext() {
        String firstContext = "cron-test-a-" + System.nanoTime();
        String secondContext = "cron-test-b-" + System.nanoTime();
        CronService first;
        CronService second;

        try (ServiceContext.Scope ignored = ServiceContext.use(firstContext)) {
            CronService.initSingleton(TestRunnableRunner.class);
            first = CronService.getInstance();
        }

        try (ServiceContext.Scope ignored = ServiceContext.use(secondContext)) {
            CronService.initSingleton(TestRunnableRunner.class);
            second = CronService.getInstance();
        }

        assertNotSame(first, second);

        try (ServiceContext.Scope ignored = ServiceContext.use(firstContext)) {
            CronService.getInstance().shutdown();
        }
        try (ServiceContext.Scope ignored = ServiceContext.use(secondContext)) {
            CronService.getInstance().shutdown();
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
