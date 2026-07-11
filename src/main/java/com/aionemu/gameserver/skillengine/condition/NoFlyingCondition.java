package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 禁止飞行条件：校验施法者/效果目标当前未处于飞行状态。
 * No-flying condition: validates the effector/effected is currently not flying.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NoFlyingCondition")
public class NoFlyingCondition extends Condition {

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param env 技能环境 / skill environment
	 * whether valid
	 */
	@Override
	public boolean validate(Skill env) {
		return (!env.getEffector().isFlying());
	}

	/**
	 * 校验效果环境是否满足本条件。
	 * Validates whether the effect environment satisfies this condition.
	 *
	 * effect environment
	 * whether valid
	 */
	@Override
	public boolean validate(Effect effect) {
		return (!effect.getEffected().isFlying());
	}
}
