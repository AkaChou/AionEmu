package com.aionemu.gameserver.questEngine.retail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RetailClientSummaryRowsTest {

	@Test
	void defaultSummaryRowsMatchesDecodedExpectations() {
		RetailClientSummaryRows rows = RetailClientSummaryRows.defaultSummaryRows();
		assertNotNull(rows);
		assertEquals(8931, rows.size());

		// 抽样验证代表性任务行数
		assertEquals(4, rows.rows(1000));
		assertEquals(3, rows.lastRowIndex(1000));

		assertEquals(5, rows.rows(1001));
		assertEquals(4, rows.lastRowIndex(1001));

		assertEquals(10, rows.rows(1002));
		assertEquals(9, rows.lastRowIndex(1002));

		assertEquals(5, rows.rows(1003));
		assertEquals(4, rows.lastRowIndex(1003));

		assertEquals(7, rows.rows(1004));
		assertEquals(6, rows.lastRowIndex(1004));

		assertEquals(10, rows.rows(1005));
		assertEquals(9, rows.lastRowIndex(1005));

		assertEquals(11, rows.rows(1016));
		assertEquals(10, rows.lastRowIndex(1016));

		assertEquals(1, rows.rows(1018));
		assertEquals(0, rows.lastRowIndex(1018));

		assertEquals(13, rows.rows(1035));
		assertEquals(12, rows.lastRowIndex(1035));

		// 未登记任务
		assertEquals(0, rows.rows(-999));
		assertEquals(0, rows.lastRowIndex(-999));
	}
}
