package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 伊迪安充能条件：施放时消耗已装备武器上伊迪安石的抛光充能。
 * Idian charge condition: on cast, consumes polish charge from Idian stones on equipped weapons.
 *
 * @author Ranastic
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdianChargeCondition")
public class IdianChargeCondition extends ChargeCondition {

	/**
	 * 校验并消耗已装备武器上伊迪安石的抛光充能。
	 * Validates and consumes polish charge from Idian stones on equipped weapons.
	 *
	 * @param env 技能环境 / skill environment
	 * always true
	 */
	@Override
	public boolean validate(Skill env) {
		if (env.getEffector() instanceof Player) {
			Player effector = (Player) env.getEffector();
			for (Item item : effector.getEquipment().getEquippedItems()) {
				if (item.getItemTemplate().isWeapon() && item.getIdianStone() != null) {
					item.getIdianStone().decreasePolishCharge(effector, 500);
				}
			}
		}
		return true;
	}
}
