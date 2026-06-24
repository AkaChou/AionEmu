package com.aionemu.gameserver.dao.mysql8;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class MySQL8PlayerEquipmentSettingDAOTest {

	@Test
	void insertQueryIncludesRequiredNameColumn() {
		assertTrue(MySQL8PlayerEquipmentSettingDAO.INSERT_QUERY.contains("`name`"));
	}

	@Test
	void duplicateUpdateDoesNotBlankExistingName() {
		String updateClause = MySQL8PlayerEquipmentSettingDAO.INSERT_QUERY.substring(
				MySQL8PlayerEquipmentSettingDAO.INSERT_QUERY.indexOf("ON DUPLICATE KEY UPDATE"));

		assertFalse(updateClause.contains("`name`"));
	}
}
