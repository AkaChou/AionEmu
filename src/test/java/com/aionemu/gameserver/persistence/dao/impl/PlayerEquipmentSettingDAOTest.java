package com.aionemu.gameserver.persistence.dao.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.ibatis.annotations.Insert;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.persistence.entity.PlayerEquipmentSettingPo;
import com.aionemu.gameserver.persistence.mapper.PlayerEquipmentSettingMapper;

/**
 * 装备方案 upsert SQL 契约测试。
 * SQL contract tests for the equipment-setting upsert statement.
 */
class PlayerEquipmentSettingDAOTest {

	@Test
	void insertQueryIncludesRequiredNameColumn() throws Exception {
		assertTrue(upsertSql().contains("`name`"));
	}

	@Test
	void duplicateUpdateDoesNotBlankExistingName() throws Exception {
		String sql = upsertSql();
		String updateClause = sql.substring(sql.indexOf("ON DUPLICATE KEY UPDATE"));

		assertTrue(updateClause.contains("`name` = IF(`name` = '', VALUES(`name`), `name`)"));
	}

	private static String upsertSql() throws Exception {
		return PlayerEquipmentSettingMapper.class.getMethod("upsertSetting", PlayerEquipmentSettingPo.class)
			.getAnnotation(Insert.class).value()[0];
	}
}
