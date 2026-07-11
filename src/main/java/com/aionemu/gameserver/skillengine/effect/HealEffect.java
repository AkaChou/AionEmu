package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 生命持续治疗效果：周期恢复目标 HP。
 * HP heal-over-time effect: periodically restores the target's hit points.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HealEffect")
public class HealEffect extends HealOverTimeEffect {

	/**
	 * 计算 HP 持续治疗。
	 * Calculates HP heal-over-time.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, HealType.HP);
	}

	/**
	 * 周期恢复目标生命。
	 * Periodically restores the target's HP.
	 */
	@Override
	public void onPeriodicAction(Effect effect) {
		super.onPeriodicAction(effect, HealType.HP);
	}

	/**
	 * 返回当前生命值。
	 * Returns current HP.
	 */
	@Override
	protected int getCurrentStatValue(Effect effect) {
		return effect.getEffected().getLifeStats().getCurrentHp();
	}

	/**
	 * 返回最大生命值。
	 * Returns maximum HP.
	 */
	@Override
	protected int getMaxStatValue(Effect effect) {
		return effect.getEffected().getGameStats().getMaxHp().getCurrent();
	}
}
