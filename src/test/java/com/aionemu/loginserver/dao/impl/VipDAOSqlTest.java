package com.aionemu.loginserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VipDAOSqlTest {

    @Test
    void synchronizationAndInsertionNeverOverwriteExistingVipData() {
        assertTrue(VipDAO.SYNC_QUERY.startsWith("INSERT IGNORE"));
        assertTrue(VipDAO.INSERT_QUERY.startsWith("INSERT IGNORE"));
        assertTrue(VipDAO.FIND_QUERY.contains("expire_time"));
        assertTrue(VipDAO.SYNC_QUERY.contains("expire_time"));
        assertFalse(VipDAO.SYNC_QUERY.contains("UPDATE"));
        assertFalse(VipDAO.INSERT_QUERY.contains("UPDATE"));
    }
}
