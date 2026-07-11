package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 飞行点瞬时恢复效果：立即回复目标 FP。
 * Instant FP heal effect: immediately restores the target's flight points.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FPHealInstantEffect")
public class FPHealInstantEffect extends AbstractHealEffect {

	/**
	 * 计算瞬时 FP 恢复。
	 * Calculates instant FP heal.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, HealType.FP);
	}

	/**
	 * 立即恢复目标飞行点。
	 * Immediately restores the target's flight points.
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, HealType.FP);
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
