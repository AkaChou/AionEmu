package com.aionemu.gameserver.persistence.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mybatisflex.core.table.TableInfoFactory;

/**
 * 军阶实体 INSERT 列契约：三列排行位置必须退出 INSERT 列集。
 * 遗留 JDBC 的 INSERT/UPDATE 均不含 {@code rank_pos} / {@code old_rank_pos} / {@code rank_ap}，
 * 其取值完全来自 DDL 默认值（{@code NOT NULL DEFAULT '0'}），随后由排行榜重算 SQL 维护。
 * Flex 的 {@code insert} 在 {@code ignoreNulls=false} 时按「全列减 notInsertableColumns」拼接，
 * 这三个从未被赋值的字段会显式写入 {@code null} 而违反 NOT NULL 约束，故以
 * {@code @Column(insertable = false)} 将其排除。
 * Contract for the abyss-rank entity INSERT column set: the three rank-position columns must stay
 * out of it. The legacy JDBC INSERT/UPDATE never listed {@code rank_pos} / {@code old_rank_pos} /
 * {@code rank_ap}; their values came from the DDL defaults ({@code NOT NULL DEFAULT '0'}) and are
 * maintained by the ranking recalculation SQL. Flex's {@code insert} with {@code ignoreNulls=false}
 * builds its column list as "all columns minus notInsertableColumns", so these three never-assigned
 * fields would be written as an explicit {@code null} and violate the NOT NULL constraint; they are
 * excluded with {@code @Column(insertable = false)}.
 */
class AbyssRankPoInsertColumnsTest {

	@Test
	void rankPositionColumnsStayOutOfTheInsertColumnSet() {
		String[] insertColumns = TableInfoFactory.ofEntityClass(AbyssRankPo.class)
			.obtainInsertColumns(new AbyssRankPo(), false);

		for (String property : new String[] {"rankPos", "oldRankPos", "rankAp"}) {
			String column = TableInfoFactory.ofEntityClass(AbyssRankPo.class).getColumnByProperty(property);
			assertFalse(contains(insertColumns, column),
				"列 " + column + " 必须退出 INSERT 列集，否则显式写入 null 违反 NOT NULL 约束");
		}
	}

	@Test
	void legacyColumnsRemainInTheInsertColumnSet() {
		String[] insertColumns = TableInfoFactory.ofEntityClass(AbyssRankPo.class)
			.obtainInsertColumns(new AbyssRankPo(), false);

		// 旧 INSERT_QUERY 明确写入的列，修复不得误伤。
		// Columns the legacy INSERT_QUERY wrote explicitly; the fix must not drop them.
		for (String column : new String[] {"player_id", "daily_ap", "ap", "gp", "rank", "top_ranking",
			"all_kill", "max_rank", "last_update"}) {
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
