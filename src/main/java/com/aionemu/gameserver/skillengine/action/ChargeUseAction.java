package com.aionemu.gameserver.skillengine.action;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChargeUseAction")
public class ChargeUseAction extends Action {

	@XmlAttribute(required = true)
	protected int weapon;

	@XmlAttribute(required = true)
	protected int armor;

	@Override
	public void act(Skill skill) {
		if (!(skill.getEffector() instanceof Player player)) {
			return;
		}
		for (Item item : player.getEquipment().getEquippedItems()) {
			int cost = item.getItemTemplate().isWeapon() ? weapon : item.getItemTemplate().isArmor() ? armor : 0;
			if (cost > 0 && item.getConditioningInfo() != null) {
				item.getConditioningInfo().burn(cost);
			}
		}
	}

	public int getWeapon() {
		return weapon;
	}

	public int getArmor() {
		return armor;
	}
}
