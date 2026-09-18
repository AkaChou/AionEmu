package com.aionemu.loginserver.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.configs.Config;
import com.aionemu.testutil.ConfigSnapshot;
import org.junit.jupiter.api.Test;

class FloodProtectorTest {

    @Test
    void blocksRepeatedConnectionsUntilTemporaryBanExpires() {
        ConfigSnapshot snapshot = ConfigSnapshot.of(Config.class,
            "FAST_RECONNECTION_TIME", "WRONG_LOGIN_BAN_TIME", "EXCLUDED_IP");
        try {
            Config.FAST_RECONNECTION_TIME = 60;
            Config.WRONG_LOGIN_BAN_TIME = 1;
            Config.EXCLUDED_IP = "";
            FloodProtector protector = new FloodProtector();

            assertFalse(protector.tooFast("192.0.2.11"));
            assertTrue(protector.tooFast("192.0.2.11"));
            assertTrue(protector.tooFast("192.0.2.11"));
        } finally {
            snapshot.restore();
        }
    }

    @Test
    void ignoresConfiguredExcludedIpAddresses() {
        ConfigSnapshot snapshot = ConfigSnapshot.of(Config.class, "EXCLUDED_IP");
        try {
            Config.EXCLUDED_IP = "192.0.2.12";
            FloodProtector protector = new FloodProtector();

            assertFalse(protector.tooFast("192.0.2.12"));
            assertFalse(protector.tooFast("192.0.2.12"));
        } finally {
            snapshot.restore();
        }
    }
}
