package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.PrivateStore;
import com.aionemu.gameserver.model.trade.TradeList;
import com.aionemu.gameserver.model.trade.TradePSItem;

class PrivateStoreServiceTest {

	@Test
	void calculatesStorePriceAndRejectsInvalidTotals() {
		PrivateStore store = new PrivateStore(null);
		store.addItemToSell(10, new TradePSItem(10, 1000, 4, 100));
		store.addItemToSell(20, new TradePSItem(20, 2000, 3, 250));

		TradeList purchase = new TradeList();
		purchase.addPSItem(10, 2);
		purchase.addPSItem(20, 3);
		assertEquals(950, PrivateStoreService.getTotalPrice(store, purchase));

		TradeList missingItem = new TradeList();
		missingItem.addPSItem(30, 1);
		assertEquals(-1, PrivateStoreService.getTotalPrice(store, missingItem));

		store.addItemToSell(30, new TradePSItem(30, 3000, 2, Long.MAX_VALUE));
		TradeList overflow = new TradeList();
		overflow.addPSItem(30, 2);
		assertEquals(-1, PrivateStoreService.getTotalPrice(store, overflow));
	}
}
