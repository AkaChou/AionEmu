package com.aionemu.gameserver.network.aion.clientpackets;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.items.ItemSlot;

/**
 * 装备方案应用动作（穿戴/卸下/切换主副手）。
 * unequip / switch hands). / unequip / switch hands).
 */
class EquipmentSettingUseAction {

	/** 穿戴动作 / Equip action. */
	private static final int ACTION_EQUIP = 0;
	/** 卸下动作 / Unequip action. */
	private static final int ACTION_UNEQUIP = 1;
	/** 切换主副手 / Switch hands action. */
	private static final int ACTION_SWITCH_HANDS = 2;
	/** 武器槽位掩码 / Weapon slot mask. */
	private static final long WEAPON_SLOTS = ItemSlot.MAIN_OR_SUB.getSlotIdMask() | ItemSlot.MAIN_OFF_OR_SUB_OFF.getSlotIdMask();

	/**
	 * 动作类型。
	 * Action type.
	 */
	private final int action;
	/**
	 * 目标槽位掩码。
	 * Target slot mask.
	 */
	private final long slot;
	/**
	 * 物品对象 ID。
	 * Item object id.
	 */
	private final int itemObjectId;

	/**
	 * action type
	 * slot
	 * item object id
	 */
	EquipmentSettingUseAction(int action, long slot, int itemObjectId) {
		this.action = action;
		this.slot = slot;
		this.itemObjectId = itemObjectId;
	}

	/**
	 * 对目标执行本动作。
	 * Applies this action to the target.
	 *
	 * equipment target
	 * @return 是否变更成功 / whether changed
	 */
	boolean apply(EquipmentSettingUseTarget target) {
		switch (action) {
		case ACTION_EQUIP:
			return target.equipItem(itemObjectId, slot);
		case ACTION_UNEQUIP:
			return target.unEquipItem(itemObjectId, slot);
		case ACTION_SWITCH_HANDS:
			return target.switchHands();
		default:
			return false;
		}
	}

	static boolean applyAll(List<EquipmentSettingUseAction> actions, EquipmentSettingUseTarget target) {
		boolean changed = false;
		boolean switchWeaponSets = shouldSwitchWeaponSets(actions, target);
		if (switchWeaponSets && !target.canSwitchHands()) {
			return false;
		}

		changed |= unequipCurrentEquipment(actions, target, switchWeaponSets);
		changed |= applyNonWeaponEquips(actions, target);
		if (switchWeaponSets) {
			changed |= applyWeaponEquips(actions, target, ItemSlot.MAIN_OFF_OR_SUB_OFF.getSlotIdMask(), true);
			if (!target.switchHands()) {
				return changed;
			}
			changed = true;
			changed |= applyWeaponEquips(actions, target, ItemSlot.MAIN_OR_SUB.getSlotIdMask(), false);
		} else {
			changed |= applyWeaponEquips(actions, target, ItemSlot.MAIN_OFF_OR_SUB_OFF.getSlotIdMask(), false);
			changed |= applyWeaponEquips(actions, target, ItemSlot.MAIN_OR_SUB.getSlotIdMask(), false);
		}
		return changed;
	}

	private static boolean unequipCurrentEquipment(List<EquipmentSettingUseAction> actions, EquipmentSettingUseTarget target,
			boolean switchWeaponSets) {
		boolean changed = false;
		Map<Integer, Long> slotsToUnequip = new LinkedHashMap<Integer, Long>();
		for (EquipmentSettingUseAction action : actions) {
			if (action.action == ACTION_UNEQUIP) {
				addCurrentSlotToUnequip(slotsToUnequip, action.itemObjectId, target);
			}
		}
		for (EquipmentSettingUseAction action : actions) {
			if (action.action == ACTION_EQUIP && shouldUnequipBeforeEquip(action, target, switchWeaponSets)) {
				addCurrentSlotToUnequip(slotsToUnequip, action.itemObjectId, target);
			}
		}
		for (Map.Entry<Integer, Long> slotToUnequip : slotsToUnequip.entrySet()) {
			changed |= target.unEquipItem(slotToUnequip.getKey(), slotToUnequip.getValue());
		}
		return changed;
	}

	private static void addCurrentSlotToUnequip(Map<Integer, Long> slotsToUnequip, int itemObjectId, EquipmentSettingUseTarget target) {
		long currentSlot = target.getEquippedSlot(itemObjectId);
		if (currentSlot != 0 && !slotsToUnequip.containsKey(itemObjectId)) {
			slotsToUnequip.put(itemObjectId, currentSlot);
		}
	}

	private static boolean shouldUnequipBeforeEquip(EquipmentSettingUseAction action, EquipmentSettingUseTarget target, boolean switchWeaponSets) {
		long currentSlot = target.getEquippedSlot(action.itemObjectId);
		if (currentSlot == 0) {
			return false;
		}
		if (switchWeaponSets && isWeaponSlot(action.slot)) {
			return true;
		}
		return !isEquipSatisfied(currentSlot, action.slot);
	}

	private static boolean applyNonWeaponEquips(List<EquipmentSettingUseAction> actions, EquipmentSettingUseTarget target) {
		boolean changed = false;
		for (EquipmentSettingUseAction action : actions) {
			if (action.action == ACTION_EQUIP && !isWeaponSlot(action.slot) && !isEquipSatisfied(target.getEquippedSlot(action.itemObjectId), action.slot)) {
				changed |= action.apply(target);
			}
		}
		return changed;
	}

	private static boolean applyWeaponEquips(List<EquipmentSettingUseAction> actions, EquipmentSettingUseTarget target, long weaponSet,
			boolean stageForSwitch) {
		boolean changed = false;
		Map<Integer, Long> requestedWeaponSlots = getRequestedWeaponSlots(actions, weaponSet);
		for (Map.Entry<Integer, Long> requestedWeaponSlot : requestedWeaponSlots.entrySet()) {
			long slot = stageForSwitch ? switchWeaponHands(requestedWeaponSlot.getValue()) : requestedWeaponSlot.getValue();
			if (!isEquipSatisfied(target.getEquippedSlot(requestedWeaponSlot.getKey()), slot)) {
				changed |= target.equipItem(requestedWeaponSlot.getKey(), slot);
			}
		}
		return changed;
	}

	private static Map<Integer, Long> getRequestedWeaponSlots(List<EquipmentSettingUseAction> actions, long weaponSet) {
		Map<Integer, Long> requestedWeaponSlots = new LinkedHashMap<Integer, Long>();
		for (EquipmentSettingUseAction action : actions) {
			if (action.action == ACTION_EQUIP && isWeaponSlot(action.slot) && (action.slot & weaponSet) != 0) {
				Long requestedSlot = requestedWeaponSlots.get(action.itemObjectId);
				requestedWeaponSlots.put(action.itemObjectId, requestedSlot == null ? action.slot : requestedSlot | action.slot);
			}
		}
		return requestedWeaponSlots;
	}

	private static boolean shouldSwitchWeaponSets(List<EquipmentSettingUseAction> actions, EquipmentSettingUseTarget target) {
		for (EquipmentSettingUseAction action : actions) {
			if (action.action == ACTION_SWITCH_HANDS) {
				return true;
			}
		}
		for (EquipmentSettingUseAction action : actions) {
			if (action.action == ACTION_EQUIP && isWeaponSlot(action.slot) && isOppositeWeaponSet(action.slot, target.getEquippedSlot(action.itemObjectId))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isOppositeWeaponSet(long firstSlot, long secondSlot) {
		return isWeaponSlot(firstSlot) && isWeaponSlot(secondSlot)
				&& (isMainWeaponSlot(firstSlot) && isOffHandWeaponSlot(secondSlot)
				|| isOffHandWeaponSlot(firstSlot) && isMainWeaponSlot(secondSlot));
	}

	private static boolean isEquipSatisfied(long currentSlot, long requestedSlot) {
		return currentSlot != 0 && (currentSlot & requestedSlot) == requestedSlot;
	}

	private static boolean isMainWeaponSlot(long slot) {
		return (slot & ItemSlot.MAIN_OR_SUB.getSlotIdMask()) != 0;
	}

	private static boolean isOffHandWeaponSlot(long slot) {
		return (slot & ItemSlot.MAIN_OFF_OR_SUB_OFF.getSlotIdMask()) != 0;
	}

	private static boolean isWeaponSlot(long slot) {
		return slot != 0 && (slot & WEAPON_SLOTS) != 0 && (slot & ~WEAPON_SLOTS) == 0;
	}

	private static long switchWeaponHands(long slot) {
		long switchedSlot = slot;
		if ((switchedSlot & ItemSlot.RIGHT_HAND.getSlotIdMask()) != 0) {
			switchedSlot ^= ItemSlot.RIGHT_HAND.getSlotIdMask();
		}
		if ((switchedSlot & ItemSlot.LEFT_HAND.getSlotIdMask()) != 0) {
			switchedSlot ^= ItemSlot.LEFT_HAND.getSlotIdMask();
		}
		return switchedSlot;
	}
}
