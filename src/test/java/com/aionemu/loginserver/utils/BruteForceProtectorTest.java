package com.aionemu.loginserver.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.configs.Config;
import org.junit.jupiter.api.Test;

class BruteForceProtectorTest {

    @Test
    void bansAfterConfiguredFailedLoginThresholdAndThenResets() {
        int loginTryBeforeBan = Config.LOGIN_TRY_BEFORE_BAN;
        int wrongLoginBanTime = Config.WRONG_LOGIN_BAN_TIME;
        Config.LOGIN_TRY_BEFORE_BAN = 2;
        Config.WRONG_LOGIN_BAN_TIME = 15;
        try {
            BruteForceProtector protector = new BruteForceProtector();

            assertFalse(protector.addFailedConnect("192.0.2.10"));
            assertFalse(protector.addFailedConnect("192.0.2.10"));
            assertTrue(protector.addFailedConnect("192.0.2.10"));
            assertFalse(protector.addFailedConnect("192.0.2.10"));
        } finally {
            Config.LOGIN_TRY_BEFORE_BAN = loginTryBeforeBan;
            Config.WRONG_LOGIN_BAN_TIME = wrongLoginBanTime;
        }
    }
}
