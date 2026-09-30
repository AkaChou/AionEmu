package com.aionemu.gameserver.questEngine.retail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 已退役客户端契约 TSV 的常量回归门：防止常量视图退化为空并复现生产启动缺资源问题。
 * <p>
 * Regression gate for retired client-contract TSVs; their canonical constants must not silently empty.
 */
class RetailClientContractConstantsGateTest {

	@Test
	void dialogExitsKeepCanonicalSelectNoneRows() {
		RetailClientDialogExits exits = RetailClientDialogExits.defaultExits();
		assertEquals(14, exits.size());

		for (int questId : new int[] {1888, 2888, 15478, 15479, 15606, 16800, 25050,
				25073, 25094, 25478, 25479, 25606, 26800, 80989}) {
			assertTrue(exits.requires(questId, RetailClientDialogExits.SELECT_NONE_1),
				() -> "SELECT_NONE_1 must stay registered for quest " + questId);
		}

		assertFalse(exits.requires(1000, RetailClientDialogExits.SELECT_NONE_1));
		assertFalse(exits.requires(1888, "LEGACY_PAGE_EXIT"));
	}

	@Test
	void useItemReportKeepsCheckModeItems() {
		RetailClientUseItemReport report = RetailClientUseItemReport.defaultReport();

		record Check(int questId, int itemId) {
		}

		for (Check check : new Check[] {new Check(80482, 182215419), new Check(80486, 182215421),
				new Check(80554, 182215444), new Check(80558, 182215445),
				new Check(80612, 182215579)}) {
			assertEquals(RetailClientUseItemReport.Mode.CHECK, report.mode(check.questId()));
			assertEquals(check.itemId(), report.checkItemId(check.questId()));
		}

		assertEquals(RetailClientUseItemReport.Mode.REWARD, report.mode(80481));
		assertEquals(-1, report.checkItemId(80481));
	}
}
