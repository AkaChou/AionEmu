package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.skillengine.model.Skill;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DualWeaponCondition")
public class DualWeaponCondition extends Condition {

	@Override
	public boolean validate(Skill env) {
		if (env.getEffector() instanceof Player player) {
			return player.getEquipment().hasDualWeaponEquipped(ItemSlot.SUB_HAND);
		}
		return false;
	}
}
