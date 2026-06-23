package com.aionemu.commons.database.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.commons.services.ServiceContext;
import org.junit.jupiter.api.Test;

class DAOManagerTest {

    @Test
    void isInitializedIsFalseBeforeContextDaoManagerStarts() {
        try (ServiceContext.Scope ignored = ServiceContext.use("dao-test")) {
            assertFalse(DAOManager.isInitialized());

            DAOManager.shutdown();

            assertFalse(DAOManager.isInitialized());
        }
    }
}
