package com.aionemu.loginserver.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.configs.Config;
import org.junit.jupiter.api.Test;

class FloodProtectorTest {

    @Test
    void blocksRepeatedConnectionsUntilTemporaryBanExpires() {
        int fastReconnectionTime = Config.FAST_RECONNECTION_TIME;
        int wrongLoginBanTime = Config.WRONG_LOGIN_BAN_TIME;
        String excludedIp = Config.EXCLUDED_IP;
        Config.FAST_RECONNECTION_TIME = 60;
        Config.WRONG_LOGIN_BAN_TIME = 1;
        Config.EXCLUDED_IP = "";
        try {
            FloodProtector protector = new FloodProtector();

            assertFalse(protector.tooFast("192.0.2.11"));
            assertTrue(protector.tooFast("192.0.2.11"));
            assertTrue(protector.tooFast("192.0.2.11"));
        } finally {
            Config.FAST_RECONNECTION_TIME = fastReconnectionTime;
            Config.WRONG_LOGIN_BAN_TIME = wrongLoginBanTime;
            Config.EXCLUDED_IP = excludedIp;
        }
    }

    @Test
    void ignoresConfiguredExcludedIpAddresses() {
        String excludedIp = Config.EXCLUDED_IP;
        Config.EXCLUDED_IP = "192.0.2.12";
        try {
            FloodProtector protector = new FloodProtector();

            assertFalse(protector.tooFast("192.0.2.12"));
            assertFalse(protector.tooFast("192.0.2.12"));
        } finally {
            Config.EXCLUDED_IP = excludedIp;
        }
    }
}
