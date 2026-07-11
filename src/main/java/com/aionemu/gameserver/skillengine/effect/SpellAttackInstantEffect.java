package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 法术攻击瞬发：立即造成魔法技能伤害。
 * Instant spell attack: deals magical skill damage immediately.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpellAttackInstantEffect")
public class SpellAttackInstantEffect extends DamageEffect {

	/**
	 * 按魔法伤害类型计算。
	 * Calculates as magical damage.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, DamageType.MAGICAL);
	}
}
