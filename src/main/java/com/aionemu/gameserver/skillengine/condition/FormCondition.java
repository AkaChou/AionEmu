package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.TransformType;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 变身形态条件：校验玩家当前变身类型是否与要求一致。
 * Form condition: validates the player's current transform type matches the requirement.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FormCondition")
public class FormCondition extends Condition {

	@XmlAttribute(required = true)
	protected TransformType value;

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param env 技能环境 / skill environment
	 * whether valid
	 */
	public boolean validate(Skill env) {
		if ((env.getEffector() instanceof Player)) {
			if ((env.getEffector().getTransformModel().isActive())
					&& (env.getEffector().getTransformModel().getType() == value)) {
				return true;
			}
			PacketSendUtility.sendPacket((Player) env.getEffector(),
					SM_SYSTEM_MESSAGE.STR_SKILL_CAN_NOT_CAST_IN_THIS_FORM);
			return false;
		}
		return true;
	}
}
