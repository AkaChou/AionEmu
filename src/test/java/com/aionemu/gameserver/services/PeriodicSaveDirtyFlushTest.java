package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.persistence.dao.impl.InventoryDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.ItemStoneDaoImpl;

/**
 * 周期存盘脏标记防空转单元测试。
 * Unit tests verifying that clean entities bypass database access during periodic flush.
 * <p>空集合直接短路返回，不做任何 Mapper 交互，因此无需装配 Mapper 即可断言。</p>
 * <p>Empty collections short-circuit before any Mapper interaction, so the assertion holds without wired mappers.</p>
 */
class PeriodicSaveDirtyFlushTest {

	@Test
	void emptyItemListBypassesDatabaseCheckoutInInventoryDAO() {
		InventoryDaoImpl dao = new InventoryDaoImpl();

		assertDoesNotThrow(() -> {
			boolean result = dao.store(Collections.emptyList(), 12345, null, null);
			assertTrue(result, "空物品列表应直接返回成功且绝不获取数据库连接");
		});
	}

	@Test
	void emptyItemStoneListBypassesDatabaseCheckoutInItemStoneListDAO() {
		ItemStoneDaoImpl dao = new ItemStoneDaoImpl();

		assertDoesNotThrow(() -> {
			dao.save(Collections.emptyList());
			dao.storeManaStones(Collections.emptySet());
		});
	}
}
