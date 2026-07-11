package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 飞行点持续恢复效果：周期回复目标 FP。
 * FP heal-over-time effect: periodically restores the target's flight points.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FPHealEffect")
public class FPHealEffect extends HealOverTimeEffect {

	/**
	 * 计算 FP 持续恢复。
	 * Calculates FP heal-over-time.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, HealType.FP);
	}

	/**
	 * 周期恢复目标飞行点。
	 * Periodically restores the target's flight points.
	 */
	@Override
	public void onPeriodicAction(Effect effect) {
		super.onPeriodicAction(effect, HealType.FP);
	}

	/**
	 * 返回当前飞行点。
	 * Returns current flight points.
	 */
	@Override
	protected int getCurrentStatValue(Effect effect) {
		return effect.getEffected().getLifeStats().getCurrentFp();
	}

	/**
	 * 返回最大飞行点。
	 * Returns maximum flight points.
	 */
	@Override
	protected int getMaxStatValue(Effect effect) {
		return effect.getEffected().getLifeStats().getMaxFp();
	}
}
