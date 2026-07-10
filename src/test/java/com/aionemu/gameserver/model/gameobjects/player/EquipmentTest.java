package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.templates.item.EquipType;
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
	void accessoryUsesClientCombatPowerSelectedSlot() {
		Item ring = new Item(2000, new TestAccessoryTemplate());

		long slotMask = Equipment.itemSlotMaskForEquip(ring, ItemSlot.RING_RIGHT.getSlotIdMask());

		assertEquals(ItemSlot.RING_RIGHT.getSlotIdMask(), slotMask);
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
		@Override
		public int getItemSlot() {
			return (int) ItemSlot.RING_RIGHT_OR_LEFT.getSlotIdMask();
		}
	}
}
