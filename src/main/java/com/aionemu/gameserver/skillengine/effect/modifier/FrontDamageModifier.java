package com.aionemu.gameserver.skillengine.effect.modifier;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PositionUtil;

/**
 * 正面伤害修正：攻击者位于目标正面时加成伤害。
 * Front damage modifier: bonus damage when the attacker is in front of the target.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FrontDamageModifier")
public class FrontDamageModifier extends ActionModifier {

	@Override
	public int analyze(Effect effect) {
		return value + effect.getSkillLevel() * delta;
	}

	@Override
	public boolean check(Effect effect) {
		return PositionUtil.isInFrontOfTarget(effect.getEffector(), effect.getEffected());
	}
}
