package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.gameserver.dao.impl.InventoryDAO;
import com.aionemu.gameserver.dao.impl.ItemStoneListDAO;

/**
 * 周期存盘脏标记防空转单元测试。
 * Unit tests verifying that clean entities bypass database connection checkout during periodic flush.
 */
class PeriodicSaveDirtyFlushTest {

	@Test
	void emptyItemListBypassesDatabaseCheckoutInInventoryDAO() {
		DataSource noAccessDataSource = createNoAccessDataSource();
		InventoryDAO dao = new InventoryDAO(JdbcClient.create(noAccessDataSource));

		assertDoesNotThrow(() -> {
			boolean result = dao.store(Collections.emptyList(), 12345, null, null);
			assertTrue(result, "空物品列表应直接返回成功且绝不获取数据库连接");
		});
	}

	@Test
	void emptyItemStoneListBypassesDatabaseCheckoutInItemStoneListDAO() {
		DataSource noAccessDataSource = createNoAccessDataSource();
		ItemStoneListDAO dao = new ItemStoneListDAO(JdbcClient.create(noAccessDataSource));

		assertDoesNotThrow(() -> {
			dao.save(Collections.emptyList());
			dao.storeManaStones(Collections.emptySet());
		});
	}

	private static DataSource createNoAccessDataSource() {
		return (DataSource) Proxy.newProxyInstance(
			PeriodicSaveDirtyFlushTest.class.getClassLoader(),
			new Class<?>[]{DataSource.class},
			(proxy, method, args) -> {
				if ("getConnection".equals(method.getName())) {
					throw new AssertionError("对干净/未变动实体执行存盘时不应尝试获取数据库连接！");
				}
				return null;
			}
		);
	}
}
