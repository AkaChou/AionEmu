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
        "AUTO_ENABLE", "AUTO_ENABLE_LEVEL");

    @AfterEach
    void restoreConfig() {
        vipConfigSnapshot.restore();
    }

    @Test
    void disabledAutoEnableDoesNotWrite() {
        FakeVipState state = new FakeVipState();
        VipConfig.AUTO_ENABLE = false;

        VipService service = new VipService(fakeDao(state));

        assertEquals(0, service.syncMissingAccounts());
        assertFalse(service.insertIfAbsent(42));
        assertEquals(0, state.writeCalls);
    }

    @Test
    void enabledAutoEnableUsesConfiguredLevel() {
        FakeVipState state = new FakeVipState();
        VipConfig.AUTO_ENABLE = true;
        VipConfig.AUTO_ENABLE_LEVEL = 4;

        VipService service = new VipService(fakeDao(state));

        assertEquals(3, service.syncMissingAccounts());
        assertTrue(service.insertIfAbsent(42));
        assertEquals(4, state.lastLevel);
        assertEquals(42, state.lastAccountId);
    }

    @Test
    void enabledAutoEnableRejectsInvalidLevel() {
        VipConfig.AUTO_ENABLE = true;
        VipConfig.AUTO_ENABLE_LEVEL = 7;

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
        assertThrows(IllegalStateException.class, () -> new VipService(dao).syncMissingAccounts());
    }

    /**
     * 仅实现用例所需方法的动态代理 DAO 桩。
     * Dynamic-proxy DAO stub implementing only the methods these cases need.
     */
    private static AccountVipDao fakeDao(FakeVipState state) {
        return (AccountVipDao) Proxy.newProxyInstance(AccountVipDao.class.getClassLoader(),
            new Class<?>[] { AccountVipDao.class }, (proxy, method, args) -> switch (method.getName()) {
                case "findByAccountId" -> null;
                case "syncMissingAccounts" -> {
                    if (state.failSync) {
                        throw new IllegalStateException("database unavailable");
                    }
                    state.writeCalls++;
                    state.lastLevel = (int) args[0];
                    yield 3;
                }
                case "insertIfAbsent" -> {
                    state.writeCalls++;
                    state.lastAccountId = (int) args[0];
                    state.lastLevel = (int) args[1];
                    yield true;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private static final class FakeVipState {

        private int writeCalls;
        private int lastAccountId;
        private int lastLevel;
        private boolean failSync;
    }
}
