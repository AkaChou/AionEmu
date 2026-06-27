package com.aionemu.loginserver.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.dao.BannedMacDAO;
import com.aionemu.loginserver.model.base.BannedMacEntry;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class BannedMacManagerTest {

    @Test
    void banAddsEntryToMapAndPersistsIt() throws Exception {
        FakeBannedMacDAO dao = new FakeBannedMacDAO();
        Map<String, BannedMacEntry> bannedList = new HashMap<>();
        BannedMacManager manager = manager(dao, bannedList);
        long banTime = System.currentTimeMillis() + 60_000;

        manager.ban("00:11:22:33:44:55", banTime, "test");

        BannedMacEntry entry = bannedList.get("00:11:22:33:44:55");
        assertEquals("00:11:22:33:44:55", entry.getMac());
        assertEquals(banTime, entry.getTime().getTime());
        assertEquals("test", entry.getDetails());
        assertSame(entry, dao.updatedEntry);
    }

    @Test
    void unbanRemovesKnownEntryAndPersistsRemoval() throws Exception {
        FakeBannedMacDAO dao = new FakeBannedMacDAO();
        Map<String, BannedMacEntry> bannedList = new HashMap<>();
        bannedList.put("00:11:22:33:44:66", new BannedMacEntry("00:11:22:33:44:66", System.currentTimeMillis() + 60_000));
        BannedMacManager manager = manager(dao, bannedList);

        manager.unban("00:11:22:33:44:66", "test");

        assertFalse(bannedList.containsKey("00:11:22:33:44:66"));
        assertEquals("00:11:22:33:44:66", dao.removedAddress);
    }

    @Test
    void unbanIgnoresUnknownEntry() throws Exception {
        FakeBannedMacDAO dao = new FakeBannedMacDAO();
        Map<String, BannedMacEntry> bannedList = new HashMap<>();
        BannedMacManager manager = manager(dao, bannedList);

        manager.unban("00:11:22:33:44:77", "test");

        assertTrue(bannedList.isEmpty());
        assertNull(dao.removedAddress);
    }

    private static BannedMacManager manager(BannedMacDAO dao, Map<String, BannedMacEntry> bannedList) throws Exception {
        BannedMacManager manager = new ObjenesisStd().newInstance(BannedMacManager.class);
        setField(manager, "dao", dao);
        setField(manager, "bannedList", bannedList);
        return manager;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class FakeBannedMacDAO extends BannedMacDAO {

        private BannedMacEntry updatedEntry;
        private String removedAddress;

        @Override
        public boolean update(BannedMacEntry entry) {
            updatedEntry = entry;
            return true;
        }

        @Override
        public boolean remove(String address) {
            removedAddress = address;
            return true;
        }

        @Override
        public Map<String, BannedMacEntry> load() {
            return new HashMap<>();
        }

        @Override
        public void cleanExpiredBans() {
        }

        @Override
        public boolean supports(String databaseName, int majorVersion, int minorVersion) {
            return true;
        }
    }
}
