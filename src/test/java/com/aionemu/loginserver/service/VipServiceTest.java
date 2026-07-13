package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.loginserver.configs.VipConfig;
import com.aionemu.loginserver.dao.VipDAO;
import com.aionemu.loginserver.model.Vip;

class VipServiceTest {

    private final boolean originalAutoEnable = VipConfig.AUTO_ENABLE;
    private final int originalLevel = VipConfig.AUTO_ENABLE_LEVEL;
    private final boolean originalStsEnable = VipConfig.STS_ENABLE;
    private final int originalStsPort = VipConfig.STS_PORT;
    private final long originalStsDefaultScore = VipConfig.STS_DEFAULT_SCORE;
    private final String originalStsAppGroup = VipConfig.STS_APP_GROUP;

    @AfterEach
    void restoreConfig() {
        VipConfig.AUTO_ENABLE = originalAutoEnable;
        VipConfig.AUTO_ENABLE_LEVEL = originalLevel;
        VipConfig.STS_ENABLE = originalStsEnable;
        VipConfig.STS_PORT = originalStsPort;
        VipConfig.STS_DEFAULT_SCORE = originalStsDefaultScore;
        VipConfig.STS_APP_GROUP = originalStsAppGroup;
    }

    @Test
    void disabledAutoEnableDoesNotWrite() {
        FakeVipDAO dao = new FakeVipDAO();
        VipConfig.AUTO_ENABLE = false;

        VipService service = new VipService(dao);

        assertEquals(0, service.syncMissingAccounts());
        assertFalse(service.insertIfAbsent(42));
        assertEquals(0, dao.writeCalls);
    }

    @Test
    void enabledAutoEnableUsesConfiguredLevel() {
        FakeVipDAO dao = new FakeVipDAO();
        VipConfig.AUTO_ENABLE = true;
        VipConfig.AUTO_ENABLE_LEVEL = 4;

        VipService service = new VipService(dao);

        assertEquals(3, service.syncMissingAccounts());
        assertTrue(service.insertIfAbsent(42));
        assertEquals(4, dao.lastLevel);
        assertEquals(42, dao.lastAccountId);
    }

    @Test
    void enabledAutoEnableRejectsInvalidLevel() {
        VipConfig.AUTO_ENABLE = true;
        VipConfig.AUTO_ENABLE_LEVEL = 7;

        assertThrows(IllegalArgumentException.class, VipConfig::validate);
    }

    @Test
    void enabledStsRejectsInvalidPort() {
        VipConfig.STS_ENABLE = true;
        VipConfig.STS_PORT = 0;
        VipConfig.STS_APP_GROUP = "AION";
        VipConfig.STS_DEFAULT_SCORE = 0;

        assertThrows(IllegalArgumentException.class, VipConfig::validate);
    }

    @Test
    void synchronizationFailureIsNotHiddenAsZeroChanges() {
        FakeVipDAO dao = new FakeVipDAO();
        dao.failSync = true;
        VipConfig.AUTO_ENABLE = true;
        VipConfig.AUTO_ENABLE_LEVEL = 1;

        assertThrows(IllegalStateException.class, () -> new VipService(dao).syncMissingAccounts());
    }

    private static final class FakeVipDAO extends VipDAO {

        private int writeCalls;
        private int lastAccountId;
        private int lastLevel;
        private boolean failSync;

        @Override
        public Vip findByAccountId(int accountId) {
            return null;
        }

        @Override
        public int syncMissingAccounts(int level) {
            if (failSync) {
                throw new IllegalStateException("database unavailable");
            }
            writeCalls++;
            lastLevel = level;
            return 3;
        }

        @Override
        public boolean insertIfAbsent(int accountId, int level) {
            writeCalls++;
            lastAccountId = accountId;
            lastLevel = level;
            return true;
        }

        @Override
        public boolean supports(String database, int majorVersion, int minorVersion) {
            return true;
        }
    }
}
