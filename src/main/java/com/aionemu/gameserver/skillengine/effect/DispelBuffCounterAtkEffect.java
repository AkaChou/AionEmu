package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.change.Func;
import com.aionemu.gameserver.skillengine.effect.modifier.ActionModifier;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 驱散 Buff 并反击效果：按可驱散 Buff 数量结算伤害并驱散。
 * Dispel-buff counter-attack: damage scales with removable buff count, then dispels them.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DispelBuffCounterAtkEffect")
public class DispelBuffCounterAtkEffect extends DamageEffect {
	@XmlAttribute
	protected int dpower;
	@XmlAttribute
	protected int power;
	@XmlAttribute
	protected int hitvalue;
	@XmlAttribute
	protected int hitdelta;
	@XmlAttribute(name = "dispel_level")
	protected int dispelLevel;
	private int i;
	private int finalPower;

	/**
	 * 应用伤害并按层数驱散 Buff。
	 * Applies damage and dispels buffs by computed count.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect);
		effect.getEffected().getEffectController().dispelBuffCounterAtkEffect(i, dispelLevel, finalPower);
	}

	/**
	 * 按可驱散 Buff 数计算伤害与驱散参数。
	 * Calculates damage and dispel params from removable buff count.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		if (!super.calculate(effect, null, null)) {
			return;
		}
		Creature effected = effect.getEffected();
		int count = value + delta * effect.getSkillLevel();
		finalPower = power + dpower * effect.getSkillLevel();
		i = effected.getEffectController().calculateNumberOfEffects(dispelLevel);
		i = (i < count ? i : count);
		int newValue = 0;
		if (i == 1) {
			newValue = hitvalue;
		} else if (i > 1) {
			newValue = hitvalue + ((hitvalue / 2) * (i - 1));
		}
		int valueWithDelta = newValue + hitdelta * effect.getSkillLevel();
		ActionModifier modifier = getActionModifiers(effect);
		AttackUtil.calculateMagicalSkillResult(effect, valueWithDelta, modifier, getElement(), true, true, false,
				Func.ADD, 0, 0, shared, true);
	}
}
