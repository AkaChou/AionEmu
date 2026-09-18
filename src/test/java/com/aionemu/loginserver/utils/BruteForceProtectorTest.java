package com.aionemu.loginserver.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.configs.Config;
import com.aionemu.testutil.ConfigSnapshot;
import org.junit.jupiter.api.Test;

class BruteForceProtectorTest {

    @Test
    void bansAfterConfiguredFailedLoginThresholdAndThenResets() {
        ConfigSnapshot snapshot = ConfigSnapshot.of(Config.class,
            "LOGIN_TRY_BEFORE_BAN", "WRONG_LOGIN_BAN_TIME");
        try {
            Config.LOGIN_TRY_BEFORE_BAN = 2;
            Config.WRONG_LOGIN_BAN_TIME = 15;
            BruteForceProtector protector = new BruteForceProtector();

            assertFalse(protector.addFailedConnect("192.0.2.10"));
            assertFalse(protector.addFailedConnect("192.0.2.10"));
            assertTrue(protector.addFailedConnect("192.0.2.10"));
            assertFalse(protector.addFailedConnect("192.0.2.10"));
        } finally {
            snapshot.restore();
        }
    }
}
