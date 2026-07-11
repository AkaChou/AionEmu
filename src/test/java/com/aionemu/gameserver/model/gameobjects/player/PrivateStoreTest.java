package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.trade.TradePSItem;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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

	@Test
	void soldItemsAreSynchronizedAndReturnedAsDetachedSnapshots() throws Exception {
		assertTrue(Modifier.isSynchronized(PrivateStore.class.getDeclaredMethod("getSoldItems").getModifiers()));
		assertTrue(Modifier.isSynchronized(PrivateStore.class
				.getDeclaredMethod("addItemToSell", int.class, TradePSItem.class).getModifiers()));
		assertTrue(Modifier.isSynchronized(PrivateStore.class
				.getDeclaredMethod("decreaseItemCount", int.class, long.class).getModifiers()));

		PrivateStore store = new PrivateStore(null);
		TradePSItem listedItem = new TradePSItem(10, 1000, 2, 100);
		store.addItemToSell(10, listedItem);
		listedItem.decreaseCount(1);

		LinkedHashMap<Integer, TradePSItem> snapshot = store.getSoldItems();
		snapshot.get(10).decreaseCount(1);
		snapshot.clear();

		assertEquals(2, store.getTradeItemByObjId(10).getCount());
		store.decreaseItemCount(10, 2);
		assertFalse(store.getSoldItems().containsKey(10));
	}
}
