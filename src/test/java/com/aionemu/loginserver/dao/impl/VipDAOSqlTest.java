package com.aionemu.loginserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VipDAOSqlTest {

    @Test
    void synchronizationAndInsertionNeverOverwriteExistingVipData() {
        assertTrue(VipDAO.SYNC_QUERY.startsWith("INSERT IGNORE"));
        assertTrue(VipDAO.INSERT_QUERY.startsWith("INSERT IGNORE"));
        assertFalse(VipDAO.SYNC_QUERY.contains("UPDATE"));
        assertFalse(VipDAO.INSERT_QUERY.contains("UPDATE"));
    }
}
