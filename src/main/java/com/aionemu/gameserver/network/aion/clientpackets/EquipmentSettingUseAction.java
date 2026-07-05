package com.aionemu.gameserver.network.aion.clientpackets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.items.ItemSlot;

class EquipmentSettingUseAction {

	private static final int ACTION_EQUIP = 0;
	private static final int ACTION_UNEQUIP = 1;
	private static final int ACTION_SWITCH_HANDS = 2;
	private static final long WEAPON_SLOTS = ItemSlot.MAIN_OR_SUB.getSlotIdMask() | ItemSlot.MAIN_OFF_OR_SUB_OFF.getSlotIdMask();

	private final int action;
	private final long slot;
	private final int itemObjectId;

	EquipmentSettingUseAction(int action, long slot, int itemObjectId) {
		this.action = action;
		this.slot = slot;
		this.itemObjectId = itemObjectId;
	}

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
		Map<Integer, Long> requestedWeaponSlots = getRequestedWeaponSlots(actions);
		boolean switchedHands = shouldSwitchHands(actions, target);
		boolean changed = false;
		List<EquipmentSettingUseAction> appliedActions = new ArrayList<EquipmentSettingUseAction>();
		if (switchedHands) {
			changed = target.switchHands();
			if (!changed) {
				return false;
			}
		} else {
			changed |= applyInventoryWeaponEquips(actions, target, appliedActions);
			changed |= applyRequestedWeaponUnequips(actions, target, requestedWeaponSlots, appliedActions);
		}
		for (EquipmentSettingUseAction action : actions) {
			if (action.action == ACTION_SWITCH_HANDS || appliedActions.contains(action)
					|| switchedHands && action.isHandledByHandSwitch(target, requestedWeaponSlots)) {
				continue;
			}
			changed |= action.apply(target);
		}
		return changed;
	}

	private static Map<Integer, Long> getRequestedWeaponSlots(List<EquipmentSettingUseAction> actions) {
		Map<Integer, Long> requestedWeaponSlots = new HashMap<Integer, Long>();
		for (EquipmentSettingUseAction action : actions) {
			if (action.action == ACTION_EQUIP && isWeaponSlot(action.slot)) {
				Long requestedSlot = requestedWeaponSlots.get(action.itemObjectId);
				requestedWeaponSlots.put(action.itemObjectId, requestedSlot == null ? action.slot : requestedSlot | action.slot);
			}
		}
		return requestedWeaponSlots;
	}

	private static boolean shouldSwitchHands(List<EquipmentSettingUseAction> actions, EquipmentSettingUseTarget target) {
		boolean requestedSwitch = false;
		boolean hasSwitchableWeaponEquip = false;
		for (EquipmentSettingUseAction action : actions) {
			if (action.action == ACTION_SWITCH_HANDS) {
				requestedSwitch = true;
			}
			if (action.isEquipFromOppositeWeaponHand(target)) {
				hasSwitchableWeaponEquip = true;
			}
		}
		return (requestedSwitch || hasSwitchableWeaponEquip) && canSwitchHandsForWeaponActions(actions, target);
	}

	private static boolean canSwitchHandsForWeaponActions(List<EquipmentSettingUseAction> actions, EquipmentSettingUseTarget target) {
		for (EquipmentSettingUseAction action : actions) {
			if (action.action != ACTION_EQUIP || !isWeaponSlot(action.slot)) {
				continue;
			}
			long currentSlot = target.getEquippedSlot(action.itemObjectId);
			if (currentSlot == 0 || (switchWeaponHands(currentSlot) & action.slot) != action.slot) {
				return false;
			}
		}
		return true;
	}

	private static boolean applyInventoryWeaponEquips(List<EquipmentSettingUseAction> actions, EquipmentSettingUseTarget target,
			List<EquipmentSettingUseAction> appliedActions) {
		boolean changed = false;
		for (EquipmentSettingUseAction action : actions) {
			if (action.action == ACTION_EQUIP && isWeaponSlot(action.slot) && target.getEquippedSlot(action.itemObjectId) == 0) {
				changed |= action.apply(target);
				appliedActions.add(action);
			}
		}
		return changed;
	}

	private static boolean applyRequestedWeaponUnequips(List<EquipmentSettingUseAction> actions, EquipmentSettingUseTarget target,
			Map<Integer, Long> requestedWeaponSlots, List<EquipmentSettingUseAction> appliedActions) {
		boolean changed = false;
		for (EquipmentSettingUseAction action : actions) {
			if (action.action != ACTION_UNEQUIP || !isWeaponSlot(action.slot) || !requestedWeaponSlots.containsKey(action.itemObjectId)
					|| appliedActions.contains(action)) {
				continue;
			}
			long currentSlot = target.getEquippedSlot(action.itemObjectId);
			long requestedSlot = requestedWeaponSlots.get(action.itemObjectId);
			if (currentSlot != 0 && (currentSlot & requestedSlot) != requestedSlot) {
				changed |= action.apply(target);
				appliedActions.add(action);
			}
		}
		return changed;
	}

	private boolean isEquipFromOppositeWeaponHand(EquipmentSettingUseTarget target) {
		if (action != ACTION_EQUIP) {
			return false;
		}
		long currentSlot = target.getEquippedSlot(itemObjectId);
		return areOppositeWeaponHands(slot, currentSlot);
	}

	private boolean isHandledByHandSwitch(EquipmentSettingUseTarget target, Map<Integer, Long> requestedWeaponSlots) {
		long currentSlot = target.getEquippedSlot(itemObjectId);
		if (action == ACTION_EQUIP) {
			return isWeaponSlot(slot) && isWeaponSlot(currentSlot) && (currentSlot & slot) == slot;
		}
		if (action == ACTION_UNEQUIP) {
			Long requestedSlot = requestedWeaponSlots.get(itemObjectId);
			return areOppositeWeaponHands(slot, currentSlot) || requestedSlot != null && isWeaponSlot(currentSlot)
					&& (currentSlot & requestedSlot) == requestedSlot;
		}
		return false;
	}

	private static boolean areOppositeWeaponHands(long firstSlot, long secondSlot) {
		return isWeaponSlot(firstSlot) && isWeaponSlot(secondSlot)
				&& ((switchWeaponHands(firstSlot) & secondSlot) != 0 || (switchWeaponHands(secondSlot) & firstSlot) != 0);
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
