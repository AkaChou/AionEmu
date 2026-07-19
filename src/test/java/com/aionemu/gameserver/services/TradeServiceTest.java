package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.limiteditems.LimitedItem;
import com.aionemu.gameserver.model.templates.item.Acquisition;
import com.aionemu.gameserver.model.templates.item.AcquisitionType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.trade.TradeItem;
import com.aionemu.gameserver.model.trade.TradeList;
import com.aionemu.gameserver.services.item.ItemService;

class TradeServiceTest {

	@Test
	void rejectsBatchOfNonStackableItemsWhenOnlyOneSlotIsFree() throws Exception {
		TradeList tradeList = tradeList(item(1001, 3, template(1001, 1, null)));

		assertFalse(TradeService.hasInventorySpace(new TestStorage(1, Map.of()), tradeList));
		assertTrue(TradeService.hasInventorySpace(new TestStorage(3, Map.of()), tradeList));
	}

	@Test
	void usesExistingStackCapacityBeforeRequiringAnotherSlot() throws Exception {
		ItemTemplate template = template(1001, 10, null);
		Item existing = new Item(1, template);
		existing.setItemCount(8);

		assertTrue(TradeService.hasInventorySpace(new TestStorage(0, Map.of(), Map.of(1001, List.of(existing))),
				tradeList(item(1001, 2, template))));
		assertFalse(TradeService.hasInventorySpace(new TestStorage(0, Map.of(), Map.of(1001, List.of(existing))),
				tradeList(item(1001, 3, template))));
	}

	@Test
	void calculatesMedalsForEveryItemInTheBatch() throws Exception {
		int medalId = 186000096;
		TradeList tradeList = tradeList(item(1001, 3, template(1001, 1, acquisition(medalId, 2, 100))));
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.inventory = new TestStorage(10, Map.of(medalId, 100L));
		player.abyssRank = new AbyssRank(0, 0, 0, 0, 10_000, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0);

		assertTrue(tradeList.calculateAbyssBuyListPrice(player));
		assertEquals(300, tradeList.getRequiredAp());
		assertEquals(6L, tradeList.getRequiredItems().get(medalId));
	}

	@Test
	void rejectsOverflowedBatchCosts() throws Exception {
		TradeList tradeList = tradeList(item(1001, Long.MAX_VALUE,
				template(1001, 1, acquisition(186000096, 2, 100))));
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.inventory = new TestStorage(10, Map.of(186000096, Long.MAX_VALUE));
		player.abyssRank = new AbyssRank(0, 0, 0, 0, 10_000, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0);

		assertFalse(tradeList.calculateAbyssBuyListPrice(player));
		assertTrue(tradeList.getRequiredItems().isEmpty());
	}

	@Test
	void rejectsWholeLimitedBatchBeforeUpdatingOrCharging() {
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		LimitedItem limitedItem = new LimitedItem(1001, 10, 3, "0 0 0 * * ?");
		limitedItem.getBuyCount().put(player.getObjectId(), 2);

		assertFalse(TradeService.canPurchaseLimitedItems(player, Map.of(limitedItem, 2L)));
		assertTrue(TradeService.canPurchaseLimitedItems(player, Map.of(limitedItem, 1L)));
		assertEquals(10, limitedItem.getSellLimit());
		assertEquals(2, limitedItem.getBuyCount().get(player.getObjectId()));
	}

	@Test
	void doesNotConsumeAnyRequiredItemWhenOneMaterialIsInsufficient() {
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		MutableTestStorage inventory = new MutableTestStorage(Map.of(1001, 5L, 1002, 9L));
		player.inventory = inventory;

		assertFalse(ItemService.decreaseItems(player, Map.of(1001, 5L, 1002, 10L)));
		assertEquals(5L, inventory.getItemCountByItemId(1001));
		assertEquals(9L, inventory.getItemCountByItemId(1002));
	}

	private static TradeList tradeList(TradeItem... items) {
		TradeList tradeList = new TradeList();
		tradeList.getTradeItems().addAll(List.of(items));
		return tradeList;
	}

	private static TradeItem item(int itemId, long count, ItemTemplate template) {
		TradeItem item = new TradeItem(itemId, count);
		item.setItemTemplate(template);
		return item;
	}

	private static ItemTemplate template(int itemId, int maxStackCount, Acquisition acquisition) throws Exception {
		ItemTemplate template = new ItemTemplate();
		setField(template, "itemId", itemId);
		setField(template, "maxStackCount", maxStackCount);
		setField(template, "acquisition", acquisition);
		return template;
	}

	private static Acquisition acquisition(int itemId, int count, int ap) throws Exception {
		Acquisition acquisition = new Acquisition();
		setField(acquisition, "type", AcquisitionType.ABYSS);
		setField(acquisition, "itemId", itemId);
		setField(acquisition, "itemCount", count);
		setField(acquisition, "ap", ap);
		return acquisition;
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class TestPlayer extends Player {
		private Storage inventory;
		private AbyssRank abyssRank;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public Storage getInventory() {
			return inventory;
		}

		@Override
		public AbyssRank getAbyssRank() {
			return abyssRank;
		}
	}

	private static final class TestStorage extends PlayerStorage {
		private final int freeSlots;
		private final Map<Integer, Long> itemCounts;
		private final Map<Integer, List<Item>> items;

		private TestStorage(int freeSlots, Map<Integer, Long> itemCounts) {
			this(freeSlots, itemCounts, Map.of());
		}

		private TestStorage(int freeSlots, Map<Integer, Long> itemCounts, Map<Integer, List<Item>> items) {
			super(StorageType.CUBE);
			this.freeSlots = freeSlots;
			this.itemCounts = itemCounts;
			this.items = items;
		}

		@Override
		public int getFreeSlots() {
			return freeSlots;
		}

		@Override
		public int getSpecialCubeFreeSlots() {
			return freeSlots;
		}

		@Override
		public long getItemCountByItemId(int itemId) {
			return itemCounts.getOrDefault(itemId, 0L);
		}

		@Override
		public List<Item> getItemsByItemId(int itemId) {
			return items.getOrDefault(itemId, List.of());
		}
	}

	private static final class MutableTestStorage extends PlayerStorage {
		private final Map<Integer, Long> itemCounts;

		private MutableTestStorage(Map<Integer, Long> itemCounts) {
			super(StorageType.CUBE);
			this.itemCounts = new HashMap<>(itemCounts);
		}

		@Override
		public long getItemCountByItemId(int itemId) {
			return itemCounts.getOrDefault(itemId, 0L);
		}

		@Override
		public boolean decreaseByItemId(int itemId, long count) {
			long current = getItemCountByItemId(itemId);
			if (current < count) {
				return false;
			}
			itemCounts.put(itemId, current - count);
			return true;
		}
	}
}
