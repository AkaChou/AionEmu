package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 触发即时 HP 治疗：按 HP 治疗类型计算并应用回复。
 * Proc instant HP heal: calculates and applies HP recovery via heal type HP.
 *
 * @author ATracer modified by Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProcHealInstantEffect")
public class ProcHealInstantEffect extends AbstractHealEffect {

	/**
	 * 以 HP 类型计算治疗量。
	 * Calculates heal amount as HP.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, HealType.HP);
	}

	/**
	 * 以 HP 类型应用治疗。
	 * Applies heal as HP.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, HealType.HP);
	}

	@Override
	protected int getCurrentStatValue(Effect effect) {
		return effect.getEffected().getLifeStats().getCurrentHp();
	}

	@Override
	protected int getMaxStatValue(Effect effect) {
		return effect.getEffected().getGameStats().getMaxHp().getCurrent();
	}
}
