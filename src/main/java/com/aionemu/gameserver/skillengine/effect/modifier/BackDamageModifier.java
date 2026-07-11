package com.aionemu.gameserver.skillengine.effect.modifier;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PositionUtil;

/**
 * 背后伤害修正：攻击者位于目标身后时加成伤害。
 * Back damage modifier: bonus damage when the attacker is behind the target.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BackDamageModifier")
public class BackDamageModifier extends ActionModifier {

	@Override
	public int analyze(Effect effect) {
		return value + effect.getSkillLevel() * delta;
	}

	@Override
	public boolean check(Effect effect) {
		return PositionUtil.isBehindTarget(effect.getEffector(), effect.getEffected());
	}
}
