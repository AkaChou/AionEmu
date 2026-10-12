package com.aionemu.loginserver.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.model.base.BannedMacEntry;
import com.aionemu.loginserver.persistence.dao.BannedMacDao;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class BannedMacManagerTest {

    @Test
    void banAddsEntryToMapAndPersistsIt() throws Exception {
        AtomicReference<BannedMacEntry> updatedEntry = new AtomicReference<>();
        Map<String, BannedMacEntry> bannedList = new HashMap<>();
        BannedMacManager manager = manager(fakeDao(updatedEntry, null), bannedList);
        long banTime = System.currentTimeMillis() + 60_000;

        manager.ban("00:11:22:33:44:55", banTime, "test");

        BannedMacEntry entry = bannedList.get("00:11:22:33:44:55");
        assertEquals("00:11:22:33:44:55", entry.getMac());
        assertEquals(banTime, entry.getTime().getTime());
        assertEquals("test", entry.getDetails());
        assertSame(entry, updatedEntry.get());
    }

    @Test
    void unbanRemovesKnownEntryAndPersistsRemoval() throws Exception {
        AtomicReference<String> removedAddress = new AtomicReference<>();
        Map<String, BannedMacEntry> bannedList = new HashMap<>();
        bannedList.put("00:11:22:33:44:66", new BannedMacEntry("00:11:22:33:44:66", System.currentTimeMillis() + 60_000));
        BannedMacManager manager = manager(fakeDao(null, removedAddress), bannedList);

        manager.unban("00:11:22:33:44:66", "test");

        assertFalse(bannedList.containsKey("00:11:22:33:44:66"));
        assertEquals("00:11:22:33:44:66", removedAddress.get());
    }

    @Test
    void unbanIgnoresUnknownEntry() throws Exception {
        AtomicReference<String> removedAddress = new AtomicReference<>();
        Map<String, BannedMacEntry> bannedList = new HashMap<>();
        BannedMacManager manager = manager(fakeDao(null, removedAddress), bannedList);

        manager.unban("00:11:22:33:44:77", "test");

        assertTrue(bannedList.isEmpty());
        assertNull(removedAddress.get());
    }

    private static BannedMacManager manager(BannedMacDao dao, Map<String, BannedMacEntry> bannedList) throws Exception {
        BannedMacManager manager = new ObjenesisStd().newInstance(BannedMacManager.class);
        setField(manager, "bannedMacDao", dao);
        setField(manager, "bannedList", bannedList);
        return manager;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 仅实现用例所需方法的动态代理 DAO 桩。
     * Dynamic-proxy DAO stub implementing only the methods these cases need.
     */
    private static BannedMacDao fakeDao(AtomicReference<BannedMacEntry> updatedEntry, AtomicReference<String> removedAddress) {
        return (BannedMacDao) Proxy.newProxyInstance(BannedMacDao.class.getClassLoader(),
            new Class<?>[] { BannedMacDao.class }, (proxy, method, args) -> switch (method.getName()) {
                case "update" -> {
                    if (updatedEntry != null) {
                        updatedEntry.set((BannedMacEntry) args[0]);
                    }
                    yield true;
                }
                case "remove" -> {
                    if (removedAddress != null) {
                        removedAddress.set((String) args[0]);
                    }
                    yield true;
                }
                case "load" -> new HashMap<String, BannedMacEntry>();
                case "cleanExpiredBans" -> null;
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }
}
