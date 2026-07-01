package com.aionemu.gameserver.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BannedMacManagerTest {

    @Test
    void dbLoadedFutureBanIsActiveAndExpiredBanIsIgnored() {
        BannedMacManager manager = new BannedMacManager();
        long now = System.currentTimeMillis();

        manager.dbLoad("00:11:22:33:44:55", now + 60_000, "active");
        manager.dbLoad("00:11:22:33:44:66", now - 60_000, "expired");

        assertTrue(manager.isBanned("00:11:22:33:44:55"));
        assertFalse(manager.isBanned("00:11:22:33:44:66"));
        assertFalse(manager.isBanned("00:11:22:33:44:77"));
    }

    @Test
    void unbanUnknownAddressReturnsFalse() {
        BannedMacManager manager = new BannedMacManager();

        assertFalse(manager.unbanAddress("00:11:22:33:44:88", "test"));
    }
}
