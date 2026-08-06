package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;
import java.util.SortedMap;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.templates.item.ArmorType;
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

	@Test
	void powerShardKeepsBothCandidateSlots() {
		Item powerShard = new Item(2001, new TestPowerShardTemplate());

		long slotMask = Equipment.itemSlotMaskForEquip(powerShard, ItemSlot.POWER_SHARD_RIGHT.getSlotIdMask());

		assertEquals(ItemSlot.SHARD_RIGHT_OR_LEFT.getSlotIdMask(), slotMask);
	}

	@Test
	void powerShardUsesExplicitLeftSlot() {
		Item powerShard = new Item(2001, new TestPowerShardTemplate());

		long slotMask = Equipment.itemSlotMaskForEquip(powerShard, ItemSlot.POWER_SHARD_LEFT.getSlotIdMask());

		assertEquals(ItemSlot.POWER_SHARD_LEFT.getSlotIdMask(), slotMask);
	}

	@Test
	void twoHandWeaponIsReturnedOnceWhenItOccupiesTwoSlots() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		Equipment equipment = new Equipment(player);
		Item bow = new Item(3001, new TestWeaponTemplate(3002, true), 1, true,
			ItemSlot.MAIN_OR_SUB.getSlotIdMask());
		Field field = Equipment.class.getDeclaredField("equipment");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		SortedMap<Long, Item> slots = (SortedMap<Long, Item>) field.get(equipment);
		slots.put(ItemSlot.MAIN_HAND.getSlotIdMask(), bow);
		slots.put(ItemSlot.SUB_HAND.getSlotIdMask(), bow);

		assertEquals(List.of(bow), equipment.getEquippedItemsByItemId(3002));
	}

	private static final class TestWeaponTemplate extends ItemTemplate {
		private final int templateId;
		private final boolean twoHandWeapon;

		private TestWeaponTemplate(boolean twoHandWeapon) {
			this(0, twoHandWeapon);
		}

		private TestWeaponTemplate(int templateId, boolean twoHandWeapon) {
			this.templateId = templateId;
			this.twoHandWeapon = twoHandWeapon;
		}

		@Override
		public int getTemplateId() {
			return templateId;
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

	private static final class TestPowerShardTemplate extends ItemTemplate {
		@Override
		public int getItemSlot() {
			return (int) ItemSlot.SHARD_RIGHT_OR_LEFT.getSlotIdMask();
		}

		@Override
		public ArmorType getArmorType() {
			return ArmorType.SHARD;
		}
	}
}
