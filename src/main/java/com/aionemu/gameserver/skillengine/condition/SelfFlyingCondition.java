package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.FlyingRestriction;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 自身飞行条件：按限制类型校验施法者处于飞行或地面状态。
 * Self flying condition: validates the effector is flying or grounded per the restriction type.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SelfFlyingCondition")
public class SelfFlyingCondition extends Condition {

	@XmlAttribute(required = true)
	protected FlyingRestriction restriction;

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param env 技能环境 / skill environment
	 * whether valid
	 */
	@Override
	public boolean validate(Skill env) {
		if (env.getEffector() == null) {
			return false;
		}
		switch (restriction) {
		case FLY:
			return env.getEffector().isFlying();
		case GROUND:
			return !env.getEffector().isFlying();
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
		if (effect.getEffector() == null) {
			return false;
		}
		switch (restriction) {
		case FLY:
			return effect.getEffector().isFlying();
		case GROUND:
			return !effect.getEffector().isFlying();
		}
		return true;
	}
}
