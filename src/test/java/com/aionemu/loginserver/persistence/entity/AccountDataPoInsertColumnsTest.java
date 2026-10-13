package com.aionemu.loginserver.persistence.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mybatisflex.core.table.TableInfoFactory;

/**
 * 账号实体 INSERT 列契约：遗留 INSERT 未写入的三列必须退出 INSERT 列集。
 * 遗留 {@code INSERT_ACCOUNT_QUERY} 只写 name/password/access_level/membership/activated/
 * last_server/last_ip/last_mac/ip_force/toll 十列，{@code old_membership}、{@code balance}、
 * {@code luna} 的取值完全来自 DDL 默认值。Flex 的 {@code insert} 在 {@code ignoreNulls=false}
 * 时按「全列减 notInsertableColumns」拼接，这三个 {@code toPo} 从不赋值的字段会显式写入
 * {@code null} 而违反 NOT NULL 约束，故以 {@code @Column(insertable = false)} 排除。
 * Contract for the account entity INSERT column set: the three columns the legacy INSERT omitted
 * must stay out of it. The legacy {@code INSERT_ACCOUNT_QUERY} wrote only ten columns
 * (name/password/access_level/membership/activated/last_server/last_ip/last_mac/ip_force/toll);
 * {@code old_membership}, {@code balance} and {@code luna} took their values from the DDL defaults.
 * Flex's {@code insert} with {@code ignoreNulls=false} builds its column list as "all columns minus
 * notInsertableColumns", so these three never-assigned fields would be written as an explicit
 * {@code null} and violate the NOT NULL constraint; they are excluded with
 * {@code @Column(insertable = false)}.
 */
class AccountDataPoInsertColumnsTest {

	@Test
	void legacyOmittedColumnsStayOutOfTheInsertColumnSet() {
		String[] insertColumns = TableInfoFactory.ofEntityClass(AccountDataPo.class)
			.obtainInsertColumns(new AccountDataPo(), false);

		for (String property : new String[] {"oldMembership", "balance", "luna"}) {
			String column = TableInfoFactory.ofEntityClass(AccountDataPo.class).getColumnByProperty(property);
			assertFalse(contains(insertColumns, column),
				"列 " + column + " 必须退出 INSERT 列集，否则显式写入 null 违反 NOT NULL 约束");
		}
	}

	@Test
	void legacyInsertedColumnsRemainInTheInsertColumnSet() {
		String[] insertColumns = TableInfoFactory.ofEntityClass(AccountDataPo.class)
			.obtainInsertColumns(new AccountDataPo(), false);

		// 旧 INSERT_ACCOUNT_QUERY 明确写入的列，修复不得误伤。
		// Columns the legacy INSERT_ACCOUNT_QUERY wrote explicitly; the fix must not drop them.
		for (String column : new String[] {"name", "password", "access_level", "membership",
			"activated", "last_server", "last_ip", "last_mac", "ip_force", "toll"}) {
			assertTrue(contains(insertColumns, column), "列 " + column + " 必须保留在 INSERT 列集内");
		}
	}

	private static boolean contains(String[] columns, String column) {
		for (String candidate : columns) {
			if (column.equals(candidate)) {
				return true;
			}
		}
		return false;
	}
}
