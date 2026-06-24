package com.aionemu.gameserver.model.gameobjects.player.equipmentsetting;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

public class EquipmentSettingList {

	private final Map<Integer, EquipmentSetting> equipmentSetting = new HashMap<Integer, EquipmentSetting>();
	private Player owner;

	public EquipmentSettingList(Player owner) {
		this.owner = owner;
	}

	public EquipmentSetting add(int slot, int display, int mHand, int sHand, int helmet, int torso, int glove,
			int boots, int earringsLeft, int earringsRight, int ringLeft, int ringRight, int necklace, int shoulder,
			int pants, int powershardLeft, int powershardRight, int wings, int waist, int mOffHand, int sOffHand,
			int plume, int bracelet, boolean isNew) {
		EquipmentSetting setting = new EquipmentSetting(slot, display, mHand, sHand, helmet, torso, glove, boots,
				earringsLeft, earringsRight, ringLeft, ringRight, necklace, shoulder, pants, powershardLeft,
				powershardRight, wings, waist, mOffHand, sOffHand, plume, bracelet);
		setting.setPersistentState(isNew ? PersistentState.UPDATE_REQUIRED : PersistentState.UPDATED);
		equipmentSetting.put(slot, setting);
		return setting;
	}

	public Collection<EquipmentSetting> getEquipmentSetting() {
		return Collections.unmodifiableCollection(equipmentSetting.values());
	}

	public Player getOwner() {
		return owner;
	}

	public void setOwner(Player owner) {
		this.owner = owner;
	}
}
