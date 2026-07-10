package com.aionemu.gameserver.services.drop;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.DropNpc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

class DropDistributionServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void onlyCurrentEligiblePlayerCanRollForMatchingItem() {
		Player member = objenesis.newInstance(Player.class);
		Player outsider = objenesis.newInstance(Player.class);
		DropNpc dropNpc = new DropNpc(100);
		dropNpc.addPlayerStatus(member);
		dropNpc.setDistributionId(2);
		dropNpc.setCurrentIndex(7);
		DropItem item = dropItem(200, 7);
		Set<DropItem> items = Set.of(item);

		assertSame(item, DropDistributionService.findRequestedItem(member, dropNpc, items, 2, 200, 7));
		assertNull(DropDistributionService.findRequestedItem(outsider, dropNpc, items, 2, 200, 7));
		assertNull(DropDistributionService.findRequestedItem(member, dropNpc, items, 3, 200, 7));
		assertNull(DropDistributionService.findRequestedItem(member, dropNpc, items, 2, 201, 7));
		assertNull(DropDistributionService.findRequestedItem(member, dropNpc, items, 2, 200, 8));

		dropNpc.delPlayerStatus(member);
		assertNull(DropDistributionService.findRequestedItem(member, dropNpc, items, 2, 200, 7));
	}

	private static DropItem dropItem(int itemId, int index) {
		int expectedItemId = itemId;
		DropItem item = new DropItem(new Drop() {
			@Override
			public int getItemId() {
				return expectedItemId;
			}

			@Override
			public ItemTemplate getItemTemplate() {
				return new ItemTemplate();
			}
		});
		item.setIndex(index);
		return item;
	}
}
