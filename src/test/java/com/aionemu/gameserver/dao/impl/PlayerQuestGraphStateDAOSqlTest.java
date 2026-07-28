package com.aionemu.gameserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class PlayerQuestGraphStateDAOSqlTest {

	@Test
	void selectIsDeterministicAndUpsertPersistsRecoveryColumns() {
		assertTrue(PlayerQuestGraphStateDAO.SELECT_QUERY.endsWith("ORDER BY `quest_id`"));
		assertTrue(PlayerQuestGraphStateDAO.UPSERT_QUERY.contains("ON DUPLICATE KEY UPDATE"));
		assertTrue(PlayerQuestGraphStateDAO.UPSERT_QUERY.contains("IF(VALUES(`revision`) > `revision`"));
		assertTrue(PlayerQuestGraphStateDAO.UPSERT_QUERY.contains("`next_deadline_at` = IF("));
		assertTrue(PlayerQuestGraphStateDAO.UPSERT_QUERY.contains("`state_payload` = IF("));
		assertTrue(PlayerQuestGraphStateDAO.UPSERT_QUERY.contains("`revision` = GREATEST(`revision`, VALUES(`revision`))"));
		assertTrue(Arrays.asList(new GameDAOClassProvider().daoClasses()).contains(PlayerQuestGraphStateDAO.class));
	}
}
