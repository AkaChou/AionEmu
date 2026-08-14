package com.aionemu.gameserver.model.gameobjects.player.equipmentsetting;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 装备 Setting 列表。
 * Equipment Setting List game object.
 */

public class EquipmentSettingList {

	private final Map<Integer, EquipmentSetting> equipmentSetting = new HashMap<Integer, EquipmentSetting>();
	private Player owner;

	public EquipmentSettingList(Player owner) {
		this.owner = owner;
	}

	/** 添加。 / Add. */
	public EquipmentSetting add(int slot, int display, int mHand, int sHand, int helmet, int torso, int glove,
			int boots, int earringsLeft, int earringsRight, int ringLeft, int ringRight, int necklace, int shoulder,
			int pants, int powershardLeft, int powershardRight, int wings, int waist, int mOffHand, int sOffHand,
			int plume, int bracelet, boolean isNew) {
		return add(slot, EquipmentSetting.defaultName(slot), display, mHand, sHand, helmet, torso, glove, boots,
				earringsLeft, earringsRight, ringLeft, ringRight, necklace, shoulder, pants, powershardLeft,
				powershardRight, wings, waist, mOffHand, sOffHand, plume, bracelet, isNew);
	}

	/** 添加。 / Add. */
	public EquipmentSetting add(int slot, String name, int display, int mHand, int sHand, int helmet, int torso,
			int glove, int boots, int earringsLeft, int earringsRight, int ringLeft, int ringRight, int necklace,
			int shoulder, int pants, int powershardLeft, int powershardRight, int wings, int waist, int mOffHand,
			int sOffHand, int plume, int bracelet, boolean isNew) {
		EquipmentSetting setting = new EquipmentSetting(slot, name, display, mHand, sHand, helmet, torso, glove, boots,
				earringsLeft, earringsRight, ringLeft, ringRight, necklace, shoulder, pants, powershardLeft,
				powershardRight, wings, waist, mOffHand, sOffHand, plume, bracelet);
		setting.setPersistentState(isNew ? PersistentState.UPDATE_REQUIRED : PersistentState.UPDATED);
		equipmentSetting.put(slot, setting);
		return setting;
	}

	/** 返回全部装备方案。 / Returns the equipment settings. */
	public Collection<EquipmentSetting> getEquipmentSetting() {
		return Collections.unmodifiableCollection(equipmentSetting.values());
	}

	/** 返回所有者。 / Returns the owner. */
	public Player getOwner() {
		return owner;
	}

	/** 设置所有者。 / Sets the owner. */
	public void setOwner(Player owner) {
		this.owner = owner;
	}
}
