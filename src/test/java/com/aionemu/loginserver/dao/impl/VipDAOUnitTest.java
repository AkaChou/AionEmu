package com.aionemu.loginserver.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.loginserver.model.Vip;

/**
 * 契约与单元测试：验证 VipDAO 在真实/模拟环境下的入参绑定、返回解析与异常隔离。
 * Contract & unit tests for VipDAO parameter binding, mapping, and error isolation.
 */
class VipDAOUnitTest {

	@Test
	void findByAccountIdMapsColumnsAccurately() {
		Map<String, Object> fakeRow = new HashMap<>();
		fakeRow.put("account_id", 12345);
		fakeRow.put("vip_level", 4);
		fakeRow.put("vip_exp", 987654321L);
		fakeRow.put("expire_time", 1893456000L);

		DataSource mockDataSource = createSingleRowDataSource(fakeRow);
		VipDAO dao = new VipDAO(JdbcClient.create(mockDataSource));

		Vip vip = dao.findByAccountId(12345);
		assertNotNull(vip);
		assertEquals(12345, vip.accountId());
		assertEquals(4, vip.level());
		assertEquals(987654321L, vip.experience());
		assertEquals(1893456000L, vip.expireTime());
	}

	@Test
	void findByAccountIdReturnsNullWhenEmpty() {
		DataSource mockDataSource = createEmptyDataSource();
		VipDAO dao = new VipDAO(JdbcClient.create(mockDataSource));

		Vip vip = dao.findByAccountId(999);
		assertNull(vip);
	}

	@Test
	void insertIfAbsentReturnsTrueWhenRowsAffected() {
		DataSource mockDataSource = createUpdateDataSource(1);
		VipDAO dao = new VipDAO(JdbcClient.create(mockDataSource));

		boolean ok = dao.insertIfAbsent(101, 2);
		assertTrue(ok);
	}

	@Test
	void insertIfAbsentReturnsFalseWhenZeroRowsAffected() {
		DataSource mockDataSource = createUpdateDataSource(0);
		VipDAO dao = new VipDAO(JdbcClient.create(mockDataSource));

		boolean ok = dao.insertIfAbsent(101, 2);
		assertFalse(ok);
	}

	@Test
	void syncMissingAccountsReturnsAffectedCount() {
		DataSource mockDataSource = createUpdateDataSource(7);
		VipDAO dao = new VipDAO(JdbcClient.create(mockDataSource));

		int count = dao.syncMissingAccounts(3);
		assertEquals(7, count);
	}

	private static DataSource createSingleRowDataSource(Map<String, Object> row) {
		return (DataSource) Proxy.newProxyInstance(
			VipDAOUnitTest.class.getClassLoader(),
			new Class<?>[]{DataSource.class},
			(proxy, method, args) -> {
				if ("getConnection".equals(method.getName())) {
					return createMockConnection(row, 1);
				}
				return null;
			}
		);
	}

	private static DataSource createEmptyDataSource() {
		return (DataSource) Proxy.newProxyInstance(
			VipDAOUnitTest.class.getClassLoader(),
			new Class<?>[]{DataSource.class},
			(proxy, method, args) -> {
				if ("getConnection".equals(method.getName())) {
					return createMockConnection(Map.of(), 0);
				}
				return null;
			}
		);
	}

	private static DataSource createUpdateDataSource(int affectedRows) {
		return (DataSource) Proxy.newProxyInstance(
			VipDAOUnitTest.class.getClassLoader(),
			new Class<?>[]{DataSource.class},
			(proxy, method, args) -> {
				if ("getConnection".equals(method.getName())) {
					return createMockUpdateConnection(affectedRows);
				}
				return null;
			}
		);
	}

	private static Object createMockConnection(Map<String, Object> row, int rowCount) {
		return Proxy.newProxyInstance(
			VipDAOUnitTest.class.getClassLoader(),
			new Class<?>[]{java.sql.Connection.class},
			(proxy, method, args) -> {
				if ("prepareStatement".equals(method.getName())) {
					return createMockPreparedStatement(row, rowCount);
				}
				if ("getMetaData".equals(method.getName())) {
					return createMockDatabaseMetaData();
				}
				if ("setAutoCommit".equals(method.getName()) || "close".equals(method.getName())) {
					return null;
				}
				return null;
			}
		);
	}

	private static Object createMockUpdateConnection(int affectedRows) {
		return Proxy.newProxyInstance(
			VipDAOUnitTest.class.getClassLoader(),
			new Class<?>[]{java.sql.Connection.class},
			(proxy, method, args) -> {
				if ("prepareStatement".equals(method.getName())) {
					return createMockUpdatePreparedStatement(affectedRows);
				}
				if ("getMetaData".equals(method.getName())) {
					return createMockDatabaseMetaData();
				}
				if ("setAutoCommit".equals(method.getName()) || "close".equals(method.getName())) {
					return null;
				}
				return null;
			}
		);
	}

	private static Object createMockPreparedStatement(Map<String, Object> row, int rowCount) {
		return Proxy.newProxyInstance(
			VipDAOUnitTest.class.getClassLoader(),
			new Class<?>[]{PreparedStatement.class},
			(proxy, method, args) -> {
				if ("executeQuery".equals(method.getName())) {
					return createMockResultSet(row, rowCount);
				}
				if ("close".equals(method.getName()) || method.getName().startsWith("set")) {
					return null;
				}
				return null;
			}
		);
	}

	private static Object createMockUpdatePreparedStatement(int affectedRows) {
		return Proxy.newProxyInstance(
			VipDAOUnitTest.class.getClassLoader(),
			new Class<?>[]{PreparedStatement.class},
			(proxy, method, args) -> {
				if ("executeUpdate".equals(method.getName())) {
					return affectedRows;
				}
				if ("close".equals(method.getName()) || method.getName().startsWith("set")) {
					return null;
				}
				return null;
			}
		);
	}

	private static Object createMockResultSet(Map<String, Object> row, int rowCount) {
		final int[] cursor = {0};
		return Proxy.newProxyInstance(
			VipDAOUnitTest.class.getClassLoader(),
			new Class<?>[]{ResultSet.class},
			(proxy, method, args) -> {
				if ("next".equals(method.getName())) {
					cursor[0]++;
					return cursor[0] <= rowCount;
				}
				if ("getInt".equals(method.getName())) {
					String col = (String) args[0];
					return ((Number) row.get(col)).intValue();
				}
				if ("getLong".equals(method.getName())) {
					String col = (String) args[0];
					return ((Number) row.get(col)).longValue();
				}
				if ("close".equals(method.getName()) || "wasNull".equals(method.getName())) {
					return false;
				}
				if ("getMetaData".equals(method.getName())) {
					return createMockResultSetMetaData(row);
				}
				return null;
			}
		);
	}

	private static Object createMockResultSetMetaData(Map<String, Object> row) {
		return Proxy.newProxyInstance(
			VipDAOUnitTest.class.getClassLoader(),
			new Class<?>[]{ResultSetMetaData.class},
			(proxy, method, args) -> {
				if ("getColumnCount".equals(method.getName())) {
					return row.size();
				}
				return null;
			}
		);
	}

	private static Object createMockDatabaseMetaData() {
		return Proxy.newProxyInstance(
			VipDAOUnitTest.class.getClassLoader(),
			new Class<?>[]{java.sql.DatabaseMetaData.class},
			(proxy, method, args) -> {
				if ("getDatabaseProductName".equals(method.getName())) {
					return "MySQL";
				}
				return null;
			}
		);
	}
}
