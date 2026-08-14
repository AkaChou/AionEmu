package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.PositionUtil;

/**
 * 背刺方位条件：校验施法者是否位于目标背后。
 * Back position condition: validates the effector is behind the target.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BackCondition")
public class BackCondition extends Condition {

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param env 技能环境 / skill environment
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Skill env) {
		if (env.getFirstTarget() == null || env.getEffector() == null) {
			return false;
		}
		return PositionUtil.isBehindTarget(env.getEffector(), env.getFirstTarget());
	}

	/**
	 * 校验效果环境是否满足本条件。
	 * Validates whether the effect environment satisfies this condition.
	 *
	 * @param effect 效果环境 / effect environment
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Effect effect) {
		if (effect.getEffected() == null || effect.getEffector() == null) {
			return false;
		}
		return PositionUtil.isBehindTarget(effect.getEffector(), effect.getEffected());
	}
}
