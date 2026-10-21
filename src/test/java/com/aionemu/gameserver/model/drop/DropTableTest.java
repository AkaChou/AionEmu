package com.aionemu.gameserver.model.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.ItemData;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

import org.junit.jupiter.api.Test;

class DropTableTest {

	@Test
	void compactTablePreservesEntriesAndFlags() {
		DropTable table = DropTable.builder()
			.add(111, 1, 1, 100f, false, false, null)
			.add(222, 2, 4, 12.5f, true, false, null)
			.add(333, 1, 1, 50f, false, true, Boolean.FALSE)
			.build();

		assertEquals(3, table.size());
		assertEquals(111, table.itemId(0));
		assertEquals(2, table.minAmount(1));
		assertEquals(4, table.maxAmount(1));
		assertEquals(12.5f, table.chance(1));
		assertTrue(table.isNoReduction(1));
		assertFalse(table.isEachMember(2));
		assertFalse(table.dropAt(2).isEachMember());
	}

	@Test
	void appendAndFilterKeepOrderWithoutMutatingInputs() {
		DropTable first = DropTable.builder()
			.add(1, 1, 1, 100f, false, false, null)
			.add(2, 1, 1, 50f, false, false, null)
			.build();
		DropTable second = DropTable.builder()
			.add(2, 3, 3, 25f, false, false, null)
			.add(3, 1, 1, 10f, false, false, null)
			.build();

		DropTable.ItemIdSet remove = new DropTable.ItemIdSet();
		remove.add(2);
		DropTable appended = first.append(second);
		DropTable filtered = appended.withoutItemIds(remove);

		assertEquals(List.of(1, 2, 2, 3), itemIds(appended));
		assertEquals(List.of(1, 3), itemIds(filtered));
		assertEquals(2, first.size());
		assertEquals(2, second.size());
	}

	@Test
	void itemIdSetDeduplicatesAndGrows() {
		DropTable.ItemIdSet set = new DropTable.ItemIdSet();
		for (int itemId = 0; itemId < 256; itemId++) {
			set.add(itemId);
			set.add(itemId);
		}

		assertEquals(256, set.size());
		assertTrue(set.contains(0));
		assertTrue(set.contains(255));
		assertFalse(set.contains(256));
	}

	@Test
	void runtimeSelectionDoesNotMaterializeCompatibilityList() {
		ItemData originalItemData = DataManager.ITEM_DATA;
		ItemData itemData = new ItemData();
		ItemTemplate itemTemplate = new ItemTemplate();
		itemTemplate.setItemId(111);
		itemData.assembleFromMerged(java.util.Map.of(111, itemTemplate), null);
		DataManager.ITEM_DATA = itemData;
		DropTable table = DropTable.builder()
			.add(111, 1, 1, 100f, false, false, null)
			.build();
		DropGroup group = new DropGroup();
		group.setDropTable(table);
		DropModifiers modifiers = new DropModifiers();
		modifiers.setBoostDropRate(1f);
		Set<DropItem> result = new HashSet<>();

		try {
			group.dropCalculator(result, 1, modifiers, List.of());
		} finally {
			DataManager.ITEM_DATA = originalItemData;
		}

		assertEquals(1, result.size());
		assertEquals(111, result.iterator().next().getDropTemplate().getItemId());
		assertNull(group.drop);
	}

	private static List<Integer> itemIds(DropTable table) {
		List<Integer> itemIds = new java.util.ArrayList<>(table.size());
		for (int i = 0; i < table.size(); i++) {
			itemIds.add(table.itemId(i));
		}
		return itemIds;
	}
}
