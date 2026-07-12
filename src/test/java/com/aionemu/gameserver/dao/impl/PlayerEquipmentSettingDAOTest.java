package com.aionemu.gameserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerEquipmentSettingDAOTest {

	@Test
	void insertQueryIncludesRequiredNameColumn() {
		assertTrue(PlayerEquipmentSettingDAO.INSERT_QUERY.contains("`name`"));
	}

	@Test
	void duplicateUpdateDoesNotBlankExistingName() {
		String updateClause = PlayerEquipmentSettingDAO.INSERT_QUERY.substring(
				PlayerEquipmentSettingDAO.INSERT_QUERY.indexOf("ON DUPLICATE KEY UPDATE"));

		assertTrue(updateClause.contains("`name` = IF(`name` = '', VALUES(`name`), `name`)"));
	}
}
