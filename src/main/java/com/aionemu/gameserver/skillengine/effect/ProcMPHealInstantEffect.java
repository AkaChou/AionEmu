package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 触发即时 MP 治疗：按 MP 治疗类型计算并应用回复。
 * Proc instant MP heal: calculates and applies MP recovery via heal type MP.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProcMPHealInstantEffect")
public class ProcMPHealInstantEffect extends AbstractHealEffect {

	/**
	 * 以 MP 类型计算治疗量。
	 * Calculates heal amount as MP.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, HealType.MP);
	}

	/**
	 * 以 MP 类型应用治疗。
	 * Applies heal as MP.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, HealType.MP);
	}

	@Override
	protected int getCurrentStatValue(Effect effect) {
		return effect.getEffected().getLifeStats().getCurrentMp();
	}

	@Override
	protected int getMaxStatValue(Effect effect) {
		return effect.getEffected().getGameStats().getMaxMp().getCurrent();
	}
}
