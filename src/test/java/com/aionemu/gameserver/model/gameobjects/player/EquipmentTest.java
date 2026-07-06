package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.templates.item.EquipType;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

class EquipmentTest {

	@Test
	void twoHandWeaponUsesRequestedOffHandWeaponSet() {
		Item bow = new Item(1001, new TestWeaponTemplate(true));

		long slotMask = Equipment.itemSlotMaskForEquip(bow, ItemSlot.MAIN_OFF_HAND.getSlotIdMask());

		assertEquals(ItemSlot.MAIN_OFF_OR_SUB_OFF.getSlotIdMask(), slotMask);
	}

	@Test
	void twoHandWeaponKeepsMainWeaponSetWhenMainHandIsRequested() {
		Item bow = new Item(1001, new TestWeaponTemplate(true));

		long slotMask = Equipment.itemSlotMaskForEquip(bow, ItemSlot.MAIN_HAND.getSlotIdMask());

		assertEquals(ItemSlot.MAIN_OR_SUB.getSlotIdMask(), slotMask);
	}

	@Test
	void oneHandWeaponUsesRequestedOffHandSlot() {
		Item dagger = new Item(1002, new TestWeaponTemplate(false));

		long slotMask = Equipment.itemSlotMaskForEquip(dagger, ItemSlot.MAIN_OFF_HAND.getSlotIdMask());

		assertEquals(ItemSlot.MAIN_OFF_HAND.getSlotIdMask(), slotMask);
	}

	@Test
	void oneHandWeaponUsesRequestedSubHandSlot() {
		Item dagger = new Item(1002, new TestWeaponTemplate(false));

		long slotMask = Equipment.itemSlotMaskForEquip(dagger, ItemSlot.SUB_HAND.getSlotIdMask());

		assertEquals(ItemSlot.SUB_HAND.getSlotIdMask(), slotMask);
	}

	@Test
	void fullRingsReplaceLowerScoredRightSlot() {
		SortedMap<Long, Item> equipment = new TreeMap<>();
		equipment.put(ItemSlot.RING_LEFT.getSlotIdMask(), accessory(2001, 60, ItemQuality.MYTHIC, 0));
		equipment.put(ItemSlot.RING_RIGHT.getSlotIdMask(), accessory(2002, 40, ItemQuality.MYTHIC, 0));

		long slot = Equipment.occupiedSlotWithLowestEquipmentScore(
				ItemSlot.getSlotsFor(ItemSlot.RING_RIGHT_OR_LEFT.getSlotIdMask()), equipment);

		assertEquals(ItemSlot.RING_RIGHT.getSlotIdMask(), slot);
	}

	@Test
	void fullEarringsReplaceLowerScoredLeftSlot() {
		SortedMap<Long, Item> equipment = new TreeMap<>();
		equipment.put(ItemSlot.EARRINGS_LEFT.getSlotIdMask(), accessory(3001, 40, ItemQuality.MYTHIC, 0));
		equipment.put(ItemSlot.EARRINGS_RIGHT.getSlotIdMask(), accessory(3002, 60, ItemQuality.MYTHIC, 0));

		long slot = Equipment.occupiedSlotWithLowestEquipmentScore(
				ItemSlot.getSlotsFor(ItemSlot.EARRING_RIGHT_OR_LEFT.getSlotIdMask()), equipment);

		assertEquals(ItemSlot.EARRINGS_LEFT.getSlotIdMask(), slot);
	}

	@Test
	void accessoryScoreUsesQualityAndEnchantWhenLevelsMatch() {
		SortedMap<Long, Item> equipment = new TreeMap<>();
		equipment.put(ItemSlot.RING_LEFT.getSlotIdMask(), accessory(4001, 50, ItemQuality.UNIQUE, 10));
		equipment.put(ItemSlot.RING_RIGHT.getSlotIdMask(), accessory(4002, 50, ItemQuality.EPIC, 0));

		long slot = Equipment.occupiedSlotWithLowestEquipmentScore(
				ItemSlot.getSlotsFor(ItemSlot.RING_RIGHT_OR_LEFT.getSlotIdMask()), equipment);

		assertEquals(ItemSlot.RING_LEFT.getSlotIdMask(), slot);
	}

	private static Item accessory(int itemId, int level, ItemQuality quality, int enchantLevel) {
		Item item = new Item(itemId, new TestAccessoryTemplate(itemId, level, quality));
		item.setEnchantLevel(enchantLevel);
		return item;
	}

	private static final class TestWeaponTemplate extends ItemTemplate {
		private final boolean twoHandWeapon;

		private TestWeaponTemplate(boolean twoHandWeapon) {
			this.twoHandWeapon = twoHandWeapon;
		}

		@Override
		public int getItemSlot() {
			return (int) ItemSlot.MAIN_OR_SUB.getSlotIdMask();
		}

		@Override
		public EquipType getEquipmentType() {
			return EquipType.WEAPON;
		}

		@Override
		public boolean isTwoHandWeapon() {
			return twoHandWeapon;
		}
	}

	private static final class TestAccessoryTemplate extends ItemTemplate {
		private final int itemId;
		private final int level;
		private final ItemQuality quality;

		private TestAccessoryTemplate(int itemId, int level, ItemQuality quality) {
			this.itemId = itemId;
			this.level = level;
			this.quality = quality;
		}

		@Override
		public int getTemplateId() {
			return itemId;
		}

		@Override
		public int getLevel() {
			return level;
		}

		@Override
		public ItemQuality getItemQuality() {
			return quality;
		}
	}
}
