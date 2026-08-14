package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.properties.FirstTargetAttribute;
import com.aionemu.gameserver.skillengine.properties.TargetRangeAttribute;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 目标类型条件：校验技能首目标是否符合配置的目标属性（NPC/PC 等）。
 * Target type condition: validates the skill first target matches the configured target attribute (NPC/PC, etc.).
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TargetCondition")
public class TargetCondition extends Condition {

	@XmlAttribute(required = true)
	protected TargetAttribute value;

	/**
	 * 获取配置的目标属性。
	 * Gets the configured target attribute.
	 *
	 * @return 目标属性 / target attribute
	 */
	public TargetAttribute getValue() {
		return value;
	}

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param skill 技能环境 / skill environment
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Skill skill) {
		if ((value == TargetAttribute.NONE) || (value == TargetAttribute.ALL)) {
			return true;
		}
		if (skill.getSkillTemplate().getProperties().getTargetType().equals(TargetRangeAttribute.AREA)) {
			return true;
		}
		if ((skill.getSkillTemplate().getProperties().getFirstTarget() != FirstTargetAttribute.TARGET)
				&& (skill.getSkillTemplate().getProperties().getFirstTarget() != FirstTargetAttribute.TARGETORME)) {
			return true;
		}
		if ((skill.getSkillTemplate().getProperties().getFirstTarget() == FirstTargetAttribute.TARGETORME)
				&& (skill.getEffector() == skill.getFirstTarget())) {
			return true;
		}
		boolean result = false;
		switch (value) {
		case NPC:
			result = skill.getFirstTarget() instanceof Npc;
			break;
		case PC:
			result = skill.getFirstTarget() instanceof Player;
		}

		if ((!result) && ((skill.getEffector() instanceof Player))) {
			PacketSendUtility.sendPacket((Player) skill.getEffector(), SM_SYSTEM_MESSAGE.STR_SKILL_TARGET_IS_NOT_VALID);
		}
		return result;
	}
}
