package com.aionemu.gameserver.model.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TradeItemTest {

	@Test
	void decreasesPartialAndFullCountsWithoutUnderflow() {
		TradeItem item = new TradeItem(1, 10);

		item.decreaseCount(4);
		assertEquals(6, item.getCount());

		item.decreaseCount(7);
		item.decreaseCount(-1);
		assertEquals(6, item.getCount());

		item.decreaseCount(6);
		assertEquals(0, item.getCount());

		item.decreaseCount(1);
		assertEquals(0, item.getCount());
	}
}
