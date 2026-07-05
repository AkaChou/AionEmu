package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.gameserver.model.trade.TradePSItem;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PrivateStoreTest {

	@Test
	void removeItemRemovesOnlyRequestedItemAndKeepsOrder() {
		PrivateStore store = new PrivateStore(null);
		store.addItemToSell(10, new TradePSItem(10, 1000, 1, 100));
		store.addItemToSell(20, new TradePSItem(20, 2000, 1, 200));
		store.addItemToSell(30, new TradePSItem(30, 3000, 1, 300));

		store.removeItem(20);

		assertFalse(store.getSoldItems().containsKey(20));
		assertEquals(Arrays.asList(10, 30), new ArrayList<Integer>(store.getSoldItems().keySet()));
	}
}
