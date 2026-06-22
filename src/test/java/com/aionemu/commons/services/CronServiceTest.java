package com.aionemu.commons.services;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class CronServiceTest {

    @Test
    void isInitializedIsFalseBeforeContextCronServiceStarts() {
        try (ServiceContext.Scope ignored = ServiceContext.use("cron-test")) {
            assertFalse(CronService.isInitialized());
        }
    }
}
