package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.FlyingRestriction;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 目标飞行条件：按限制类型校验首目标/效果目标处于飞行或地面状态。
 * Target flying condition: validates the first target/effected is flying or grounded per the restriction type.
 *
 * @author Sippolo
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TargetFlyingCondition")
public class TargetFlyingCondition extends Condition {

	@XmlAttribute(required = true)
	protected FlyingRestriction restriction = FlyingRestriction.FLY;

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param env 技能环境 / skill environment
	 * whether valid
	 */
	@Override
	public boolean validate(Skill env) {
		if (env.getFirstTarget() == null) {
			return false;
		}
		switch (restriction) {
		case FLY:
			return env.getFirstTarget().isFlying();
		case GROUND:
			return !env.getFirstTarget().isFlying();
		}
		return true;
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
		if (effect.getEffected() == null) {
			return false;
		}
		switch (restriction) {
		case FLY:
			return effect.getEffected().isFlying();
		case GROUND:
			return !effect.getEffected().isFlying();
		}
		return true;
	}
}
