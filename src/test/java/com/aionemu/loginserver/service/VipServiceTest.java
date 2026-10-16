package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.testutil.ConfigSnapshot;
import com.aionemu.loginserver.configs.VipConfig;
import com.aionemu.loginserver.persistence.dao.AccountVipDao;
import com.aionemu.loginserver.model.Vip;

class VipServiceTest {

    private final ConfigSnapshot vipConfigSnapshot = ConfigSnapshot.of(VipConfig.class,
        "AUTO_ENABLE", "AUTO_ENABLE_LEVEL", "AUTO_ENABLE_DAYS");

    @AfterEach
    void restoreConfig() {
        vipConfigSnapshot.restore();
    }

    @Test
    void disabledAutoEnableDoesNotWrite() {
        FakeVipState state = new FakeVipState();
        VipConfig.AUTO_ENABLE = false;

        VipService service = new VipService(fakeDao(state));

        assertEquals(0, service.grantToAllAccounts());
        assertFalse(service.insertIfAbsent(42));
        assertEquals(0, state.writeCalls);
    }

    @Test
    void enabledAutoEnableUsesConfiguredLevelAndDays() {
        FakeVipState state = new FakeVipState();
        VipConfig.AUTO_ENABLE = true;
        VipConfig.AUTO_ENABLE_LEVEL = 6;
        VipConfig.AUTO_ENABLE_DAYS = 365;

        VipService service = new VipService(fakeDao(state));

        assertEquals(3, service.grantToAllAccounts());
        assertTrue(service.insertIfAbsent(42));
        assertEquals(6, state.lastLevel);
        assertEquals(365, state.lastDays);
        assertEquals(42, state.lastAccountId);
    }

    @Test
    void enabledAutoEnableRejectsInvalidLevel() {
        VipConfig.AUTO_ENABLE = true;
        VipConfig.AUTO_ENABLE_LEVEL = 7;

        assertThrows(IllegalArgumentException.class, VipConfig::validate);
    }

    @Test
    void enabledAutoEnableRejectsInvalidDays() {
        VipConfig.AUTO_ENABLE = true;
        VipConfig.AUTO_ENABLE_LEVEL = 6;
        VipConfig.AUTO_ENABLE_DAYS = 0;

        assertThrows(IllegalArgumentException.class, VipConfig::validate);
    }

    @Test
    void vipExpiryAllowsPermanentAndRejectsExpiredRows() {
        assertTrue(new Vip(1, 5, 0, 0).isActive(100));
        assertTrue(new Vip(1, 5, 0, 101).isActive(100));
        assertFalse(new Vip(1, 5, 0, 100).isActive(100));
    }

    @Test
    void synchronizationFailureIsNotHiddenAsZeroChanges() {
        FakeVipState state = new FakeVipState();
        state.failSync = true;
        VipConfig.AUTO_ENABLE = true;
        VipConfig.AUTO_ENABLE_LEVEL = 1;

        AccountVipDao dao = fakeDao(state);
        assertThrows(IllegalStateException.class, () -> new VipService(dao).grantToAllAccounts());
    }

    /**
     * 仅实现用例所需方法的动态代理 DAO 桩。
     * Dynamic-proxy DAO stub implementing only the methods these cases need.
     */
    private static AccountVipDao fakeDao(FakeVipState state) {
        return (AccountVipDao) Proxy.newProxyInstance(AccountVipDao.class.getClassLoader(),
            new Class<?>[] { AccountVipDao.class }, (proxy, method, args) -> switch (method.getName()) {
                case "findByAccountId" -> null;
                case "grantToAllAccounts" -> {
                    if (state.failSync) {
                        throw new IllegalStateException("database unavailable");
                    }
                    state.writeCalls++;
                    state.lastLevel = (int) args[0];
                    state.lastDays = (int) args[1];
                    yield 3;
                }
                case "insertIfAbsent" -> {
                    state.writeCalls++;
                    state.lastAccountId = (int) args[0];
                    state.lastLevel = (int) args[1];
                    state.lastDays = (int) args[2];
                    yield true;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private static final class FakeVipState {

        private int writeCalls;
        private int lastAccountId;
        private int lastLevel;
        private int lastDays;
        private boolean failSync;
    }
}
