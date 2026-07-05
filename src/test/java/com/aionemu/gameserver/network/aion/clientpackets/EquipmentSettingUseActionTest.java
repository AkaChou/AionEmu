package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.items.ItemSlot;

class EquipmentSettingUseActionTest {

	@Test
	void actionTwoSwitchesWeaponHands() {
		FakeTarget target = new FakeTarget();

		boolean changed = new EquipmentSettingUseAction(2, 0, 0).apply(target);

		assertTrue(changed);
		assertEquals(1, target.switchHands);
		assertEquals(0, target.equip);
		assertEquals(0, target.unEquip);
	}

	@Test
	void actionZeroEquipsItem() {
		FakeTarget target = new FakeTarget();

		boolean changed = new EquipmentSettingUseAction(0, 2, 1001).apply(target);

		assertTrue(changed);
		assertEquals(1001, target.itemObjectId);
		assertEquals(2, target.slot);
		assertEquals(1, target.equip);
	}

	@Test
	void actionOneUnequipsItem() {
		FakeTarget target = new FakeTarget();

		boolean changed = new EquipmentSettingUseAction(1, 1, 1002).apply(target);

		assertTrue(changed);
		assertEquals(1002, target.itemObjectId);
		assertEquals(1, target.slot);
		assertEquals(1, target.unEquip);
	}

	@Test
	void weaponSetSwapSwitchesHandsWithoutUnequippingPreviousWeapons() {
		FakeTarget target = new FakeTarget();
		target.equippedSlots.put(128523, ItemSlot.MAIN_OR_SUB.getSlotIdMask());
		target.equippedSlots.put(127229, ItemSlot.MAIN_OFF_HAND.getSlotIdMask());
		target.equippedSlots.put(127185, ItemSlot.SUB_OFF_HAND.getSlotIdMask());

		boolean changed = EquipmentSettingUseAction.applyAll(Arrays.asList(
				new EquipmentSettingUseAction(0, ItemSlot.MAIN_HAND.getSlotIdMask(), 127229),
				new EquipmentSettingUseAction(0, ItemSlot.SUB_HAND.getSlotIdMask(), 127185),
				new EquipmentSettingUseAction(1, ItemSlot.MAIN_HAND.getSlotIdMask(), 128523)), target);

		assertTrue(changed);
		assertEquals(1, target.switchHands);
		assertEquals(0, target.unEquip);
		assertEquals(0, target.equip);
		assertEquals(ItemSlot.MAIN_OFF_OR_SUB_OFF.getSlotIdMask(), target.equippedSlots.get(128523).longValue());
		assertEquals(ItemSlot.MAIN_HAND.getSlotIdMask(), target.equippedSlots.get(127229).longValue());
		assertEquals(ItemSlot.SUB_HAND.getSlotIdMask(), target.equippedSlots.get(127185).longValue());
	}

	@Test
	void weaponSetSwapIgnoresUnequipWhenItemAlreadyReachedRequestedOffHandSlot() {
		FakeTarget target = new FakeTarget();
		target.equippedSlots.put(128523, ItemSlot.MAIN_OR_SUB.getSlotIdMask());
		target.equippedSlots.put(127229, ItemSlot.MAIN_OFF_HAND.getSlotIdMask());
		target.equippedSlots.put(127185, ItemSlot.SUB_OFF_HAND.getSlotIdMask());

		boolean changed = EquipmentSettingUseAction.applyAll(Arrays.asList(
				new EquipmentSettingUseAction(0, ItemSlot.MAIN_HAND.getSlotIdMask(), 127229),
				new EquipmentSettingUseAction(0, ItemSlot.SUB_HAND.getSlotIdMask(), 127185),
				new EquipmentSettingUseAction(0, ItemSlot.MAIN_OFF_HAND.getSlotIdMask(), 128523),
				new EquipmentSettingUseAction(0, ItemSlot.SUB_OFF_HAND.getSlotIdMask(), 128523),
				new EquipmentSettingUseAction(1, ItemSlot.MAIN_OFF_HAND.getSlotIdMask(), 128523)), target);

		assertTrue(changed);
		assertEquals(1, target.switchHands);
		assertEquals(0, target.unEquip);
		assertEquals(0, target.equip);
		assertEquals(ItemSlot.MAIN_OFF_OR_SUB_OFF.getSlotIdMask(), target.equippedSlots.get(128523).longValue());
		assertEquals(ItemSlot.MAIN_HAND.getSlotIdMask(), target.equippedSlots.get(127229).longValue());
		assertEquals(ItemSlot.SUB_HAND.getSlotIdMask(), target.equippedSlots.get(127185).longValue());
	}

	@Test
	void mixedWeaponSetSwapReusesFirstMainHandAsSecondOffHandWithoutWholeHandSwitch() {
		FakeTarget target = new FakeTarget();
		target.strictInventory = true;
		target.equippedSlots.put(1001, ItemSlot.MAIN_HAND.getSlotIdMask());
		target.equippedSlots.put(2001, ItemSlot.SUB_HAND.getSlotIdMask());
		target.inventoryItemIds.add(3001);

		boolean changed = EquipmentSettingUseAction.applyAll(Arrays.asList(
				new EquipmentSettingUseAction(2, 0, 0),
				new EquipmentSettingUseAction(0, ItemSlot.SUB_OFF_HAND.getSlotIdMask(), 1001),
				new EquipmentSettingUseAction(1, ItemSlot.MAIN_HAND.getSlotIdMask(), 1001),
				new EquipmentSettingUseAction(0, ItemSlot.MAIN_OFF_HAND.getSlotIdMask(), 3001)), target);

		assertTrue(changed);
		assertEquals(0, target.switchHands);
		assertEquals(Arrays.asList(
				operation("equip", 3001, ItemSlot.MAIN_OFF_HAND),
				operation("unequip", 1001, ItemSlot.MAIN_HAND),
				operation("equip", 1001, ItemSlot.SUB_OFF_HAND)), target.operations);
		assertEquals(ItemSlot.MAIN_OFF_HAND.getSlotIdMask(), target.equippedSlots.get(3001).longValue());
		assertEquals(ItemSlot.SUB_OFF_HAND.getSlotIdMask(), target.equippedSlots.get(1001).longValue());
		assertEquals(ItemSlot.SUB_HAND.getSlotIdMask(), target.equippedSlots.get(2001).longValue());
	}

	@Test
	void firstSetMainHandCanBecomeSecondSetSubHandWhenSecondMainHandIsNew() {
		FakeTarget target = new FakeTarget();
		target.strictInventory = true;
		target.equippedSlots.put(1001, ItemSlot.MAIN_HAND.getSlotIdMask());
		target.equippedSlots.put(2001, ItemSlot.SUB_HAND.getSlotIdMask());
		target.inventoryItemIds.add(3001);

		boolean changed = EquipmentSettingUseAction.applyAll(Arrays.asList(
				new EquipmentSettingUseAction(0, ItemSlot.SUB_HAND.getSlotIdMask(), 1001),
				new EquipmentSettingUseAction(1, ItemSlot.SUB_HAND.getSlotIdMask(), 2001),
				new EquipmentSettingUseAction(0, ItemSlot.MAIN_HAND.getSlotIdMask(), 3001)), target);

		assertTrue(changed);
		assertEquals(0, target.switchHands);
		assertEquals(Arrays.asList(
				operation("equip", 3001, ItemSlot.MAIN_HAND),
				operation("equip", 1001, ItemSlot.SUB_HAND)), target.operations);
		assertEquals(ItemSlot.MAIN_HAND.getSlotIdMask(), target.equippedSlots.get(3001).longValue());
		assertEquals(ItemSlot.SUB_HAND.getSlotIdMask(), target.equippedSlots.get(1001).longValue());
		assertFalse(target.equippedSlots.containsKey(2001));
	}

	@Test
	void normalInventoryEquipStillUsesEquipAction() {
		FakeTarget target = new FakeTarget();

		boolean changed = EquipmentSettingUseAction.applyAll(Arrays.asList(
				new EquipmentSettingUseAction(0, ItemSlot.MAIN_HAND.getSlotIdMask(), 2001)), target);

		assertTrue(changed);
		assertEquals(1, target.equip);
		assertEquals(0, target.switchHands);
		assertEquals(0, target.unEquip);
	}

	@Test
	void blockedHandSwitchStopsWholeBatch() {
		FakeTarget target = new FakeTarget();
		target.switchHandsResult = false;

		boolean changed = EquipmentSettingUseAction.applyAll(Arrays.asList(
				new EquipmentSettingUseAction(2, 0, 0),
				new EquipmentSettingUseAction(0, ItemSlot.HELMET.getSlotIdMask(), 3001)), target);

		assertFalse(changed);
		assertEquals(1, target.switchHands);
		assertEquals(0, target.equip);
		assertEquals(0, target.unEquip);
	}

	private static class FakeTarget implements EquipmentSettingUseTarget {
		private int equip;
		private int unEquip;
		private int switchHands;
		private int itemObjectId;
		private long slot;
		private boolean switchHandsResult = true;
		private boolean strictInventory;
		private final Map<Integer, Long> equippedSlots = new HashMap<Integer, Long>();
		private final Set<Integer> inventoryItemIds = new HashSet<Integer>();
		private final List<String> operations = new ArrayList<String>();

		@Override
		public boolean equipItem(int itemObjectId, long slot) {
			this.itemObjectId = itemObjectId;
			this.slot = slot;
			equip++;
			if (strictInventory && !inventoryItemIds.remove(itemObjectId)) {
				return false;
			}
			operations.add("equip:" + itemObjectId + ":" + slot);
			equippedSlots.remove(itemObjectId);
			equippedSlots.entrySet().removeIf(equippedSlot -> {
				boolean overlaps = (equippedSlot.getValue() & slot) != 0;
				if (overlaps && strictInventory) {
					inventoryItemIds.add(equippedSlot.getKey());
				}
				return overlaps;
			});
			equippedSlots.put(itemObjectId, slot);
			return true;
		}

		@Override
		public boolean unEquipItem(int itemObjectId, long slot) {
			this.itemObjectId = itemObjectId;
			this.slot = slot;
			unEquip++;
			Long currentSlot = equippedSlots.get(itemObjectId);
			if (strictInventory && (currentSlot == null || (currentSlot & slot) == 0)) {
				return false;
			}
			operations.add("unequip:" + itemObjectId + ":" + slot);
			if (currentSlot != null && (currentSlot & slot) != 0) {
				equippedSlots.remove(itemObjectId);
				if (strictInventory) {
					inventoryItemIds.add(itemObjectId);
				}
			}
			return true;
		}

		@Override
		public boolean switchHands() {
			switchHands++;
			operations.add("switch");
			if (!switchHandsResult) {
				return false;
			}
			Map<Integer, Long> switchedSlots = new HashMap<Integer, Long>();
			for (Map.Entry<Integer, Long> equippedSlot : equippedSlots.entrySet()) {
				long slot = equippedSlot.getValue();
				if ((slot & ItemSlot.RIGHT_HAND.getSlotIdMask()) != 0) {
					slot ^= ItemSlot.RIGHT_HAND.getSlotIdMask();
				}
				if ((slot & ItemSlot.LEFT_HAND.getSlotIdMask()) != 0) {
					slot ^= ItemSlot.LEFT_HAND.getSlotIdMask();
				}
				switchedSlots.put(equippedSlot.getKey(), slot);
			}
			equippedSlots.clear();
			equippedSlots.putAll(switchedSlots);
			return true;
		}

		@Override
		public long getEquippedSlot(int itemObjectId) {
			Long slot = equippedSlots.get(itemObjectId);
			return slot == null ? 0 : slot;
		}
	}

	private static String operation(String operation, int itemObjectId, ItemSlot slot) {
		return operation + ":" + itemObjectId + ":" + slot.getSlotIdMask();
	}
}
