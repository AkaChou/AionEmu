package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 战斗状态条件：校验玩家是否处于非战斗状态（非玩家恒通过）。
 * Combat check condition: validates the player is not in combat (non-players always pass).
 *
 * @author nrg
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CombatCheckCondition")
public class CombatCheckCondition extends Condition {

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param skill 技能环境 / skill environment
	 * whether valid
	 */
	@Override
	public boolean validate(Skill skill) {
		if (skill.getEffector() instanceof Player) {
			return !((Player) skill.getEffector()).getController().isInCombat();
		}
		return true;
	}
}
