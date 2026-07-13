package com.aionemu.gameserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LimitedQuestDAOSqlTest {

	@Test
	void acquisitionOnlyReportsRowsThatWereActuallyDecremented() {
		assertTrue(LimitedQuestDAO.ACQUIRE_QUERY.contains("SET `remaining` = `remaining` - 1"));
		assertTrue(LimitedQuestDAO.ACQUIRE_QUERY.contains("AND `remaining` > 0"));
		assertFalse(LimitedQuestDAO.ACQUIRE_QUERY.contains("IF("));
	}

	@Test
	void recoveryIsCappedInsideTheDatabaseStatement() {
		assertTrue(LimitedQuestDAO.RECOVER_QUERY.contains("LEAST(`remaining` + ?, ?)"));
	}

	@Test
	void initializationDoesNotOverwritePersistedRemainingCount() {
		assertTrue(LimitedQuestDAO.INITIALIZE_QUERY.startsWith("INSERT IGNORE"));
	}
}
