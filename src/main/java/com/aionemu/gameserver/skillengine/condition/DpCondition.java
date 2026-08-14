package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * DP 条件：校验玩家当前斗志是否达到施放要求。
 * DP condition: validates the player's current DP meets cast requirements.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DpCondition")
public class DpCondition extends Condition {

	@XmlAttribute(required = true)
	protected int value;

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param skill 技能环境 / skill environment
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Skill skill) {
		return ((Player) skill.getEffector()).getCommonData().getDp() >= value;
	}
}
