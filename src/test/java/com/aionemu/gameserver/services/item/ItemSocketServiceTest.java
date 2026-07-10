package com.aionemu.gameserver.services.item;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

class ItemSocketServiceTest {

	@Test
	void findsEquippedWeaponWhenItIsNotInInventory() {
		Item weapon = new Item(42, new ItemTemplate());
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.inventory = new TestStorage(null);
		player.equipment = new TestEquipment(weapon);

		assertSame(weapon, ItemSocketService.findGodstoneTarget(player, weapon.getObjectId()));
	}

	private static final class TestPlayer extends Player {
		private Storage inventory;
		private Equipment equipment;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public Storage getInventory() {
			return inventory;
		}

		@Override
		public Equipment getEquipment() {
			return equipment;
		}
	}

	private static final class TestStorage extends PlayerStorage {
		private final Item item;

		private TestStorage(Item item) {
			super(StorageType.CUBE);
			this.item = item;
		}

		@Override
		public Item getItemByObjId(int itemObjId) {
			return item != null && item.getObjectId() == itemObjId ? item : null;
		}
	}

	private static final class TestEquipment extends Equipment {
		private final Item item;

		private TestEquipment(Item item) {
			super(null);
			this.item = item;
		}

		@Override
		public Item getEquippedItemByObjId(int itemObjId) {
			return item != null && item.getObjectId() == itemObjId ? item : null;
		}
	}
}
